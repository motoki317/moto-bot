package heartbeat.tracking;

import api.WynnApi;
import api.structs.OnlinePlayers;
import app.Bot;
import db.model.track.TrackChannel;
import db.model.track.TrackType;
import db.model.world.World;
import db.model.world.WorldId;
import db.repository.TrackChannelRepository;
import db.repository.WorldRepository;
import log.Logger;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import utils.FormatUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class PlayerTracker {
    private static final Pattern mainWorld = Pattern.compile("(WC|EU)\\d+");
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
        Map<String, World> prevWorlds = new HashMap<>();
        repo.findAll().forEach(w -> prevWorlds.put(w.getName(), w));

        // Handle tracks
        for (Map.Entry<String, List<String>> world : players.getWorlds().entrySet()) {
            String name = world.getKey();
            if (!prevWorlds.containsKey(name)) {
                handleServerStartTracking(world);
            }
        }
        for (Map.Entry<String, World> prevWorld : prevWorlds.entrySet()) {
            String prevWorldName = prevWorld.getKey();
            if (!players.getWorlds().containsKey(prevWorldName)) {
                handleServerCloseTracking(prevWorld.getValue());
            }
        }

        // Update DB
        for (Map.Entry<String, List<String>> world : players.getWorlds().entrySet()) {
            String name = world.getKey();
            if (!prevWorlds.containsKey(name)) {
                World newWorld = new World(name, world.getValue().size());
                repo.create(newWorld);
            } else {
                World existingWorld = new World(name, world.getValue().size());
                repo.update(existingWorld);
            }
        }
        for (Map.Entry<String, World> prevWorld : prevWorlds.entrySet()) {
            String prevWorldName = prevWorld.getKey();
            if (!players.getWorlds().containsKey(prevWorldName)) {
                WorldId closedWorld = () -> prevWorldName;
                repo.delete(closedWorld);
            }
        }
    }

    private void handleServerStartTracking(Map.Entry<String, List<String>> world) {
        TrackChannelRepository repo = this.bot.getDatabase().getTrackingChannelRepository();
        List<TrackChannel> channels = repo.findAllOfType(TrackType.SERVER_START_ALL);
        if (mainWorld.matcher(world.getKey()).matches()) {
            // Is a main world
            channels.addAll(repo.findAllOfType(TrackType.SERVER_START));
        }

        Date now = new Date();
        String message = String.format("Server `%s` has started.", world.getKey());

        ShardManager manager = this.bot.getManager();
        channels.forEach(ch -> {
            TextChannel channel = manager.getTextChannelById(ch.getChannelId());
            if (channel == null) return;
            channel.sendMessage(
                    trackFormat.format(now) + " " + message
            ).queue();
        });
    }

    private void handleServerCloseTracking(World world) {
        TrackChannelRepository repo = this.bot.getDatabase().getTrackingChannelRepository();
        List<TrackChannel> channels = repo.findAllOfType(TrackType.SERVER_CLOSE_ALL);
        if (mainWorld.matcher(world.getName()).matches()) {
            // Is a main world
            channels.addAll(repo.findAllOfType(TrackType.SERVER_CLOSE));
        }

        Date now = new Date();
        long upSeconds = (now.getTime() - world.getCreatedAt().getTime()) / 1000L;
        String formattedUpTime = FormatUtils.getReadableDHMSFormat(upSeconds, false);
        String message = String.format("Server `%s` has closed. Uptime: `%s`", world.getName(), formattedUpTime);

        ShardManager manager = this.bot.getManager();
        channels.forEach(ch -> {
            TextChannel channel = manager.getTextChannelById(ch.getChannelId());
            if (channel == null) return;
            channel.sendMessage(
                    trackFormat.format(now) + " " + message
            ).queue();
        });
    }
}
