package commands.guild;

import api.mojang.MojangApi;
import api.mojang.structs.NullableUUID;
import api.wynn.WynnApi;
import api.wynn.structs.Player;
import app.Bot;
import commands.base.GenericCommand;
import db.model.dateFormat.CustomDateFormat;
import db.model.guild.Guild;
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
import utils.UUID;
import utils.rateLimit.RateLimitException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class PlayerWarStats extends GenericCommand {
    private final MojangApi mojangApi;
    private final WynnApi wynnApi;
    private final WarPlayerRepository warPlayerRepository;
    private final WarLogRepository warLogRepository;
    private final TerritoryLogRepository territoryLogRepository;
    private final GuildWarLogRepository guildWarLogRepository;
    private final ReactionManager reactionManager;
    private final GuildRepository guildRepository;
    private final DateFormatRepository dateFormatRepository;
    private final TimeZoneRepository timeZoneRepository;

    public PlayerWarStats(Bot bot) {
        this.mojangApi = new MojangApi(bot.getLogger());
        this.wynnApi = new WynnApi(bot.getLogger(), bot.getProperties().wynnTimeZone);
        this.warPlayerRepository = bot.getDatabase().getWarPlayerRepository();
        this.warLogRepository = bot.getDatabase().getWarLogRepository();
        this.territoryLogRepository = bot.getDatabase().getTerritoryLogRepository();
        this.guildWarLogRepository = bot.getDatabase().getGuildWarLogRepository();
        this.reactionManager = bot.getReactionManager();
        this.guildRepository = bot.getDatabase().getGuildRepository();
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"guild", "g"}, {"playerWarStats", "pWarStats", "pStats", "pws"}};
    }

    @Override
    public @NotNull String syntax() {
        return "g playerwarstats <player name|uuid>";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Shows war activities of a player.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                        .setAuthor("Player War Stats Help")
                        .setDescription("Shows war activities of a player.")
                        .addField("Syntax",
                                "`" + this.syntax() + "`",
                                false
                        )
        ).build();
    }

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        if (args.length <= 2) {
            respond(event, this.longHelp());
            return;
        }

        String specified = args[2];
        UUID uuid;
        String playerName;
        if (UUID.isUUID(specified)) {
            uuid = new UUID(specified);
            playerName = null;
        } else {
            List<String> toGet = new ArrayList<>();
            playerName = specified;
            toGet.add(specified);
            Map<String, NullableUUID> retrieved = mojangApi.getUUIDsIterative(toGet);
            if (retrieved == null) {
                respondError(event, "Something went wrong while retrieving player UUID...");
                return;
            }
            if (!retrieved.containsKey(specified) || (uuid = retrieved.get(specified).getUuid()) == null) {
                respond(event, String.format("Failed to retrieve player UUID for %s. " +
                        "Please check the username.", specified));
                return;
            }
        }

        Player player;
        try {
            player = this.wynnApi.getPlayerStats(uuid.toStringWithHyphens(),  false);
        } catch (RateLimitException e) {
            respondException(event, e.getMessage());
            return;
        }
        String guildPrefix;
        if (player != null && player.getGuildInfo().getName() != null) {
            Guild guild = this.guildRepository.findOne(() -> player.getGuildInfo().getName());
            guildPrefix = guild != null ? guild.getPrefix() : null;
        } else {
            guildPrefix = null;
        }

        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);
        CustomDateFormat customDateFormat = this.dateFormatRepository.getDateFormat(event);

        if (maxPage(uuid) == 0) {
            respond(event, format(uuid, playerName, player, guildPrefix,0, customTimeZone, customDateFormat));
            return;
        }

        respond(event, format(uuid, playerName, player, guildPrefix, 0, customTimeZone, customDateFormat), msg -> {
            MultipageHandler handler = new MultipageHandler(
                    msg,
                    page -> new MessageBuilder(format(uuid, playerName, player, guildPrefix, page, customTimeZone, customDateFormat)).build(),
                    () -> maxPage(uuid)
            );
            this.reactionManager.addEventListener(handler);
        });
    }

    private static final int LOGS_PER_PAGE = 5;

    private int getLogCount(UUID playerUUID) {
        return this.warPlayerRepository.countOfPlayer(playerUUID);
    }

    private int maxPage(UUID playerUUID) {
        return (getLogCount(playerUUID) - 1) / LOGS_PER_PAGE;
    }

    @NotNull
    private String format(@NotNull UUID playerUUID,
                          @Nullable String playerName,
                          @Nullable Player player,
                          @Nullable String guildPrefix,
                          int page,
                          @NotNull CustomTimeZone timeZone,
                          @NotNull CustomDateFormat dateFormat) {
        int min = page * LOGS_PER_PAGE;
        List<WarPlayer> logs = this.warPlayerRepository.getLogsOfPlayer(playerUUID, LOGS_PER_PAGE, min);
        if (logs == null) {
            return "Something went wrong while retrieving data...";
        }
        List<Integer> warLogIds = logs.stream().map(WarPlayer::getWarLogId).collect(Collectors.toList());

        // Does not necessarily contain all war log ids (in case player's guild was unknown)
        List<GuildWarLog> guildWarLogs = this.guildWarLogRepository.findAllOfWarLogIdIn(warLogIds);
        List<WarLog> warLogs = this.warLogRepository.findAllIn(warLogIds);
        if (guildWarLogs == null || warLogs == null) {
            return "Something went wrong while retrieving data...";
        }

        List<Integer> territoryLogIds = guildWarLogs.stream().filter(l -> l.getTerritoryLogId() != null)
                .map(GuildWarLog::getTerritoryLogId).collect(Collectors.toList());
        List<TerritoryLog> territoryLogs = this.territoryLogRepository.findAllIn(territoryLogIds);
        if (territoryLogs == null) {
            return "Something went wrong while retrieving data...";
        }

        Map<Integer, GuildWarLog> guildWarLogMap = guildWarLogs.stream().filter(l -> l.getWarLogId() != null)
                .collect(Collectors.toMap(GuildWarLog::getWarLogId, l -> l));
        Map<Integer, TerritoryLog> territoryLogMap = territoryLogs.stream()
                .collect(Collectors.toMap(TerritoryLog::getId, l -> l));

        int count = getLogCount(playerUUID);
        int success = this.warPlayerRepository.countSuccessWars(playerUUID);
        int survived = this.warPlayerRepository.countSurvivedWars(playerUUID);
        if (count == -1 || success == -1 || survived == -1) {
            return "Something went wrong while retrieving data...";
        }

        // actual format starts here

        List<String> ret = new ArrayList<>();
        ret.add("```ml");
        ret.add("---- Player War History ----");
        ret.add("");
        if (player != null) {
            if (player.getGuildInfo().getName() != null) {
                if (guildPrefix != null) {
                    ret.add(String.format("%s - %s [%s] (%s)",
                            player.getUsername(), player.getGuildInfo().getName(),
                            guildPrefix, player.getGuildInfo().getRank()
                    ));
                } else {
                    ret.add(String.format("%s - %s (%s)",
                            player.getUsername(), player.getGuildInfo().getName(),
                            player.getGuildInfo().getRank()
                    ));
                }
            } else {
                ret.add(player.getUsername());
            }
        } else if (playerName != null) {
            ret.add(playerName);
        }
        ret.add("UUID: " + playerUUID.toStringWithHyphens());
        ret.add("");

        // success bar
        ret.add(createBar("Success", "Success", success, count));
        ret.add("");

        // survive bar
        ret.add(createBar("Survived", "Survive", survived, count));
        ret.add("");

        warLogs.sort((l1, l2) -> l2.getId() - l1.getId());
        int seq = min;
        for (WarLog warLog : warLogs) {
            seq++;

            GuildWarLog guildWarLog = guildWarLogMap.getOrDefault(warLog.getId(), null);
            TerritoryLog territoryLog = guildWarLog != null && guildWarLog.getTerritoryLogId() != null
                    ? territoryLogMap.getOrDefault(guildWarLog.getTerritoryLogId(), null)
                    : null;
            ret.add(formatSingleLog(seq, warLog, territoryLog, dateFormat, timeZone));
            ret.add("");
        }

        int maxPage = (count - 1) / LOGS_PER_PAGE;
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

    private static String createBar(String namePP, String name, int targetCount, int totalCount) {
        List<String> ret = new ArrayList<>();

        double rate = (double) targetCount * 100d / (double) (totalCount != 0 ? totalCount : 1);
        String rateStr = String.format("%.1f", rate);

        ret.add(String.format("%s %s Wars / %s Total Wars", targetCount, namePP, totalCount));
        ret.add(String.format("= %s%% %s Rate", rateStr, name));

        int bars = BigDecimal.valueOf(rate).divide(BigDecimal.valueOf(5), 0, RoundingMode.HALF_DOWN).intValue();
        bars = Math.min(Math.max(bars, 0), 20);
        ret.add(String.format("[%s%s]", nCopies("=", bars), nCopies("-", 20 - bars)));

        return String.join("\n", ret);
    }

    private static String nCopies(String s, int n) {
        return String.join("", Collections.nCopies(n, s));
    }

    @NotNull
    private static String formatSingleLog(int seq,
                                          @NotNull WarLog warLog,
                                          @Nullable TerritoryLog territoryLog,
                                          @NotNull CustomDateFormat customDateFormat,
                                          @NotNull CustomTimeZone customTimeZone) {
        DateFormat formatter = customDateFormat.getDateFormat().getSecondFormat();
        formatter.setTimeZone(customTimeZone.getTimeZoneInstance());

        if (territoryLog == null) {
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
            // Assert territory acquired, and war was a success
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
        }
    }

    private static String formatPlayers(List<WarPlayer> players) {
        return players.stream().map(p -> p.hasExited() ? "(" + p.getPlayerName() + ")" : p.getPlayerName()).
                collect(Collectors.joining(", "));
    }
}