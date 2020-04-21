package music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

/**
 * Queue entry record
 */
class QueueEntry {
    private final AudioTrack track;
    private final long userId;

    QueueEntry(AudioTrack track, long userId) {
        this.track = track;
        this.userId = userId;
    }

    AudioTrack getTrack() {
        return track;
    }

    long getUserId() {
        return userId;
    }
}
