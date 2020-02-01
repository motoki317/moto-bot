package heartbeat.tasks;

import app.Bot;
import db.model.track.TrackChannel;
import db.repository.base.TrackChannelRepository;
import heartbeat.base.TaskBase;
import log.Logger;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Tracking manager deletes expired tracking, and sends messages to the channel.
 */
public class TrackingManager implements TaskBase {
    private final ShardManager shardManager;
    private final Logger logger;
    private final TrackChannelRepository trackChannelRepository;

    public TrackingManager(Bot bot) {
        this.shardManager = bot.getManager();
        this.logger = bot.getLogger();
        this.trackChannelRepository = bot.getDatabase().getTrackingChannelRepository();
    }

    @Override
    public void run() {
        long now = System.currentTimeMillis();

        List<TrackChannel> tracks = this.trackChannelRepository.findAll();
        if (tracks == null) {
            this.logger.log(0, "Something went wrong while retrieving all track data...");
            return;
        }

        for (TrackChannel track : tracks) {
            if (track.getExpiresAt().getTime() <= now) {
                // track expired, delete the track and send message
                if (this.trackChannelRepository.delete(track)) {
                    TextChannel channel = this.shardManager.getTextChannelById(track.getChannelId());
                    if (channel == null) {
                        continue;
                    }
                    channel.sendMessage(
                            ":exclamation: Disabled the following tracking in this channel because it has expired. " +
                            "Use the same command to enable the tracking again, and use `track <update|refresh>` command " +
                            "to refresh them before they expire.\n" +
                            String.format("**%s**", track.getDisplayName())
                    ).queue();

                    this.logger.log(0, ":mute: A tracking has expired:\n" + track.toString());
                } else {
                    this.logger.log(0, "Something went wrong while trying to remove expired track: " + track.toString());
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
        return TimeUnit.HOURS.toMillis(1);
    }
}
