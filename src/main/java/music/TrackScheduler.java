package music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
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
import java.time.Instant;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static music.MusicUtils.formatLength;
import static music.MusicUtils.getThumbnailURL;

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

    TrackScheduler(SchedulerGateway gateway) {
        this.queue = new ArrayDeque<>();
        this.gateway = gateway;
    }

    /**
     * Shuffles the queue. Retains the position of the current playing track (first element in the queue).
     */
    private void shuffleQueue() {
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

    void clearQueue() {
        this.queue.clear();
    }

    Deque<QueueEntry> getCurrentQueue() {
        return new ArrayDeque<>(this.queue);
    }

    long getRemainingLength() {
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
        RepeatState repeat = setting.getRepeat();
        AudioTrackInfo info = track.getInfo();
        String title = info.title;
        User user = queue.peek() != null ? this.gateway.getUser(queue.peek().getUserId()) : null;

        Message message = new MessageBuilder(
                new EmbedBuilder()
                        .setAuthor(String.format("♪ Now playing%s", repeat == RepeatState.OFF ? "" : " (" + repeat.getMessage() + ")"),
                                null, this.gateway.getBotAvatarURL())
                        .setTitle("".equals(title) ? "(No title)" : title, track.getInfo().uri)
                        .addField("Length", info.isStream ? "LIVE" : formatLength(info.length), true)
                        .addField("Player volume", setting.getVolume() + "%", true)
                        .setThumbnail(getThumbnailURL(info.uri))
                        .setFooter(String.format("Requested by %s",
                                user != null ? user.getName() + "#" + user.getDiscriminator() : "Unknown User"),
                                user != null ? user.getEffectiveAvatarUrl() : null)
                        .setTimestamp(Instant.now())
                        .build()
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
        playNextTrack(track, endReason != AudioTrackEndReason.FINISHED);
    }

    private void playNextTrack(AudioTrack finishedTrack, boolean isManualSkip) {
        QueueEntry track = this.queue.poll();
        if (track == null) {
            // indicates the queue has been cleared by other means
            return;
        }

        // Poll the queue, re-queue the song depending on repeat state,
        QueueEntry next = getQueueEntry(finishedTrack, isManualSkip, track);
        if (next == null) {
            this.sendEmptyQueueMessage();
            return;
        }
        playTrack(next.getTrack());
    }

    @Nullable
    private QueueEntry getQueueEntry(AudioTrack finishedTrack, boolean isManualSkip, QueueEntry track) {
        QueueEntry next = null;
        switch (this.gateway.getSetting().getRepeat()) {
            case OFF:
                next = queue.peek();
                break;
            case ONE:
                // If this was a manual skip (or an unexpected finish), forcefully skip to the next track
                if (isManualSkip) {
                    next = queue.peek();
                } else {
                    // Re-add the cloned track
                    QueueEntry newEntry = new QueueEntry(finishedTrack.makeClone(), track.getUserId());
                    this.queue.addFirst(newEntry);
                    next = newEntry;
                }
                break;
            case QUEUE:
                // Re-add the cloned track
                QueueEntry newEntry = new QueueEntry(finishedTrack.makeClone(), track.getUserId());
                this.queue.add(newEntry);

                next = queue.peek();
                break;
            case RANDOM:
                next = queue.peek();
                shuffleQueue();
                break;
            case RANDOM_REPEAT:
                // Re-add the cloned track
                newEntry = new QueueEntry(finishedTrack.makeClone(), track.getUserId());
                this.queue.add(newEntry);

                next = newEntry;
                shuffleQueue();
                break;
        }
        return next;
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
