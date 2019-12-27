package heartbeat.tracking;

import api.WynnApi;
import api.structs.OnlinePlayers;
import app.Bot;
import db.model.track.TrackChannel;
import db.model.track.TrackType;
import db.model.world.World;
import db.repository.TrackChannelRepository;
import db.repository.WorldRepository;
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

    public PlayerTracker(Bot bot, Object dbLock) {
        this.logger = bot.getLogger();
        this.dbLock = dbLock;
        this.manager = bot.getManager();
        this.api = new WynnApi(this.logger, bot.getProperties().wynnTimeZone);
        this.worldRepository = bot.getDatabase().getWorldRepository();
        this.trackChannelRepository = bot.getDatabase().getTrackingChannelRepository();
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
        this.handleTracking(currentWorlds, prevWorlds);
    }

    /**
     * Do war & server tracking.
     * @param currentWorlds Current worlds retrieved from Wynn API.
     * @param prevWorlds Previous worlds (stored in the db).
     */
    private void handleTracking(Map<String, World> currentWorlds, Map<String, World> prevWorlds) {
        // Handle tracks
        // TODO: war trackers
        for (World currentWorld : currentWorlds.values()) {
            if (!prevWorlds.containsKey(currentWorld.getName())) {
                handleServerTracking(true, this.trackChannelRepository, this.manager, this.logger, currentWorld);
            }
        }
        for (World prevWorld : prevWorlds.values()) {
            if (!currentWorlds.containsKey(prevWorld.getName())) {
                handleServerTracking(false, this.trackChannelRepository, this.manager, this.logger, prevWorld);
            }
        }

        this.manager.setActivity(Activity.playing("Wynn " + countOnlinePlayers(currentWorlds.values()) + " online"));
    }

    /**
     * Called when a server starts or closes.
     * @param start Boolean indicating server started or closed.
     * @param repo Track channel repository.
     * @param manager Bot manager to send messages.
     * @param logger Bot logger.
     * @param world Corresponding world.
     */
    private static void handleServerTracking(boolean start,
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

        logger.log(-1, message);

        // Ignore war worlds
        if (warWorld.matcher(world.getName()).matches()) {
            return;
        }

        List<TrackChannel> channelsToSend = repo.findAllOfType(start ? TrackType.SERVER_START_ALL : TrackType.SERVER_CLOSE_ALL);
        if (channelsToSend == null) return;

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
