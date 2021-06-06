package heartbeat.tasks;

import app.Bot;
import db.model.track.TrackChannel;
import db.repository.base.TrackChannelRepository;
import heartbeat.base.TaskBase;
import log.Logger;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tracking manager deletes expired tracking, and sends messages to the channel.
 */
public class TrackingManager implements TaskBase {
    private final Bot bot;
    private final ShardManager shardManager;
    private final Logger logger;
    private final TrackChannelRepository trackChannelRepository;

    public TrackingManager(Bot bot) {
        this.bot = bot;
        this.shardManager = bot.getManager();
        this.logger = bot.getLogger();
        this.trackChannelRepository = bot.getDatabase().getTrackingChannelRepository();
    }

    @Override
    public @NotNull String getName() {
        return "Tracking Manager";
    }

    private void delete(TrackChannel track) {
        if (!this.trackChannelRepository.delete(track)) {
            this.logger.log(0, "Something went wrong while trying to remove expired track:\n" + track.toString());
        }
    }

    @Override
    public void run() {
        // Do not process if JDA is disconnected from WS
        if (!this.bot.isAllConnected()) return;

        long now = System.currentTimeMillis();

        List<TrackChannel> tracks = this.trackChannelRepository.findAll();
        if (tracks == null) {
            this.logger.log(0, "Something went wrong while retrieving all track data...");
            return;
        }

        for (TrackChannel track : tracks) {
            // Check if the channel still exists and the bot can send messages
            TextChannel channel = this.shardManager.getTextChannelById(track.getChannelId());
            if (channel == null) {
                this.logger.log(0,
                        "Tracking Manager: Failed to get text channel for a track entry, removing.\n" + track.toString());

                this.delete(track);
                continue;
            }
            if (!channel.canTalk()) {
                this.logger.log(0,
                        "Tracking Manager: Cannot talk in a track entry, removing.\n" + track.toString());

                this.delete(track);
                continue;
            }

            // Check if the track expired, if so delete the track and send message
            if (track.getExpiresAt().getTime() <= now) {
                if (this.trackChannelRepository.delete(track)) {
                    channel.sendMessage(
                            ":exclamation: Disabled the following tracking in this channel because it has expired. " +
                            "Use the same command to enable the tracking again, and use `track <update|refresh>` command " +
                            "to refresh them before they expire.\n" +
                            String.format("**%s**", track.getDisplayName())
                    ).queue();

                    this.logger.log(0, ":mute: A tracking has expired:\n" + track.toString());
                } else {
                    this.logger.log(0,
                            "Something went wrong while trying to remove expired track:\n" + track.toString());
                }
            }
        }
    }

    @Override
    public long getFirstDelay() {
        return TimeUnit.MINUTES.toMillis(1);
    }

    @Override
    public long getInterval() {
        return TimeUnit.MINUTES.toMillis(15);
    }
}
