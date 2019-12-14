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

    private final Bot bot;
    private final Logger logger;

    // cache
    private WynnApi api;

    public PlayerTracker(Bot bot) {
        this.bot = bot;
        this.logger = bot.getLogger();
    }

    public void run() {
        if (this.api == null) {
            this.api = new WynnApi(this.logger, this.bot.getProperties().wynnTimeZone);
        }

        OnlinePlayers players = this.api.getOnlinePlayers();
        if (players == null) {
            this.logger.log(0, "Player Tracker: Failed to retrieve online players list");
            return;
        }

        WorldRepository repo = this.bot.getDatabase().getWorldRepository();
        List<World> prevWorldList = repo.findAll();
        if (prevWorldList == null) return;

        Map<String, World> prevWorlds = prevWorldList.stream().collect(Collectors.toMap(World::getName, w -> w));
        Map<String, World> currentWorlds = players.getWorlds().entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> new World(e.getKey(), e.getValue().size())));

        // Handle tracks
        TrackChannelRepository trackChannelRepo = this.bot.getDatabase().getTrackingChannelRepository();
        for (World currentWorld : currentWorlds.values()) {
            if (!prevWorlds.containsKey(currentWorld.getName())) {
                handleServerTracking(true, trackChannelRepo, this.bot.getManager(), this.logger, currentWorld);
            }
        }
        for (World prevWorld : prevWorlds.values()) {
            if (!currentWorlds.containsKey(prevWorld.getName())) {
                handleServerTracking(false, trackChannelRepo, this.bot.getManager(), this.logger, prevWorld);
            }
        }

        this.bot.getManager().setActivity(Activity.playing("Wynn " + countOnlinePlayers(currentWorlds.values()) + " online"));

        // Update DB
        if (!repo.updateAll(currentWorlds.values())) {
            this.logger.log(0, "Player Tracer: Failed to update worlds in DB");
        }
    }

    /**
     * Handles server tracking and sends messages to channels.
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
            String formattedUpTime = FormatUtils.getReadableDHMSFormat(upSeconds, false);
            message = String.format("Server `%s` has closed. Uptime: `%s`", world.getName(), formattedUpTime);
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
