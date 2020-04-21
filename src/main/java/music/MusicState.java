package music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import music.exception.DuplicateTrackException;
import music.exception.QueueFullException;

import java.util.Deque;

class MusicState {
    private AudioPlayer player;
    private TrackScheduler scheduler;
    private long lastInteract;

    MusicState(AudioPlayer player, TrackScheduler scheduler, long lastInteract) {
        this.player = player;
        this.scheduler = scheduler;
        this.lastInteract = lastInteract;
    }

    void setLastInteract(long lastInteract) {
        this.lastInteract = lastInteract;
    }

    void enqueue(QueueEntry entry) throws DuplicateTrackException, QueueFullException {
        this.scheduler.enqueue(entry);
    }

    AudioPlayer getPlayer() {
        return this.player;
    }

    void stopPlaying() {
        this.scheduler.clearQueue();
        this.player.stopTrack();
    }

    QueueState getCurrentQueue() {
        Deque<QueueEntry> queue = this.scheduler.getCurrentQueue();
        AudioTrack track = this.player.getPlayingTrack();
        long position = track != null ? track.getPosition() : 0L;
        return new QueueState(queue, position);
    }

    /**
     * Retrieves remaining queue length in milliseconds.
     * @return Queue length.
     */
    long getRemainingLength() {
        long queueLength = this.scheduler.getRemainingLength();
        AudioTrack track = this.player.getPlayingTrack();
        if (track != null) {
            queueLength -= track.getPosition();
        }
        return queueLength;
    }
}
