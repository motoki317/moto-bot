package commands.guild;

import app.Bot;
import commands.base.GenericCommand;
import db.model.dateFormat.CustomDateFormat;
import db.model.guildWarLog.GuildWarLog;
import db.model.territoryLog.TerritoryLog;
import db.model.timezone.CustomTimeZone;
import db.model.warLog.WarLog;
import db.model.warPlayer.WarPlayer;
import db.repository.base.*;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import update.multipage.MultipageHandler;
import update.reaction.ReactionManager;
import utils.FormatUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class GuildWarStats extends GenericCommand {
    private final ReactionManager reactionManager;
    private final TimeZoneRepository timeZoneRepository;
    private final DateFormatRepository dateFormatRepository;
    private final GuildWarLogRepository guildWarLogRepository;
    private final WarLogRepository warLogRepository;
    private final TerritoryLogRepository territoryLogRepository;

    private final GuildNameResolver guildNameResolver;

    public GuildWarStats(Bot bot) {
        this.reactionManager = bot.getReactionManager();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
        this.guildWarLogRepository = bot.getDatabase().getGuildWarLogRepository();
        this.warLogRepository = bot.getDatabase().getWarLogRepository();
        this.territoryLogRepository = bot.getDatabase().getTerritoryLogRepository();

        this.guildNameResolver = new GuildNameResolver(
                bot.getResponseManager(),
                bot.getDatabase().getGuildRepository()
        );
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"guild", "g"}, {"warStats", "wStats", "ws"}};
    }

    @Override
    public @NotNull String syntax() {
        return "g warstats <guild name>";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Shows war activities of a guild.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                .setAuthor("Guild War Stats Help")
                .setDescription("Shows war activities of a guild.")
                .addField("Syntax",
                        "`" + this.syntax() + "`",
                        false
                )
        ).build();
    }

    private static final int LOGS_PER_PAGE = 5;

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        if (args.length < 3) {
            respond(event, "Please specify a guild name to see the guild's war activities!");
            return;
        }

        String guildName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        this.guildNameResolver.resolve(
                guildName,
                event.getTextChannel(),
                event.getAuthor(),
                (resolvedName, prefix) -> handleChosenGuild(event, resolvedName, prefix),
                reason -> respondError(event, reason)
        );
    }

    private void handleChosenGuild(MessageReceivedEvent event, @NotNull String guildName, @Nullable String guildPrefix) {
        int count = this.guildWarLogRepository.countGuildLogs(guildName);
        if (count < 0) {
            respondError(event, "Something went wrong while retrieving data...");
            return;
        } else if (count == 0) {
            respond(event, String.format("Specified guild `%s` does not seem to have any logged war activities...", guildName));
            return;
        }

        CustomTimeZone timeZone = this.timeZoneRepository.getTimeZone(event);
        CustomDateFormat dateFormat = this.dateFormatRepository.getDateFormat(event);

        if (count <= LOGS_PER_PAGE) {
            respond(event, format(guildName, guildPrefix, 0, timeZone, dateFormat));
            return;
        }

        respond(event, format(guildName, guildPrefix, 0, timeZone, dateFormat), success -> {
            MultipageHandler handler = new MultipageHandler(
                    success,
                    event.getAuthor().getIdLong(),
                    page -> new MessageBuilder(format(guildName, guildPrefix, page, timeZone, dateFormat)).build(),
                    () -> maxPage(guildName)
            );
            this.reactionManager.addEventListener(handler);
        });
    }

    private int maxPage(String guildName) {
        int count = this.guildWarLogRepository.countGuildLogs(guildName);
        return count > 0 ? (count - 1) / LOGS_PER_PAGE : -1;
    }

    private String format(String guildName, @Nullable String guildPrefix, int page, CustomTimeZone timeZone, CustomDateFormat dateFormat) {
        // Retrieve data
        List<GuildWarLog> logs = this.guildWarLogRepository.findGuildLogs(guildName, LOGS_PER_PAGE, page * LOGS_PER_PAGE);
        int maxPage = this.maxPage(guildName);

        int successWarCount = this.guildWarLogRepository.countSuccessWars(guildName);
        int totalWarCount = this.guildWarLogRepository.countTotalWars(guildName);

        if (logs == null || maxPage < 0 || successWarCount < 0 || totalWarCount < 0) {
            return "Something went wrong while retrieving data...";
        }

        // To avoid n+1 (although logs per page is 5)
        List<WarLog> retrievedWarLogs = this.warLogRepository.findAllIn(
                logs.stream().filter(log -> log.getWarLogId() != null)
                        .map(GuildWarLog::getWarLogId).collect(Collectors.toList())
        );
        List<TerritoryLog> retrievedTerritoryLogs = this.territoryLogRepository.findAllIn(
                logs.stream().filter(log -> log.getTerritoryLogId() != null)
                        .map(GuildWarLog::getTerritoryLogId).collect(Collectors.toList())
        );
        if (retrievedWarLogs == null || retrievedTerritoryLogs == null) {
            return "Something went wrong while retrieving data...";
        }

        Map<Integer, WarLog> warLogs = retrievedWarLogs.stream()
                .collect(Collectors.toMap(WarLog::getId, log -> log));
        Map<Integer, TerritoryLog> territoryLogs = retrievedTerritoryLogs.stream()
                .collect(Collectors.toMap(TerritoryLog::getId, log -> log));

        // Format start
        List<String> ret = new ArrayList<>();

        ret.add("```ml");
        ret.add("---- War History ----");
        ret.add("");
        ret.add(guildName + (guildPrefix != null ? String.format(" [%s]", guildPrefix) : ""));
        ret.add("");

        // e.g. "95.0"
        double successRate = (double) successWarCount * 100d / (double) (totalWarCount != 0 ? totalWarCount : 1);
        String warSuccessRate = String.format("%.1f", successRate);

        ret.add(String.format("%s Success Wars / %s Total Wars", successWarCount, totalWarCount));
        ret.add(String.format("= %s%% Success Rate", warSuccessRate));

        int successBars = BigDecimal.valueOf(successRate).divide(BigDecimal.valueOf(5), 0, RoundingMode.HALF_DOWN).intValue();
        successBars = Math.min(Math.max(successBars, 0), 20);
        ret.add(String.format("[%s%s]", nCopies("=", successBars), nCopies("-", 20 - successBars)));

        ret.add("");

        logs.sort((l1, l2) -> l2.getId() - l1.getId());
        int seq = LOGS_PER_PAGE * page;
        for (GuildWarLog log : logs) {
            seq++;
            ret.add(formatSingleLog(seq, guildName, log, warLogs, territoryLogs, timeZone, dateFormat));
            ret.add("");
        }

        ret.add(String.format("< showing page %s of %s >", page + 1, maxPage + 1));
        ret.add("");

        Date now = new Date();
        DateFormat formatter = dateFormat.getDateFormat().getSecondFormat();
        formatter.setTimeZone(timeZone.getTimeZoneInstance());
        ret.add(String.format("Current Time: %s", formatter.format(now)));
        ret.add(String.format("Timezone Offset: %s", timeZone.getTimezone()));

        ret.add("```");

        return String.join("\n", ret);
    }

    private static String nCopies(String s, int n) {
        return String.join("", Collections.nCopies(n, s));
    }

    @NotNull
    private String formatSingleLog(int seq,
                                   String guildName,
                                   GuildWarLog log,
                                   Map<Integer, WarLog> warLogs,
                                   Map<Integer, TerritoryLog> territoryLogs,
                                   CustomTimeZone timeZone,
                                   CustomDateFormat dateFormat) {
        DateFormat formatter = dateFormat.getDateFormat().getSecondFormat();
        formatter.setTimeZone(timeZone.getTimeZoneInstance());

        if (log.getWarLogId() == null) {
            if (log.getTerritoryLogId() == null) {
                // both war & territory log id is null
                return String.format("%s. %s", seq, "An error occurred...");
            } else {
                TerritoryLog territoryLog = territoryLogs.get(log.getTerritoryLogId());
                String event;
                if (territoryLog.getNewGuildName().equalsIgnoreCase(guildName)) {
                    // territory acquired without war
                    event = "(No war)";
                } else {
                    // territory lost
                    event = "Territory lost!";
                }
                return String.format("%s. %s : %s\n" +
                                "   %s (%s) → %s (%s)\n" +
                                "   Time: %s\n" +
                                "   Held for %s",
                        seq, event, territoryLog.getTerritoryName(),
                        territoryLog.getOldGuildName(), territoryLog.getOldGuildTerrAmt(),
                        territoryLog.getNewGuildName(), territoryLog.getNewGuildTerrAmt(),
                        formatter.format(territoryLog.getAcquired()),
                        FormatUtils.formatReadableTime(territoryLog.getTimeDiff() / 1000L, false, "s")
                );
            }
        } else {
            WarLog warLog = warLogs.get(log.getWarLogId());
            if (log.getTerritoryLogId() == null) {
                // War ongoing, or war lost
                String event;
                if (warLog.isEnded()) {
                    Date now = new Date();
                    // If it ended within 90s from now, deem it as just ended
                    // Else, deem it as war lost
                    if (now.getTime() - warLog.getLastUp().getTime() < TimeUnit.SECONDS.toMillis(90)) {
                        event = "War ended.";
                    } else {
                        event = "War lost...";
                    }
                } else {
                    // War ongoing
                    event = "In fight...";
                }
                return String.format("%s. %s : %s\n" +
                                "   War Time: %s ~ %s\n" +
                                "   Players: %s",
                        seq, warLog.getServerName(), event,
                        formatter.format(warLog.getCreatedAt()), formatter.format(warLog.getLastUp()),
                        formatPlayers(warLog.getPlayers())
                );
            } else {
                TerritoryLog territoryLog = territoryLogs.get(log.getTerritoryLogId());
                // Assert territory acquired, and war was a success
                if (territoryLog.getNewGuildName().equals(guildName)) {
                    return String.format("%s. %s : %s\n" +
                            "   %s (%s) → %s (%s)\n" +
                            "   War Time: %s ~ %s\n" +
                            "   Players: %s",
                            seq, warLog.getServerName(), territoryLog.getTerritoryName(),
                            territoryLog.getOldGuildName(), territoryLog.getOldGuildTerrAmt(),
                            territoryLog.getNewGuildName(), territoryLog.getNewGuildTerrAmt(),
                            formatter.format(warLog.getCreatedAt()), formatter.format(warLog.getLastUp()),
                            formatPlayers(warLog.getPlayers())
                    );
                } else {
                    return String.format("%s. %s", seq, "An error occurred...");
                }
            }
        }
    }

    private static String formatPlayers(List<WarPlayer> players) {
        return players.stream().map(p -> p.hasExited() ? "(" + p.getPlayerName() + ")" : p.getPlayerName()).
                collect(Collectors.joining(", "));
    }
}
