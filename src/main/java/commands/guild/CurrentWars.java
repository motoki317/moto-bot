package commands.guild;

import app.Bot;
import commands.base.GenericCommand;
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
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import utils.FormatUtils;

import java.text.DateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        List<WarLog> wars = this.warLogRepository.findAllNotEnded();
        List<World> notYetStartedWars = this.worldRepository.findAllWarWorlds();

        if (wars == null || notYetStartedWars == null) {
            respondError(event, "Something went wrong while retrieving data...");
            return;
        }

        Set<String> startedWars = wars.stream().map(WarLog::getServerName).collect(Collectors.toSet());
        notYetStartedWars = notYetStartedWars.stream().filter(w -> !startedWars.contains(w.getName())).collect(Collectors.toList());

        if (wars.isEmpty() && notYetStartedWars.isEmpty()) {
            respond(event, "There doesn't seem to be any guild wars going on.");
            return;
        }

        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);
        CustomDateFormat customDateFormat = this.dateFormatRepository.getDateFormat(event);
        respond(event, formatMessage(wars, notYetStartedWars, customTimeZone, customDateFormat));
    }

    @NotNull
    private String formatMessage(@NotNull List<WarLog> wars,
                                 @NotNull List<World> notYetStartedWars,
                                 @NotNull CustomTimeZone customTimeZone,
                                 @NotNull CustomDateFormat customDateFormat) {
        wars.sort(Comparator.comparingLong(w -> w.getCreatedAt().getTime()));

        class Display {
            private String server;
            private String guild;
            private String players;
            private String elapsed;

            private Display(String server, String guild, String players, String elapsed) {
                this.server = server;
                this.guild = guild;
                this.players = players;
                this.elapsed = elapsed;
            }
        }

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
        displays.addAll(notYetStartedWars.stream().map(createDisplayFromWorld).collect(Collectors.toList()));

        int serverNameJustify = IntStream.concat(
                displays.stream().mapToInt(d -> d.server.length()),
                IntStream.of("Server".length())
        ).max().orElse("Server".length());
        int guildNameJustify = IntStream.concat(
                displays.stream().mapToInt(d -> d.guild.length()),
                IntStream.of("Guild".length())
        ).max().orElse("Guild".length());
        int playersJustify = IntStream.concat(
                displays.stream().mapToInt(d -> d.players.length()),
                IntStream.of("Players".length())
        ).max().orElse("Players".length());
        int elapsedFieldJustify = IntStream.concat(
                displays.stream().mapToInt(d -> d.elapsed.length()),
                IntStream.of("Elapsed".length())
        ).max().orElse("Elapsed".length());

        List<String> ret = new ArrayList<>();
        ret.add("```ml");
        ret.add("---- Ongoing Wars ----");
        ret.add(String.format(
                "%s / %s servers in fight",
                wars.size(), displays.size()
        ));
        ret.add("");

        ret.add(String.format(
                "Server%s | Guild%s | Players%s | Elapsed%s",
                nCopies(" ", serverNameJustify - "Server".length()),
                nCopies(" ", guildNameJustify - "Guild".length()),
                nCopies(" ", playersJustify - "Players".length()),
                nCopies(" ", elapsedFieldJustify - "Elapsed".length())
        ));
        ret.add(String.format(
                "%s-+-%s-+-%s-+-%s-",
                nCopies("-", serverNameJustify),
                nCopies("-", guildNameJustify),
                nCopies("-", playersJustify),
                nCopies("-", elapsedFieldJustify)
        ));

        for (Display display : displays) {
            ret.add(String.format(
                    "%s%s | %s%s | %s%s | %s%s",
                    display.server, nCopies(" ", serverNameJustify - display.server.length()),
                    display.guild, nCopies(" ", guildNameJustify - display.guild.length()),
                    nCopies(" ", playersJustify - display.players.length()), display.players,
                    nCopies(" ", elapsedFieldJustify - display.elapsed.length()), display.elapsed
            ));
        }

        ret.add("");

        DateFormat dateFormat = customDateFormat.getDateFormat().getSecondFormat();
        dateFormat.setTimeZone(customTimeZone.getTimeZoneInstance());

        Date lastUpdate = wars.isEmpty()
                ? notYetStartedWars.get(0).getUpdatedAt()
                : wars.get(0).getLastUp();
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

    private static String nCopies(String s, int n) {
        return String.join("", Collections.nCopies(n, s));
    }
}
