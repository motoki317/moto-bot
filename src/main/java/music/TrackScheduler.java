package music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import db.model.musicSetting.MusicSetting;
import music.exception.DuplicateTrackException;
import music.exception.QueueFullException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static music.MusicUtils.formatNowPlaying;

/**
 * Handles track queue.
 */
public class TrackScheduler extends AudioEventAdapter {
    private static final int QUEUE_LIMIT = 300;

    public interface SchedulerGateway {
        /**
         * Sends message to the bound music channel.
         * @param message Message.
         */
        void sendMessage(Message message);

        /**
         * Retrieves the current setting for the guild.
         * @return Music setting.
         */
        @NotNull
        MusicSetting getSetting();

        /**
         * Retrieves bot avatar URL.
         * @return URL.
         */
        String getBotAvatarURL();

        /**
         * Retrieves user.
         * @param userId User ID.
         * @return User.
         */
        @Nullable
        User getUser(long userId);

        /**
         * Sets the last interact time to the current time.
         */
        void setLastInteract();

        /**
         * Plays the specified track.
         * @param track Audio track
         */
        void playTrack(AudioTrack track);
    }

    private final Deque<QueueEntry> queue;

    private final SchedulerGateway gateway;

    public TrackScheduler(SchedulerGateway gateway) {
        this.queue = new ArrayDeque<>();
        this.gateway = gateway;
    }

    /**
     * Shuffles the queue. Retains the position of the current playing track (first element in the queue).
     */
    void shuffleQueue() {
        QueueEntry first = this.queue.poll();
        List<QueueEntry> others = new ArrayList<>(this.queue);
        Collections.shuffle(others);
        this.queue.clear();
        this.queue.add(first);
        this.queue.addAll(others);
    }

    private static boolean hasDuplicate(Deque<QueueEntry> queue, QueueEntry toQueue) {
        Set<String> URLs = queue.stream().map(q -> q.getTrack().getInfo().uri).collect(Collectors.toSet());
        return URLs.contains(toQueue.getTrack().getInfo().uri);
    }

    void enqueue(QueueEntry entry) throws DuplicateTrackException, QueueFullException {
        if (hasDuplicate(this.queue, entry)) {
            throw new DuplicateTrackException("The current queue contains a duplicate track!");
        }
        if (QUEUE_LIMIT <= this.queue.size()) {
            throw new QueueFullException(String.format(
                    "Queue full (size: %s)! Please empty some slots in the queue before you enqueue new songs.",
                    this.queue.size()));
        }

        boolean toStartPlaying = this.queue.isEmpty();
        this.queue.add(entry);
        if (toStartPlaying) {
            this.playTrack(entry.getTrack());
        }
    }

    /**
     * Purges all waiting songs in the queue.
     * Does NOT remove the current song.
     */
    void purgeWaitingQueue() {
        if (this.queue.isEmpty() || this.queue.size() == 1) {
            return;
        }

        QueueEntry first = this.queue.poll();
        this.queue.clear();
        this.queue.add(first);
    }

    void clearQueue() {
        this.queue.clear();
    }

    List<QueueEntry> getCurrentQueue() {
        return new ArrayList<>(this.queue);
    }

    long getQueueLength() {
        return this.queue.stream().mapToLong(q -> q.getTrack().getDuration()).sum();
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        this.gateway.sendMessage(
                new MessageBuilder(
                        "Player paused."
                ).build()
        );
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        this.gateway.sendMessage(
                new MessageBuilder(
                        "Player resumed."
                ).build()
        );
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        if (!this.gateway.getSetting().isShowNp()) {
            return;
        }

        MusicSetting setting = this.gateway.getSetting();
        User user = queue.peek() != null ? this.gateway.getUser(queue.peek().getUserId()) : null;

        boolean showPosition = track.getPosition() > TimeUnit.SECONDS.toMillis(1);

        Message message = new MessageBuilder(
                formatNowPlaying(track, user, setting, this.gateway.getBotAvatarURL(), showPosition)
        ).build();

        this.gateway.sendMessage(message);
    }

    private void sendEmptyQueueMessage() {
        this.gateway.sendMessage(
                new MessageBuilder(
                        new EmbedBuilder()
                                .setColor(Color.RED)
                                .setDescription("Queue ended, please enqueue some new music, or the bot will leave automatically after some time.")
                                .build()
                ).build()
        );
        this.gateway.setLastInteract();
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (!endReason.mayStartNext) {
            return;
        }

        // Get the finished track
        QueueEntry entry = this.queue.poll();
        if (entry == null) {
            // indicates the queue has been cleared by other means
            return;
        }

        // Get the next track depending on the repeat state
        QueueEntry next = getQueueEntry(track, false, entry);
        if (next == null) {
            this.sendEmptyQueueMessage();
            return;
        }
        playTrack(next.getTrack());
    }

    /**
     * Skips specified amount of tracks.
     * @param amount Amount.
     */
    void skip(int amount) {
        QueueEntry next = null;
        for (int i = 0; i < amount; i++) {
            QueueEntry prev = this.queue.poll();
            if (prev == null) {
                this.sendEmptyQueueMessage();
                return;
            }
            next = getQueueEntry(prev.getTrack(), true, prev);
        }
        if (next == null) {
            this.sendEmptyQueueMessage();
            return;
        }
        playTrack(next.getTrack());
    }

    /**
     * Retrieves the next track depending on the repeat state.
     * If repeating, re-enqueues (and shuffles) inside this.
     * @param finishedTrack Finished track.
     * @param isManualSkip {@code true} if manual skip.
     * @param track Finished track.
     * @return Next track.
     */
    @Nullable
    private QueueEntry getQueueEntry(AudioTrack finishedTrack, boolean isManualSkip, QueueEntry track) {
        switch (this.gateway.getSetting().getRepeat()) {
            case OFF:
                break;
            case ONE:
                // If this was a manual skip (or an unexpected finish), forcefully skip to the next track
                if (!isManualSkip) {
                    // Re-add the cloned track
                    QueueEntry newEntry = new QueueEntry(finishedTrack.makeClone(), track.getUserId());
                    this.queue.addFirst(newEntry);
                }
                break;
            case QUEUE:
                // Re-add the cloned track
                QueueEntry newEntry = new QueueEntry(finishedTrack.makeClone(), track.getUserId());
                this.queue.add(newEntry);
                break;
            case RANDOM:
                shuffleQueue();
                break;
            case RANDOM_REPEAT:
                // Re-add the cloned track
                newEntry = new QueueEntry(finishedTrack.makeClone(), track.getUserId());
                this.queue.add(newEntry);
                shuffleQueue();
                break;
        }
        return queue.peek();
    }

    private void playTrack(@NotNull AudioTrack track) {
        this.gateway.playTrack(track);
    }

    @Override
    public void onTrackException(AudioPlayer player, AudioTrack track, FriendlyException exception) {
        String title = track.getInfo().title;
        this.gateway.sendMessage(
                new MessageBuilder(
                        new EmbedBuilder()
                                .setColor(Color.RED)
                                .setDescription(String.format(
                                        "Error: Player cannot play this track: %s\nTrack name: %s",
                                        exception.getMessage(),
                                        "".equals(title) ? "(No title)" : title))
                                .build()
                ).build()
        );
    }

    @Override
    public void onTrackStuck(AudioPlayer player, AudioTrack track, long thresholdMs) {
        this.gateway.sendMessage(
                new MessageBuilder(
                        new EmbedBuilder()
                                .setColor(Color.RED)
                                .setDescription("Error: Player timed out.")
                                .build()
                ).build()
        );
    }
}
