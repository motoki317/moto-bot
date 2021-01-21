package heartbeat.tasks;

import api.wynn.WynnApi;
import api.wynn.structs.TerritoryList;
import app.Bot;
import db.model.guildWarLog.GuildWarLog;
import db.model.territory.Territory;
import db.model.territoryLog.TerritoryLog;
import db.model.timezone.CustomTimeZone;
import db.model.track.TrackChannel;
import db.model.track.TrackType;
import db.model.warLog.WarLog;
import db.repository.base.*;
import heartbeat.base.TaskBase;
import log.Logger;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import utils.FormatUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class TerritoryTracker implements TaskBase {
    private final Logger logger;
    private final Object dbLock;
    private final ShardManager manager;
    private final WynnApi wynnApi;
    private final TerritoryRepository territoryRepository;
    private final TerritoryLogRepository territoryLogRepository;
    private final WarLogRepository warLogRepository;
    private final GuildWarLogRepository guildWarLogRepository;
    private final TrackChannelRepository trackChannelRepository;
    private final TimeZoneRepository timeZoneRepository;
    private final DateFormatRepository dateFormatRepository;

    public TerritoryTracker(Bot bot, Object dbLock) {
        this.logger = bot.getLogger();
        this.dbLock = dbLock;
        this.manager = bot.getManager();
        this.wynnApi = new WynnApi(this.logger);
        this.territoryRepository = bot.getDatabase().getTerritoryRepository();
        this.territoryLogRepository = bot.getDatabase().getTerritoryLogRepository();
        this.warLogRepository = bot.getDatabase().getWarLogRepository();
        this.guildWarLogRepository = bot.getDatabase().getGuildWarLogRepository();
        this.trackChannelRepository = bot.getDatabase().getTrackingChannelRepository();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
        this.dateFormatRepository = bot.getDatabase().getDateFormatRepository();
    }

    private static final long TERRITORY_TRACKER_DELAY = TimeUnit.SECONDS.toMillis(30);

    @Override
    public long getFirstDelay() {
        return TimeUnit.SECONDS.toMillis(1);
    }

    @Override
    public long getInterval() {
        return TERRITORY_TRACKER_DELAY;
    }

    @Override
    public @NotNull String getName() {
        return "Territory Tracker";
    }

    @Override
    public void run() {
        TerritoryList territoryList = this.wynnApi.mustGetTerritoryList();
        if (territoryList == null) return;

        List<Territory> territories = new ArrayList<>();
        for (Map.Entry<String, api.wynn.structs.Territory> e : territoryList.getTerritories().entrySet()) {
            try {
                // Skip bad API response
                if (e.getValue().getGuild() == null) continue;
                territories.add(e.getValue().convert());
            } catch (ParseException ex) {
                this.logger.logException("an exception occurred in territory tracker", ex);
                return;
            }
        }

        if (!checkIntegrity(territories)) {
            this.logger.log(0, "Territory Tracker failed to pass timestamp integrity check");
            return;
        }

        int oldLastId;
        int newLastId;
        synchronized (this.dbLock) {
            oldLastId = this.territoryLogRepository.lastInsertId();

            // Update DB
            if (!this.territoryRepository.updateAll(territories)) {
                this.logger.log(0, "Territory tracker: failed to update db");
                return;
            }

            newLastId = this.territoryLogRepository.lastInsertId();
        }

        this.handleTracking(oldLastId, newLastId);
    }

    private boolean checkIntegrity(List<Territory> retrieved) {
        Date storedLatestAcquired = this.territoryRepository.getLatestAcquiredTime();
        if (storedLatestAcquired == null) {
            return true;
        }
        Date retrievedLatestAcquired = retrieved.stream().map(Territory::getAcquired)
                .max(Comparator.comparingLong(Date::getTime)).orElse(null);
        if (retrievedLatestAcquired == null) {
            return false;
        }

        return storedLatestAcquired.getTime() <= retrievedLatestAcquired.getTime();
    }

    @NotNull
    private Map<Integer, String> getCorrespondingWarNames(List<Integer> territoryLogIds) {
        List<GuildWarLog> guildLogs = this.guildWarLogRepository.findAllOfTerritoryLogIdIn(territoryLogIds);
        if (guildLogs == null) {
            this.logger.log(0, "Territory tracker: failed to retrieve corresponding guild logs.");
            return new HashMap<>();
        }
        List<WarLog> warLogs = this.warLogRepository
                .findAllIn(guildLogs.stream().map(GuildWarLog::getWarLogId)
                                .filter(Objects::nonNull).collect(Collectors.toList()));
        if (warLogs == null) {
            this.logger.log(0, "Territory tracker: failed to retrieve corresponding war logs.");
            return new HashMap<>();
        }
        Map<Integer, String> warLogMap = warLogs.stream().collect(Collectors.toMap(WarLog::getId, WarLog::getServerName));
        return guildLogs.stream().filter(l -> l.getWarLogId() != null).collect(Collectors.toMap(
                GuildWarLog::getTerritoryLogId,
                l -> warLogMap.getOrDefault(l.getWarLogId(), "No war")));
    }

    /**
     * Do territory tracking. Sends all territory_log from oldLastId (exclusive) to newLastId (inclusive).
     * @param oldLastId Last max id in territory_log before db update.
     * @param newLastId Current max id in territory_log table after db update.
     */
    private void handleTracking(int oldLastId, int newLastId) {
        List<TerritoryLog> logs = this.territoryLogRepository.findAllInRange(oldLastId, newLastId);

        if (logs == null) {
            this.logger.log(0, "Territory tracker: failed to retrieve last log list. " +
                    "Not sending tracking this time. old id (exclusive): " + oldLastId + ", new id (inclusive): " + newLastId);
            return;
        }

        if (logs.isEmpty()) {
            return;
        }

        Map<Integer, String> serverNames = this.getCorrespondingWarNames(
                logs.stream().map(TerritoryLog::getId).collect(Collectors.toList()));

        List<TrackChannel> allTerritoryTracks = this.trackChannelRepository.findAllOfType(TrackType.TERRITORY_ALL);
        if (allTerritoryTracks == null) {
            this.logger.log(0, "Territory tracker: failed to retrieve tracking channels list. " +
                    "Not sending tracking this time. old id (exclusive): " + oldLastId + ", new id (inclusive): " + newLastId);
            return;
        }

        for (TerritoryLog log : logs) {
            Set<TrackChannel> channelsToSend = new HashSet<>(allTerritoryTracks);

            List<TrackChannel> specificTracksOld = this.trackChannelRepository
                    .findAllOfGuildNameAndType(log.getOldGuildName(), TrackType.TERRITORY_SPECIFIC);
            List<TrackChannel> specificTracksNew = this.trackChannelRepository
                    .findAllOfGuildNameAndType(log.getNewGuildName(), TrackType.TERRITORY_SPECIFIC);
            if (specificTracksOld == null || specificTracksNew == null) {
                return;
            }
            channelsToSend.addAll(specificTracksOld);
            channelsToSend.addAll(specificTracksNew);

            String messageBase = formatBase(log, serverNames.get(log.getId()));
            channelsToSend.forEach(ch -> {
                TextChannel channel = this.manager.getTextChannelById(ch.getChannelId());
                if (channel == null) return;
                channel.sendMessage(messageBase + formatAcquiredTime(log, ch)).queue();
            });
        }
    }

    /**
     * Formats territory log in order to send it to tracking channels.
     * @param log Territory log.
     * @param serverName War server name.
     * @return Formatted string.
     */
    private static String formatBase(TerritoryLog log, @Nullable String serverName) {
        String heldForFormatted = FormatUtils.formatReadableTime(log.getTimeDiff() / 1000, false, "s");

        return String.format(
                "%s: *%s* (%s) → **%s** (%s) [%s]\n" +
                        "    Territory held for %s\n",
                log.getTerritoryName(),
                log.getOldGuildName(), log.getOldGuildTerrAmt(),
                log.getNewGuildName(), log.getNewGuildTerrAmt(),
                serverName != null ? serverName : "No war",
                heldForFormatted
        );
    }

    @NotNull
    private CustomTimeZone getTimeZone(TrackChannel track) {
        return this.timeZoneRepository.getTimeZone(
                track.getGuildId(),
                track.getChannelId()
        );
    }

    @NotNull
    private DateFormat getDateFormat(TrackChannel track) {
        return this.dateFormatRepository.getDateFormat(
                track.getGuildId(),
                track.getChannelId()
        ).getDateFormat().getSecondFormat();
    }

    private String formatAcquiredTime(TerritoryLog log, TrackChannel track) {
        DateFormat trackFormat = getDateFormat(track);
        CustomTimeZone timeZone = getTimeZone(track);
        trackFormat.setTimeZone(timeZone.getTimeZoneInstance());
        return String.format(
                "    Acquired: %s (%s)", trackFormat.format(log.getAcquired()), timeZone.getFormattedTime()
        );
    }
}
