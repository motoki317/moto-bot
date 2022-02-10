package commands.guild;

import app.Bot;
import commands.base.GenericCommand;
import commands.event.CommandEvent;
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
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.FormatUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class GuildWarStats extends GenericCommand {
    private final TimeZoneRepository timeZoneRepository;
    private final DateFormatRepository dateFormatRepository;
    private final GuildWarLogRepository guildWarLogRepository;
    private final WarLogRepository warLogRepository;
    private final TerritoryLogRepository territoryLogRepository;

    private final GuildNameResolver guildNameResolver;

    public GuildWarStats(Bot bot) {
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
    public @NotNull String[] slashName() {
        return new String[]{"g", "ws"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{
                new OptionData(OptionType.STRING, "guild", "Name or prefix of a guild", true)
        };
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

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    private static final int LOGS_PER_PAGE = 5;
    private static final int PLAYERS_LIST_LENGTH = 1500 / LOGS_PER_PAGE;

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        if (args.length < 3) {
            event.reply("Please specify a guild name to see the guild's war activities!");
            return;
        }

        String guildName = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        this.guildNameResolver.resolve(
                guildName,
                event.getChannel(),
                event.getAuthor(),
                (resolvedName, prefix) -> handleChosenGuild(event, resolvedName, prefix),
                event::replyError
        );
    }

    private void handleChosenGuild(CommandEvent event, @NotNull String guildName, @Nullable String guildPrefix) {
        int count = this.guildWarLogRepository.countGuildLogs(guildName);
        if (count < 0) {
            event.replyError("Something went wrong while retrieving data...");
            return;
        } else if (count == 0) {
            event.reply(String.format("Specified guild `%s` does not seem to have any logged war activities...", guildName));
            return;
        }

        CustomTimeZone timeZone = this.timeZoneRepository.getTimeZone(event);
        CustomDateFormat dateFormat = this.dateFormatRepository.getDateFormat(event);

        if (count <= LOGS_PER_PAGE) {
            event.reply(format(guildName, guildPrefix, 0, timeZone, dateFormat));
            return;
        }

        Function<Integer, Message> pages = page -> new MessageBuilder(format(guildName, guildPrefix, page, timeZone, dateFormat)).build();
        event.replyMultiPage(pages.apply(0), pages, () -> maxPage(guildName));
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
                logs.stream().map(GuildWarLog::getWarLogId)
                        .filter(Objects::nonNull).collect(Collectors.toList())
        );
        List<TerritoryLog> retrievedTerritoryLogs = this.territoryLogRepository.findAllIn(
                logs.stream().map(GuildWarLog::getTerritoryLogId)
                        .filter(Objects::nonNull).collect(Collectors.toList())
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
                return String.format("""
                                %s. %s : %s
                                   %s (%s) → %s (%s)
                                   Time: %s
                                   Held for %s""",
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
                return String.format("""
                                %s. %s : %s
                                   War Time: %s ~ %s
                                   Players: %s""",
                        seq, warLog.getServerName(), event,
                        formatter.format(warLog.getCreatedAt()), formatter.format(warLog.getLastUp()),
                        formatPlayers(warLog.getPlayers())
                );
            } else {
                TerritoryLog territoryLog = territoryLogs.get(log.getTerritoryLogId());
                // Assert territory acquired, and war was a success
                if (territoryLog.getNewGuildName().equals(guildName)) {
                    return String.format("""
                                    %s. %s : %s
                                       %s (%s) → %s (%s)
                                       War Time: %s ~ %s
                                       Players: %s""",
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
        String ret = players.stream().map(p -> p.hasExited() ? "(" + p.getPlayerName() + ")" : p.getPlayerName()).
                collect(Collectors.joining(", "));
        if (ret.length() > PLAYERS_LIST_LENGTH) {
            return ret.substring(0, PLAYERS_LIST_LENGTH) + "...";
        }
        return ret;
    }
}
