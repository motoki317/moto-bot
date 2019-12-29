package heartbeat.tracking;

import api.wynn.WynnApi;
import api.wynn.structs.OnlinePlayers;
import api.wynn.structs.Player;
import app.Bot;
import db.model.track.TrackChannel;
import db.model.track.TrackType;
import db.model.warLog.WarLog;
import db.model.warPlayer.WarPlayer;
import db.model.warTrack.WarTrack;
import db.model.world.World;
import db.repository.*;
import log.Logger;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import utils.FormatUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PlayerTracker {
    private static final Pattern mainWorld = Pattern.compile("(WC|EU)\\d+");
    private static final Pattern warWorld = Pattern.compile("WAR\\d+");
    private static final DateFormat trackFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    private final Logger logger;
    private final Object dbLock;
    private final ShardManager manager;
    private final WynnApi api;

    private final WorldRepository worldRepository;
    private final TrackChannelRepository trackChannelRepository;

    private final WarLogRepository warLogRepository;
    private final WarTrackRepository warTrackRepository;

    public PlayerTracker(Bot bot, Object dbLock) {
        this.logger = bot.getLogger();
        this.dbLock = dbLock;
        this.manager = bot.getManager();
        this.api = new WynnApi(this.logger, bot.getProperties().wynnTimeZone);
        this.worldRepository = bot.getDatabase().getWorldRepository();
        this.trackChannelRepository = bot.getDatabase().getTrackingChannelRepository();
        this.warLogRepository = bot.getDatabase().getWarLogRepository();
        this.warTrackRepository = bot.getDatabase().getWarTrackRepository();
    }

    public void run() {
        OnlinePlayers players = this.api.getOnlinePlayers();
        if (players == null) {
            this.logger.log(0, "Player Tracker: Failed to retrieve online players list");
            return;
        }

        List<World> prevWorldList;
        Map<String, World> currentWorlds = players.getWorlds().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new World(e.getKey(), e.getValue().size())));
        synchronized (this.dbLock) {
            prevWorldList = this.worldRepository.findAll();
            if (prevWorldList == null) return;

            // Update DB
            if (!this.worldRepository.updateAll(currentWorlds.values())) {
                this.logger.log(0, "Player Tracker: Failed to update worlds in DB");
            }
        }

        Map<String, World> prevWorlds = prevWorldList.stream().collect(Collectors.toMap(World::getName, w -> w));

        this.manager.setActivity(Activity.playing("Wynn " + countOnlinePlayers(currentWorlds.values()) + " online"));

        this.handleServerTracking(currentWorlds, prevWorlds);
        synchronized (this.dbLock) {
            this.handleWarTracking(players);
        }
    }

    /**
     * Do server tracking.
     * @param currentWorlds Current worlds retrieved from Wynn API.
     * @param prevWorlds Previous worlds (which was stored in the db).
     */
    private void handleServerTracking(Map<String, World> currentWorlds, Map<String, World> prevWorlds) {
        // Handle server start / close tracking.
        for (World currentWorld : currentWorlds.values()) {
            if (!prevWorlds.containsKey(currentWorld.getName())) {
                sendServerTracking(true, this.trackChannelRepository, this.manager, this.logger, currentWorld);
            }
        }
        for (World prevWorld : prevWorlds.values()) {
            if (!currentWorlds.containsKey(prevWorld.getName())) {
                sendServerTracking(false, this.trackChannelRepository, this.manager, this.logger, prevWorld);
            }
        }
    }

    /**
     * Do war tracking.
     * @param players Online players retrieved from Wynn API.
     */
    private void handleWarTracking(OnlinePlayers players) {
        // Handle war tracking.
        List<WarLog> knownWarLogs = this.warLogRepository.findAllLogNotEnded();
        if (knownWarLogs == null) {
            this.logger.log(0, "Player tracker: failed to retrieve previous war logs from db. " +
                    "Skipping updating wars.");
            return;
        }

        Map<String, List<String>> currentWars = players.getWorlds().entrySet().stream().filter(e -> warWorld.matcher(e.getKey()).matches())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        Map<String, WarLog> prevWars = knownWarLogs.stream().collect(Collectors.toMap(WarLog::getServerName, w -> w));

        Date now = new Date(players.getRequest().getTimestamp() * 1000);
        for (Map.Entry<String, List<String>> entry : currentWars.entrySet()) {
            if (!prevWars.containsKey(entry.getKey())) {
                // New war server
                if (entry.getValue().isEmpty()) {
                    continue;
                }
                // proceed only if players are in; else, next time
                startWarTrack(entry.getKey(), entry.getValue(), now);
            } else {
                // Update war server

                List<String> currentPlayers = entry.getValue();
                WarLog warLog = prevWars.get(entry.getKey());
                if (currentPlayers.isEmpty()) {
                    // if no players are in, assume the war has ended
                    endWarTrack(warLog, now);
                } else {
                    // else, update the war server
                    updateWarTrack(warLog, currentPlayers, now);
                }
            }
        }
        for (Map.Entry<String, WarLog> entry : prevWars.entrySet()) {
            if (!currentWars.containsKey(entry.getKey())) {
                // War server closed
                endWarTrack(entry.getValue(), now);
            }
        }
    }

    private void startWarTrack(String serverName, List<String> players, Date now) {
        // TODO: get player uuid
        List<WarPlayer> warPlayers = players.stream()
                .map(p -> new WarPlayer(p, null, false)).collect(Collectors.toList());
        // retrieve guild name
        String guildName = null;
        for (WarPlayer warPlayer : warPlayers) {
            Player stats = this.api.getPlayerStatistics(warPlayer.getPlayerName(), false, false);
            if (stats == null) {
                continue;
            }
            if (guildName == null && stats.getGuildInfo().getName() != null) {
                guildName = stats.getGuildInfo().getName();
            }
            warPlayer.setPlayerUUID(stats.getUuid());
        }

        WarLog warLog = new WarLog(serverName, guildName, now, now, false, false, warPlayers);

        int id = this.warLogRepository.createAndGetLastInsertId(warLog);
        if (id != 0) {
            return;
        }
        warLog.setId(id);
        sendWarTracking(warLog);
    }

    private void updateWarTrack(WarLog prevWarLog, List<String> currentPlayers, Date now) {
        prevWarLog.setLastUp(now);
        List<WarPlayer> warPlayers = prevWarLog.getPlayers();

        for (WarPlayer warPlayer : warPlayers) {
            // A player left
            if (!currentPlayers.contains(warPlayer.getPlayerName())) {
                warPlayer.setExited(true);
            }
        }
        Set<String> prevPlayers = warPlayers.stream().map(WarPlayer::getPlayerName).collect(Collectors.toSet());
        for (String currentPlayer : currentPlayers) {
            // A player joined
            if (!prevPlayers.contains(currentPlayer)) {
                // TODO: get player uuid
                WarPlayer warPlayer = new WarPlayer(prevWarLog.getId(), currentPlayer, null, false);
                warPlayers.add(warPlayer);

                // get player uuid, and if guild name is null try to retrieve it as wel
                Player stats = this.api.getPlayerStatistics(currentPlayer, false, false);
                if (stats == null) {
                    continue;
                }
                if (prevWarLog.getGuildName() == null && stats.getGuildInfo().getName() != null) {
                    prevWarLog.setGuildName(stats.getGuildInfo().getName());
                }
                warPlayer.setPlayerUUID(stats.getUuid());
            }
        }

        boolean res = this.warLogRepository.update(prevWarLog);
        if (!res) {
            return;
        }
        sendWarTracking(prevWarLog);
    }

    private void endWarTrack(WarLog warLog, Date now) {
        warLog.setLastUp(now);
        warLog.setEnded(true);
        warLog.setLogEnded(true);
        boolean res = this.warLogRepository.update(warLog);
        if (!res) {
            return;
        }
        sendWarTracking(warLog);
        // Remove ended war tracking records from db
        removeEndedWarTrack(warLog);
    }

    private void removeEndedWarTrack(WarLog warLog) {
        this.warTrackRepository.deleteAllOfWarLogId(warLog.getId());
    }

    /**
     * Sends war tracking.
     * @param warLog War log instance containing valid war log id.
     */
    private void sendWarTracking(WarLog warLog) {
        // Retrieve track channels from db
        List<TrackChannel> allTrack = this.trackChannelRepository.findAllOfType(TrackType.WAR_ALL);
        if (allTrack == null) {
            return;
        }
        Set<TrackChannel> channelsToSend = new HashSet<>(allTrack);

        List<TrackChannel> specificTrack = this.trackChannelRepository.findAllOfType(TrackType.WAR_SPECIFIC);
        if (specificTrack == null) {
            return;
        }
        for (TrackChannel t : specificTrack) {
            if (t.getGuildName() != null && t.getGuildName().equals(warLog.getGuildName())) {
                channelsToSend.add(t);
            }
        }

        List<TrackChannel> playerTrack = this.trackChannelRepository.findAllOfType(TrackType.WAR_PLAYER);
        if (playerTrack == null) {
            return;
        }
        Set<String> players = warLog.getPlayers().stream().map(WarPlayer::getPlayerName).collect(Collectors.toSet());
        for (TrackChannel t : playerTrack) {
            if (t.getPlayerName() != null && players.contains(t.getPlayerName())) {
                channelsToSend.add(t);
            }
        }

        // List of channels the bot already send track messages
        List<WarTrack> currentTracks = this.warTrackRepository.findAllOfWarLogId(warLog.getId());
        if (currentTracks == null) {
            return;
        }
        Map<Long, WarTrack> channelsAlreadySent = currentTracks.stream().collect(Collectors.toMap(WarTrack::getChannelId, t -> t));

        // Send messages
        String message = formatWarTrack(warLog);
        for (TrackChannel t : channelsToSend) {
            if (channelsAlreadySent.containsKey(t.getChannelId())) {
                WarTrack track = channelsAlreadySent.get(t.getChannelId());
                // update the message
                TextChannel channel = this.manager.getTextChannelById(track.getChannelId());
                if (channel == null) {
                    this.logger.log(0, "Player tracker: failed to retrieve text channel: " + track.getChannelId());
                    continue;
                }
                channel.editMessageById(track.getMessageId(), message).queue();
            } else {
                // send new message and insert into db
                TextChannel channel = this.manager.getTextChannelById(t.getChannelId());
                if (channel == null) {
                    this.logger.log(0, "Player tracker: failed to retrieve text channel: " + t.getChannelId());
                    continue;
                }
                channel.sendMessage(message).queue(success -> {
                    WarTrack warTrack = new WarTrack(warLog.getId(), t.getChannelId(), success.getIdLong());
                    if (!this.warTrackRepository.create(warTrack)) {
                        this.logger.log(0, "Player tracker: failed to create a new track record in db");
                    }
                });
            }
        }
    }

    private static String formatWarTrack(WarLog warLog) {
        @NotNull
        String guildName = warLog.getGuildName() != null ? warLog.getGuildName() : "(Unknown Guild)";
        String formattedPlayerNames = warLog.getPlayers().stream()
                .map(p -> p.hasExited()
                        ? "~~" + p.getPlayerName().replace("_", "\\_") + "~~"
                        : p.getPlayerName().replace("_", "\\_"))
                .collect(Collectors.joining(", "));

        String formattedTime = trackFormat.format(warLog.getCreatedAt());
        if (warLog.getCreatedAt().equals(warLog.getLastUp())) {
            // war just started
            formattedTime += " ~ (Just started)";
        } else if (!warLog.isEnded()) {
            formattedTime += " ~ " + trackFormat.format(warLog.getLastUp()) + " (in fight)";
        } else {
            formattedTime += " ~ " + trackFormat.format(warLog.getLastUp());
        }

        return String.format(
                "**%s** *%s* → %s\n" +
                        "    Time: %s",
                warLog.getServerName(), guildName, formattedPlayerNames,
                formattedTime
        );
    }

    /**
     * Called when a server starts or closes.
     * @param start Boolean indicating server started or closed.
     * @param repo Track channel repository.
     * @param manager Bot manager to send messages.
     * @param logger Bot logger.
     * @param world Corresponding world.
     */
    private static void sendServerTracking(boolean start,
                                           @NotNull TrackChannelRepository repo,
                                           ShardManager manager,
                                           Logger logger,
                                           @NotNull World world) {
        Date now = new Date();
        String message;
        if (start) {
            message = String.format("Server `%s` has started.", world.getName());
        } else {
            long upSeconds = (now.getTime() - world.getCreatedAt().getTime()) / 1000L;
            String formattedUptime = FormatUtils.formatReadableTime(upSeconds, false, "s");
            message = String.format("Server `%s` has closed. Uptime: `%s`", world.getName(), formattedUptime);
        }

        logger.debug(message);

        // Ignore war worlds
        if (warWorld.matcher(world.getName()).matches()) {
            return;
        }

        List<TrackChannel> allServers = repo.findAllOfType(start ? TrackType.SERVER_START_ALL : TrackType.SERVER_CLOSE_ALL);
        if (allServers == null) return;
        Set<TrackChannel> channelsToSend = new HashSet<>(allServers);

        // Is a main world
        if (mainWorld.matcher(world.getName()).matches()) {
            List<TrackChannel> toAdd = repo.findAllOfType(start ? TrackType.SERVER_START : TrackType.SERVER_CLOSE);
            if (toAdd == null) return;
            channelsToSend.addAll(toAdd);
        }

        channelsToSend.forEach(ch -> {
            TextChannel channelToSend = manager.getTextChannelById(ch.getChannelId());
            if (channelToSend == null) return;
            channelToSend.sendMessage(
                    trackFormat.format(now) + " " + message
            ).queue();
        });
    }

    private static int countOnlinePlayers(@NotNull Collection<World> currentWorlds) {
        return currentWorlds.stream().reduce(0, (i, w) -> i + w.getPlayers(), Integer::sum);
    }
}
