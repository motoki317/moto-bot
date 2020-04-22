package music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

/**
 * Queue entry record
 */
public class QueueEntry {
    private final AudioTrack track;
    private final long userId;

    public QueueEntry(AudioTrack track, long userId) {
        this.track = track;
        this.userId = userId;
    }

    public AudioTrack getTrack() {
        return track;
    }

    public long getUserId() {
        return userId;
    }
}
