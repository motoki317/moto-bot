package commands.guild;

import api.mojang.MojangApi;
import api.mojang.structs.NullableUUID;
import api.wynn.WynnApi;
import api.wynn.structs.Player;
import app.Bot;
import commands.base.GenericCommand;
import commands.event.CommandEvent;
import commands.event.message.SentMessage;
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
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.ArgumentParser;
import utils.UUID;
import utils.rateLimit.RateLimitException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PlayerWarStats extends GenericCommand {
    private final MojangApi mojangApi;
    private final WynnApi wynnApi;
    private final WarPlayerRepository warPlayerRepository;
    private final WarLogRepository warLogRepository;
    private final TerritoryLogRepository territoryLogRepository;
    private final GuildWarLogRepository guildWarLogRepository;
    private final GuildRepository guildRepository;
    private final DateFormatRepository dateFormatRepository;
    private final TimeZoneRepository timeZoneRepository;

    private final GuildNameResolver guildNameResolver;

    public PlayerWarStats(Bot bot) {
        this.mojangApi = new MojangApi(bot.getLogger());
        this.wynnApi = new WynnApi(bot.getLogger());
        this.warPlayerRepository = bot.getDatabase().getWarPlayerRepository();
        this.warLogRepository = bot.getDatabase().getWarLogRepository();
        this.territoryLogRepository = bot.getDatabase().getTerritoryLogRepository();
        this.guildWarLogRepository = bot.getDatabase().getGuildWarLogRepository();
        this.guildRepository = bot.getDatabase().getGuildRepository();
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();

        this.guildNameResolver = new GuildNameResolver(
                bot.getDatabase().getGuildRepository(),
                bot.getButtonClickManager()
        );
    }

    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"guild", "g"}, {"playerWarStats", "pWarStats", "pwStats", "pStats", "pws"}};
    }

    @Override
    public @NotNull String[] slashName() {
        return new String[]{"g", "pws"};
    }

    @Override
    public @NotNull OptionData[] slashOptions() {
        return new OptionData[]{
                new OptionData(OptionType.STRING, "player", "Name or UUID of a player", true)
        };
    }

    @Override
    public @NotNull String syntax() {
        return "g playerWarStats <player name|uuid> [-g <guild name>]";
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
                        ).addField("Optional arguments", String.join("\n",
                                "**-g <guild name>** : Specify a guild name to retrieve a guild-scoped logs of the player."
                        ), false)
                        .build()
        ).build();
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(3);
    }

    @NotNull
    private String getPlayerNameDisplay(@NotNull UUID uuid, @Nullable String playerName) throws RateLimitException {
        Player player = this.wynnApi.getPlayerStats(uuid.toStringWithHyphens(), false);
        String guildPrefix;
        if (player != null && player.getGuildInfo().getName() != null) {
            Guild guild = this.guildRepository.findOne(() -> player.getGuildInfo().getName());
            guildPrefix = guild != null ? guild.getPrefix() : null;
        } else {
            guildPrefix = null;
        }

        if (player != null) {
            if (player.getGuildInfo().getName() != null) {
                // Player is in a guild
                return String.format("%s - %s [%s] (%s)\nUUID: %s",
                        player.getUsername(), player.getGuildInfo().getName(),
                        guildPrefix != null ? guildPrefix : "???", player.getGuildInfo().getRank(),
                        uuid.toStringWithHyphens()
                );
            } else {
                // Player is not in a guild
                return String.format("%s\nUUID: %s", player.getUsername(), uuid.toStringWithHyphens());
            }
        } else if (playerName != null) {
            // Failed to get player data from Wynn API
            return String.format("%s\nUUID: %s", playerName, uuid.toStringWithHyphens());
        }

        return String.format("UUID: %s", uuid.toStringWithHyphens());
    }

    @Override
    public void process(@NotNull CommandEvent event, @NotNull String[] args) {
        if (args.length <= 2) {
            event.reply(this.longHelp());
            return;
        }

        String specified = args[2];
        UUID uuid;
        String playerName;
        if (UUID.isUUID(specified)) {
            uuid = new UUID(specified);
            playerName = null;
        } else {
            playerName = specified;
            Map<String, NullableUUID> retrieved = mojangApi.getUUIDsIterative(Collections.singletonList(specified));
            if (retrieved == null) {
                event.replyError("Something went wrong while retrieving player UUID...");
                return;
            }
            if (!retrieved.containsKey(specified) || (uuid = retrieved.get(specified).uuid()) == null) {
                event.reply(String.format("Failed to retrieve player UUID for %s. " +
                        "Please check the username.", specified));
                return;
            }
        }

        String playerNameDisplay;
        try {
            playerNameDisplay = getPlayerNameDisplay(uuid, playerName);
        } catch (RateLimitException e) {
            event.replyException(e.getMessage());
            return;
        }

        Map<String, String> parsedArgs = new ArgumentParser(args).getArgumentMap();
        if (parsedArgs.containsKey("g")) {
            this.guildNameResolver.resolve(
                    parsedArgs.get("g"),
                    event,
                    (next, guildName, prefix) -> respondLeaderboard(event, next, uuid, playerNameDisplay, guildName)
            );
            return;
        }

        event.reply(new EmbedBuilder().setDescription("Processing...").build(), next ->
                respondLeaderboard(event, next, uuid, playerNameDisplay, null));
    }

    private void respondLeaderboard(@NotNull CommandEvent event, SentMessage next, UUID uuid, String playerNameDisplay, String guildName) {
        CustomTimeZone customTimeZone = this.timeZoneRepository.getTimeZone(event);
        CustomDateFormat customDateFormat = this.dateFormatRepository.getDateFormat(event);

        if (maxPage(uuid, guildName) == 0) {
            next.editMessage(format(0, uuid, playerNameDisplay, guildName, customTimeZone, customDateFormat));
            return;
        }

        Function<Integer, Message> pages = page -> new MessageBuilder(format(page, uuid, playerNameDisplay, guildName, customTimeZone, customDateFormat)).build();
        next.editMultiPage(event.getBot(), pages, () -> maxPage(uuid, guildName));
    }

    private static final int LOGS_PER_PAGE = 5;
    private static final int PLAYERS_LIST_LENGTH = 1500 / LOGS_PER_PAGE;

    private int getTotalWars(@NotNull UUID playerUUID, @Nullable String guildName) {
        return guildName == null
                ? this.warPlayerRepository.countOfPlayer(playerUUID)
                : this.warPlayerRepository.countOfPlayer(playerUUID, guildName);
    }

    private int getSuccessWars(@NotNull UUID playerUUID, @Nullable String guildName) {
        return guildName == null
                ? this.warPlayerRepository.countSuccessWars(playerUUID)
                : this.warPlayerRepository.countSuccessWars(playerUUID, guildName);
    }

    private int getSurvivedWars(@NotNull UUID playerUUID, @Nullable String guildName) {
        return guildName == null
                ? this.warPlayerRepository.countSurvivedWars(playerUUID)
                : this.warPlayerRepository.countSurvivedWars(playerUUID, guildName);
    }

    private int maxPage(@NotNull UUID playerUUID, @Nullable String guildName) {
        return (getTotalWars(playerUUID, guildName) - 1) / LOGS_PER_PAGE;
    }

    private record PlayerWarLogPortion(List<WarLog> warLogs,
                                       Map<Integer, GuildWarLog> guildWarLogMap,
                                       Map<Integer, TerritoryLog> territoryLogMap) {
    }

    private PlayerWarLogPortion getPlayerWarLogPortion(int page, @NotNull UUID playerUUID, @Nullable String guildName) {
        int offset = page * LOGS_PER_PAGE;
        List<WarPlayer> logs = guildName == null
                ? this.warPlayerRepository.getLogsOfPlayer(playerUUID, LOGS_PER_PAGE, offset)
                : this.warPlayerRepository.getLogsOfPlayer(playerUUID, guildName, LOGS_PER_PAGE, offset);
        if (logs == null) {
            throw new RuntimeException("Something went wrong while retrieving data...");
        }
        List<Integer> warLogIds = logs.stream().map(WarPlayer::getWarLogId).collect(Collectors.toList());

        // Does not necessarily contain all war log ids (in case player's guild was unknown)
        List<GuildWarLog> guildWarLogs = this.guildWarLogRepository.findAllOfWarLogIdIn(warLogIds);
        List<WarLog> warLogs = this.warLogRepository.findAllIn(warLogIds);
        if (guildWarLogs == null || warLogs == null) {
            throw new RuntimeException("Something went wrong while retrieving data...");
        }

        List<Integer> territoryLogIds = guildWarLogs.stream().map(GuildWarLog::getTerritoryLogId)
                .filter(Objects::nonNull).collect(Collectors.toList());
        List<TerritoryLog> territoryLogs = this.territoryLogRepository.findAllIn(territoryLogIds);
        if (territoryLogs == null) {
            throw new RuntimeException("Something went wrong while retrieving data...");
        }

        // war logs to guild war log / territory log map
        Map<Integer, GuildWarLog> guildWarLogMap = guildWarLogs.stream().filter(l -> l.getWarLogId() != null)
                .collect(Collectors.toMap(GuildWarLog::getWarLogId, l -> l));
        Map<Integer, TerritoryLog> territoryLogMap = territoryLogs.stream()
                .collect(Collectors.toMap(TerritoryLog::getId, l -> l));

        return new PlayerWarLogPortion(warLogs, guildWarLogMap, territoryLogMap);
    }

    @NotNull
    private String format(int page,
                          UUID playerUUID,
                          String playerNameDisplay,
                          @Nullable String guildName,
                          CustomTimeZone timeZone,
                          CustomDateFormat dateFormat) {
        PlayerWarLogPortion pwl;
        try {
            pwl = getPlayerWarLogPortion(page, playerUUID, guildName);
        } catch (RuntimeException e) {
            return e.getMessage();
        }

        int count = this.getTotalWars(playerUUID, guildName);
        int success = this.getSuccessWars(playerUUID, guildName);
        int survived = this.getSurvivedWars(playerUUID, guildName);
        if (count == -1 || success == -1 || survived == -1) {
            return "Something went wrong while retrieving data...";
        }

        List<String> ret = new ArrayList<>();
        ret.add("```ml");
        ret.add("---- Player War History ----");
        ret.add("");
        ret.add(playerNameDisplay);
        ret.add("");
        if (guildName != null) {
            ret.add("Guild Scoped: " + guildName);
            ret.add("");
        }

        // success bar
        ret.add(createBar("Success", "Success", success, count));
        ret.add("");

        // survive bar
        ret.add(createBar("Survived", "Survive", survived, count));
        ret.add("");

        pwl.warLogs.sort((l1, l2) -> l2.getId() - l1.getId());
        int seq = page * LOGS_PER_PAGE;
        for (WarLog warLog : pwl.warLogs) {
            seq++;

            GuildWarLog guildWarLog = pwl.guildWarLogMap.getOrDefault(warLog.getId(), null);
            TerritoryLog territoryLog = guildWarLog != null && guildWarLog.getTerritoryLogId() != null
                    ? pwl.territoryLogMap.getOrDefault(guildWarLog.getTerritoryLogId(), null)
                    : null;
            ret.add(formatSingleLog(seq, warLog, territoryLog, dateFormat, timeZone));
            ret.add("");
        }

        makeFooter(page, timeZone, dateFormat, count, ret);

        ret.add("```");

        return String.join("\n", ret);
    }

    private static void makeFooter(int page, @NotNull CustomTimeZone timeZone, @NotNull CustomDateFormat dateFormat,
                                   int count, List<String> ret) {
        int maxPage = (count - 1) / LOGS_PER_PAGE;
        ret.add(String.format("< page %s of %s >", page + 1, maxPage + 1));
        ret.add("");

        DateFormat formatter = dateFormat.getDateFormat().getSecondFormat();
        formatter.setTimeZone(timeZone.getTimeZoneInstance());
        ret.add(String.format("Current Time: %s (%s)", formatter.format(new Date()), timeZone.getFormattedTime()));
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
            return String.format("""
                            %s. %s : %s
                               War Time: %s ~ %s
                               Players: %s""",
                    seq, warLog.getServerName(), event,
                    formatter.format(warLog.getCreatedAt()), formatter.format(warLog.getLastUp()),
                    formatPlayers(warLog.getPlayers())
            );
        } else {
            // Assert territory acquired, and war was a success
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
