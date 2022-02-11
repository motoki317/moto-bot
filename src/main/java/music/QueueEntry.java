package music;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public record QueueEntry(AudioTrack track, long userId) {
}
