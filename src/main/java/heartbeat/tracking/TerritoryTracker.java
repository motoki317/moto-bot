package heartbeat.tracking;

import api.wynn.WynnApi;
import api.wynn.structs.TerritoryList;
import app.Bot;
import db.model.territory.Territory;
import db.model.territoryLog.TerritoryLog;
import db.model.timezone.CustomTimeZone;
import db.model.track.TrackChannel;
import db.model.track.TrackType;
import db.repository.TimeZoneRepository;
import db.repository.TerritoryLogRepository;
import db.repository.TerritoryRepository;
import db.repository.TrackChannelRepository;
import log.Logger;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import utils.FormatUtils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class TerritoryTracker {
    private final Logger logger;
    private final Object dbLock;
    private final ShardManager manager;
    private final WynnApi wynnApi;
    private final TerritoryRepository territoryRepository;
    private final TerritoryLogRepository territoryLogRepository;
    private final TrackChannelRepository trackChannelRepository;
    private final TimeZoneRepository timeZoneRepository;

    public TerritoryTracker(Bot bot, Object dbLock) {
        this.logger = bot.getLogger();
        this.dbLock = dbLock;
        this.manager = bot.getManager();
        this.wynnApi = new WynnApi(this.logger, bot.getProperties().wynnTimeZone);
        this.territoryRepository = bot.getDatabase().getTerritoryRepository();
        this.territoryLogRepository = bot.getDatabase().getTerritoryLogRepository();
        this.trackChannelRepository = bot.getDatabase().getTrackingChannelRepository();
        this.timeZoneRepository = bot.getDatabase().getTimeZoneRepository();
    }

    public void run() {
        TerritoryList territoryList = this.wynnApi.getTerritoryList();
        if (territoryList == null) return;

        List<Territory> territories = new ArrayList<>();
        for (Map.Entry<String, api.wynn.structs.Territory> e : territoryList.getTerritories().entrySet()) {
            try {
                territories.add(e.getValue().convert());
            } catch (ParseException ex) {
                this.logger.logException("an error occurred in territory tracker", ex);
                return;
            }
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

        List<TrackChannel> allTerritories = this.trackChannelRepository.findAllOfType(TrackType.TERRITORY_ALL);
        List<TrackChannel> allSpecifics = this.trackChannelRepository.findAllOfType(TrackType.TERRITORY_SPECIFIC);
        if (allTerritories == null || allSpecifics == null) {
            this.logger.log(0, "Territory tracker: failed to retrieve tracking channels list. " +
                    "Not sending tracking this time. old id (exclusive): " + oldLastId + ", new id (inclusive): " + newLastId);
            return;
        }

        for (TerritoryLog log : logs) {
            Set<TrackChannel> channelsToSend = new HashSet<>(allTerritories);
            channelsToSend.addAll(
                    allSpecifics.stream().filter(
                            t -> t.getGuildName() != null && (t.getGuildName().equals(log.getOldGuildName()) || t.getGuildName().equals(log.getNewGuildName()))
                    ).collect(Collectors.toList())
            );
            String messageBase = formatBase(log);
            channelsToSend.forEach(ch -> {
                TextChannel channel = this.manager.getTextChannelById(ch.getChannelId());
                if (channel == null) return;
                channel.sendMessage(messageBase + formatAcquiredTime(log, ch)).queue();
            });
        }
    }

    private static final DateFormat trackFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    /**
     * Formats territory log in order to send it to tracking channels.
     * @param log Territory log.
     * @return Formatted string.
     */
    private static String formatBase(TerritoryLog log) {
        String heldForFormatted = FormatUtils.formatReadableTime(log.getTimeDiff() / 1000, false, "s");

        return String.format(
                "%s: *%s* (%s) → **%s** (%s)\n" +
                        "    Territory held for %s\n",
                log.getTerritoryName(), log.getOldGuildName(), log.getOldGuildTerrAmt(), log.getNewGuildName(), log.getNewGuildTerrAmt(),
                heldForFormatted
        );
    }

    private String formatAcquiredTime(TerritoryLog log, TrackChannel track) {
        long guildId = track.getGuildId();
        long channelId = track.getChannelId();
        CustomTimeZone custom = this.timeZoneRepository.getTimeZone(guildId, channelId);
        // TODO: custom format for each channel
        trackFormat.setTimeZone(custom.getTimeZoneInstance());
        return String.format(
                "    Acquired: %s (%s)", trackFormat.format(log.getAcquired()), custom.getFormattedTime()
        );
    }
}
