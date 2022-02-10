package commands;

import app.Bot;
import commands.base.GenericCommand;
import commands.event.CommandEvent;
import db.model.timezone.CustomTimeZone;
import db.model.world.World;
import db.repository.base.DateFormatRepository;
import db.repository.base.TimeZoneRepository;
import db.repository.base.WorldRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.FormatUtils;
import utils.TableFormatter;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static utils.TableFormatter.Justify.Left;
import static utils.TableFormatter.Justify.Right;

public class ServerList extends GenericCommand {
    private static final int WORLDS_PER_PAGE_DEFAULT = 20;
    private static final int WORLDS_PER_PAGE_MAX = 50;

    private final WorldRepository repo;

    private final TimeZoneRepository timeZoneRepository;
    private final DateFormatRepository dateFormatRepository;

    public ServerList(Bot bot) {
        this.repo = bot.getDatabase().getWorldRepository();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
    }

    @NotNull
    @Override
    public String[][] names() {
        return new String[][]{{"serverlist", "servers", "up"}};
    }

    @Override
    public @NotNull String[] slashName() {
        return new String[]{"up"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{
                new OptionData(OptionType.INTEGER, "per-page", "Worlds per page"),
                new OptionData(OptionType.STRING, "all", "Show all worlds not just WC")
                        .addChoice("all", "all")
        };
    }

    @NotNull
    @Override
    public String syntax() {
        return "serverlist [all] [number]";
    }

    @NotNull
    @Override
    public String shortHelp() {
        return "Shows Wynncraft servers and their respective uptime.";
    }

    @NotNull
    @Override
    public Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                        .setAuthor("Server List Command Help")
                        .setDescription("This command returns a list of Wynncraft servers and their respective uptime.")
                        .addField("Syntax",
                                String.join("\n",
                                        "`" + this.syntax() + "`",
                                        "`[all]` optional argument will, when specified, show all worlds excluding WAR worlds: " +
                                                "not only main WC/EU servers but also lobby, GM and other servers.",
                                        "`[num]` optional argument will, when specified, set the max number of worlds to show per page." +
                                                " The default is " + WORLDS_PER_PAGE_DEFAULT + " worlds per page."),
                                false)
                        .build()
        ).build();
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    /**
     * Retrieves world list.
     *
     * @param getAll If {@code true}, retrieves all worlds including lobby etc., but excluding WAR worlds.
     * @return World list.
     */
    @Nullable
    private List<World> getWorlds(boolean getAll) {
        if (getAll) {
            List<World> allWorlds = repo.findAll();
            if (allWorlds == null) {
                return null;
            }
            return allWorlds.stream().filter(w -> !w.getName().startsWith("WAR")).collect(Collectors.toList());
        }
        return repo.findAllMainWorlds();
    }

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        boolean getAll = false;
        int worldsPerPage = WORLDS_PER_PAGE_DEFAULT;
        if (args.length > 1) {
            for (String arg : args) {
                if ("all".equalsIgnoreCase(arg)) {
                    getAll = true;
                } else if (arg.matches("\\d+")) {
                    worldsPerPage = Integer.parseInt(arg);
                }
            }
        }

        if (worldsPerPage < 1 || WORLDS_PER_PAGE_MAX < worldsPerPage) {
            event.reply("Invalid number specified for worlds per page argument.\n" +
                    "Please specify a number between 1 and " + WORLDS_PER_PAGE_MAX + ".");
            return;
        }

        List<World> worlds = getWorlds(getAll);
        if (worlds == null) {
            event.replyError("Something went wrong while retrieving data...");
            return;
        }
        if (worlds.isEmpty()) {
            event.reply("There doesn't seem to be any " + (getAll ? "" : "main ") + "worlds online...");
            return;
        }

        worlds.sort((w1, w2) -> w2.getCreatedAt().compareTo(w1.getCreatedAt()));
        sendMessage(event, worldsPerPage, worlds);
    }

    // Send actual message
    private void sendMessage(@NotNull CommandEvent event, int worldsPerPage, List<World> worlds) {
        CustomTimeZone timeZone = this.timeZoneRepository.getTimeZone(event);
        DateFormat dateFormat = this.dateFormatRepository.getDateFormat(event).getDateFormat().getMinuteFormat();
        dateFormat.setTimeZone(timeZone.getTimeZoneInstance());

        Function<Integer, Message> pages = page ->
                new MessageBuilder(format(worlds, page, worldsPerPage, timeZone, dateFormat)).build();
        int maxPage = maxPage(worlds.size(), worldsPerPage);

        if (maxPage == 0) {
            event.reply(pages.apply(0));
            return;
        }

        event.replyMultiPage(pages.apply(0), pages, () -> maxPage);
    }

    private static int maxPage(int worldsSize, int worldsPerPage) {
        return (worldsSize - 1) / worldsPerPage;
    }

    @NotNull
    private static String format(@NotNull List<World> worlds, int page, int worldsPerPage, CustomTimeZone timeZone, DateFormat dateFormat) {
        long currentTimeMillis = System.currentTimeMillis();

        int maxPage = maxPage(worlds.size(), worldsPerPage);
        String pageView = String.format("< page %s / %s >", page + 1, maxPage + 1);

        List<String> ret = new ArrayList<>();
        ret.add("```ml");
        ret.add("---- Server List ----");
        ret.add("");

        TableFormatter tf = new TableFormatter(false);
        tf.addColumn("", Left, Left)
                .addColumn("Server", Left, Left)
                .addColumn("Players", Left, Right)
                .addColumn("Uptime", Left, Right);

        int min = page * worldsPerPage;
        int max = Math.min((page + 1) * worldsPerPage, worlds.size());
        for (int i = min; i < max; i++) {
            World world = worlds.get(i);
            long uptimeSeconds = (currentTimeMillis - world.getCreatedAt().getTime()) / 1000L;
            String uptime = FormatUtils.formatReadableTime(uptimeSeconds, true, "m");
            tf.addRow("" + (i + 1) + ".", world.getName(), "" + world.getPlayers(), uptime);
        }
        ret.add(tf.toString());

        ret.add(pageView);
        ret.add("");

        long lastUpdate = worlds.stream()
                .mapToLong(d -> d.getUpdatedAt().getTime())
                .max()
                .orElse(System.currentTimeMillis());
        ret.add(String.format("last update: %s (%s)",
                dateFormat.format(new Date(lastUpdate)), timeZone.getFormattedTime()));
        ret.add("```");
        return String.join("\n", ret);
    }
}
