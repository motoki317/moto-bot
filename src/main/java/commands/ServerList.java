package commands;

import app.Bot;
import commands.base.GenericCommand;
import db.model.timezone.CustomTimeZone;
import db.model.world.World;
import db.repository.base.DateFormatRepository;
import db.repository.base.TimeZoneRepository;
import db.repository.base.WorldRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import update.multipage.MultipageHandler;
import update.reaction.ReactionManager;
import utils.FormatUtils;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ServerList extends GenericCommand {
    private static final int WORLDS_PER_PAGE_DEFAULT = 20;
    private static final int WORLDS_PER_PAGE_MAX = 50;

    private final WorldRepository repo;
    private final ReactionManager reactionManager;

    private final TimeZoneRepository timeZoneRepository;
    private final DateFormatRepository dateFormatRepository;

    public ServerList(Bot bot) {
        this.repo = bot.getDatabase().getWorldRepository();
        this.reactionManager = bot.getReactionManager();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
    }

    @NotNull
    @Override
    public String[][] names() {
        return new String[][]{{"serverlist", "servers", "up"}};
    }

    @NotNull
    @Override
    public String syntax() {
        return "serverlist [all] [number]";
    }

    @NotNull
    @Override
    public String shortHelp() {
        return "Returns a list of Wynncraft servers and their respective uptime.";
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

    /**
     * Retrieves world list.
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
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        boolean getAll = false;
        int worldsPerPage = WORLDS_PER_PAGE_DEFAULT;
        if (args.length > 1) {
            for (String arg : args) {
                if ("getAll".equals(arg.toLowerCase())) {
                    getAll = true;
                } else if (arg.matches("\\d+")) {
                    worldsPerPage = Integer.parseInt(arg);
                }
            }
        }

        if (worldsPerPage < 1 || WORLDS_PER_PAGE_MAX < worldsPerPage) {
            respond(event, "Invalid number specified for worlds per page argument.\n" +
                    "Please specify a number between 1 and " + WORLDS_PER_PAGE_MAX + ".");
            return;
        }

        List<World> worlds = getWorlds(getAll);
        if (worlds == null) {
            respondError(event, "Something went wrong while retrieving data...");
            return;
        }
        if (worlds.isEmpty()) {
            respond(event, "There doesn't seem to be any " + (getAll ? "" : "main ") + "worlds online...");
            return;
        }

        sendMessage(event, worldsPerPage, worlds);
    }

    // Send actual message
    private void sendMessage(@NotNull MessageReceivedEvent event, int worldsPerPage, List<World> worlds) {
        CustomTimeZone timeZone = this.timeZoneRepository.getTimeZone(event);
        DateFormat dateFormat = this.dateFormatRepository.getDateFormat(event).getDateFormat().getMinuteFormat();
        dateFormat.setTimeZone(timeZone.getTimeZoneInstance());

        Function<Integer, Message> pages = page ->
                new MessageBuilder(format(worlds, page, worldsPerPage, timeZone, dateFormat)).build();
        int maxPage = maxPage(worlds.size(), worldsPerPage);

        if (maxPage == 0) {
            respond(event, pages.apply(0));
            return;
        }

        respond(event, pages.apply(0), message -> {
            MultipageHandler handler = new MultipageHandler(message, event.getAuthor().getIdLong(), pages, () -> maxPage);
            this.reactionManager.addEventListener(handler);
        });
    }

    private static int maxPage(int worldsSize, int worldsPerPage) {
        return (worldsSize - 1) / worldsPerPage;
    }

    private static class WorldDisplay {
        private String num;
        private String name;
        private String players;
        private String uptime;

        private WorldDisplay(int num, String name, int players, String uptime) {
            this.num = String.valueOf(num);
            this.name = name;
            this.players = String.valueOf(players);
            this.uptime = uptime;
        }
    }

    @NotNull
    private static String format(@NotNull List<World> worlds, int page, int worldsPerPage, CustomTimeZone timeZone, DateFormat dateFormat) {
        worlds.sort((w1, w2) -> (int) (w2.getCreatedAt().getTime() - w1.getCreatedAt().getTime()));

        long currentTimeMillis = System.currentTimeMillis();

        int min = page * worldsPerPage;
        int max = Math.min((page + 1) * worldsPerPage, worlds.size());
        List<WorldDisplay> displays = new ArrayList<>();
        for (int i = min; i < max; i++) {
            World w = worlds.get(i);
            int num = i + 1;
            long uptimeSeconds = (currentTimeMillis - w.getCreatedAt().getTime()) / 1000L;
            String uptime = FormatUtils.formatReadableTime(uptimeSeconds, true, "m");

            displays.add(new WorldDisplay(num, w.getName(), w.getPlayers(), uptime));
        }

        int maxPage = maxPage(worlds.size(), worldsPerPage);
        String pageView = String.format("< page %s / %s >", page + 1, maxPage + 1);

        return formatDisplays(
                displays,
                worlds.stream().mapToLong(d -> d.getUpdatedAt().getTime()).max().orElse(System.currentTimeMillis()),
                dateFormat,
                timeZone,
                pageView
        );
    }

    private static String formatDisplays(List<WorldDisplay> displays,
                                         long lastUpdate, DateFormat dateFormat, CustomTimeZone customTimeZone,
                                         String pageView) {
        int numJustify = displays.stream().mapToInt(d -> d.num.length()).max().orElse(1);
        int serverNameJustify = IntStream.concat(
                IntStream.of("Server".length()),
                displays.stream().mapToInt(d -> d.name.length())
        ).max().orElse("Server".length());
        int playersJustify = IntStream.concat(
                IntStream.of("Players".length()),
                displays.stream().mapToInt(d -> d.players.length())
        ).max().orElse("Players".length());
        int uptimeJustify = IntStream.concat(
                IntStream.of("Uptime".length()),
                displays.stream().mapToInt(d -> d.uptime.length())
        ).max().orElse("Uptime".length());

        List<String> ret = new ArrayList<>();
        ret.add("```ml");
        ret.add("---- Server List ----");
        ret.add("");

        ret.add(nSpaces(numJustify + 2) + "Server" + nSpaces(serverNameJustify - 6) +
                " | " + "Players" + nSpaces(playersJustify - 7) +
                " | " + "Uptime");
        ret.add(nHyphens(numJustify + 2 + serverNameJustify) +
                "-+-" + nHyphens(playersJustify) +
                "-+-" + nHyphens(uptimeJustify + 1));

        for (WorldDisplay d : displays) {
            ret.add(String.format("%s.%s %s%s | %s%s | %s%s",
                    d.num, nSpaces(numJustify - d.num.length()),
                    d.name, nSpaces(serverNameJustify - d.name.length()),
                    d.players, nSpaces(playersJustify - d.players.length()),
                    nSpaces(uptimeJustify - d.uptime.length()), d.uptime
            ));
        }

        ret.add("");

        ret.add(pageView);

        ret.add("");

        ret.add(String.format("last update: %s (%s)",
                dateFormat.format(new Date(lastUpdate)), customTimeZone.getFormattedTime()));

        ret.add("```");
        return String.join("\n", ret);
    }

    private static String nSpaces(int n) {
        return String.join("", Collections.nCopies(n, " "));
    }

    private static String nHyphens(int n) {
        return String.join("", Collections.nCopies(n, "-"));
    }
}
