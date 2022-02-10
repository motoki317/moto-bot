package commands.guild;

import app.Bot;
import commands.base.GenericCommand;
import commands.event.CommandEvent;
import db.model.dateFormat.CustomDateFormat;
import db.model.timezone.CustomTimeZone;
import db.model.warLog.WarLog;
import db.model.world.World;
import db.repository.base.DateFormatRepository;
import db.repository.base.TimeZoneRepository;
import db.repository.base.WarLogRepository;
import db.repository.base.WorldRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import utils.FormatUtils;
import utils.TableFormatter;

import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static utils.TableFormatter.Justify.Left;
import static utils.TableFormatter.Justify.Right;

public class CurrentWars extends GenericCommand {
    private final WorldRepository worldRepository;
    private final WarLogRepository warLogRepository;

    private final TimeZoneRepository timeZoneRepository;
    private final DateFormatRepository dateFormatRepository;

    public CurrentWars(Bot bot) {
        this.worldRepository = bot.getDatabase().getWorldRepository();
        this.warLogRepository = bot.getDatabase().getWarLogRepository();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"g", "guild"}, {"war", "wars"}};
    }

    @Override
    public @NotNull String[] slashName() {
        return new String[]{"g", "war"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{};
    }

    @Override
    public @NotNull String syntax() {
        return "g war";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Shows ongoing guild wars.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                        .setAuthor("Guild War Command Help")
                        .setDescription(this.shortHelp())
        ).build();
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        List<WarLog> wars = this.warLogRepository.findAllNotEnded();
        List<World> notYetStartedWars = this.worldRepository.findAllWarWorlds();

        if (wars == null || notYetStartedWars == null) {
            event.replyError("Something went wrong while retrieving data...");
            return;
        }

        Set<String> startedWars = wars.stream().map(WarLog::getServerName).collect(Collectors.toSet());
        notYetStartedWars = notYetStartedWars.stream().filter(w -> !startedWars.contains(w.getName())).collect(Collectors.toList());

        if (wars.isEmpty() && notYetStartedWars.isEmpty()) {
            event.reply("There doesn't seem to be any guild wars going on.");
            return;
        }

        wars.sort(Comparator.comparingLong(w -> w.getCreatedAt().getTime()));

        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);
        CustomDateFormat customDateFormat = this.dateFormatRepository.getDateFormat(event);
        Date lastUpdate = wars.isEmpty()
                ? notYetStartedWars.get(0).getUpdatedAt()
                : wars.get(0).getLastUp();

        List<Display> displays = formatDisplays(wars, notYetStartedWars);
        int pages = (displays.size() - 1) / WARS_PER_PAGE;
        if (pages == 0) {
            event.reply(formatMessage(0, wars.size(), displays, lastUpdate, customTimeZone, customDateFormat));
            return;
        }

        Function<Integer, Message> pageSupplier = page -> new MessageBuilder(formatMessage(page, wars.size(), displays, lastUpdate, customTimeZone, customDateFormat)).build();
        event.replyMultiPage(pageSupplier.apply(0), pageSupplier, () -> pages);
    }

    private static final int WARS_PER_PAGE = 20;

    private record Display(String server, String guild, String players,
                           String elapsed) {
    }

    @NotNull
    private static List<Display> formatDisplays(@NotNull List<WarLog> wars,
                                                @NotNull List<World> notYetStartedWars) {
        Function<WarLog, Display> createDisplay = warLog -> {
            int remainingPlayers = (int) warLog.getPlayers().stream().filter(p -> !p.hasExited()).count();
            int totalPlayers = warLog.getPlayers().size();
            long elapsedSeconds = (warLog.getLastUp().getTime() - warLog.getCreatedAt().getTime()) / 1000L;
            return new Display(
                    warLog.getServerName(),
                    warLog.getGuildName() != null ? warLog.getGuildName() : "(Unknown Guild)",
                    String.format("%s / %s", remainingPlayers, totalPlayers),
                    FormatUtils.formatReadableTime(elapsedSeconds, true, "s")
            );
        };
        Function<World, Display> createDisplayFromWorld = world -> new Display(
                world.getName(),
                "(Not Yet Started)",
                String.format("%s / %s", world.getPlayers(), world.getPlayers()),
                FormatUtils.formatReadableTime(0L, true, "s")
        );

        List<Display> displays = wars.stream().map(createDisplay).collect(Collectors.toList());
        displays.addAll(notYetStartedWars.stream().map(createDisplayFromWorld).toList());
        return displays;
    }

    @NotNull
    private static String formatMessage(int page, int startedWars,
                                        @NotNull List<Display> displays,
                                        @NotNull Date lastUpdate,
                                        @NotNull CustomTimeZone customTimeZone,
                                        @NotNull CustomDateFormat customDateFormat) {
        List<String> ret = new ArrayList<>();
        ret.add("```ml");
        ret.add("---- Ongoing Wars ----");
        ret.add(String.format(
                "%s / %s servers in fight",
                startedWars, displays.size()
        ));
        ret.add("");

        TableFormatter tf = new TableFormatter(false);
        tf.addColumn("Server", Left, Left)
                .addColumn("Guild", Left, Left)
                .addColumn("Players", Left, Right)
                .addColumn("Elapsed", Left, Right);

        int from = page * WARS_PER_PAGE;
        int to = Math.min(displays.size(), (page + 1) * WARS_PER_PAGE);
        for (int i = from; i < to; i++) {
            Display d = displays.get(i);
            tf.addRow(d.server, d.guild, d.players, d.elapsed);
        }

        ret.add(tf.toString());

        // show current page if max page >= 1
        int maxPage = (displays.size() - 1) / WARS_PER_PAGE;
        if (maxPage >= 1) {
            ret.add(String.format("< page %d / %d >", page + 1, maxPage + 1));
            ret.add("");
        }

        DateFormat dateFormat = customDateFormat.getDateFormat().getSecondFormat();
        dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());

        long secondsFromLastUpdate = (new Date().getTime() - lastUpdate.getTime()) / 1000L;
        ret.add(String.format(
                "last update: %s (%s), %s ago",
                dateFormat.format(lastUpdate),
                customTimeZone.getFormattedTime(),
                FormatUtils.formatReadableTime(secondsFromLastUpdate, false, "s")
        ));

        ret.add("```");

        return String.join("\n", ret);
    }
}
