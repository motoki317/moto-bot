package music;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import db.model.musicSetting.MusicSetting;
import music.exception.DuplicateTrackException;
import music.exception.QueueFullException;

import java.util.List;

public class MusicState {
    private final AudioPlayer player;
    private final TrackScheduler scheduler;
    private final MusicSetting setting;
    // TODO: automatically leave vc after some inactivity
    private long lastInteract;
    private Runnable onStopLoadingCache;
    private final long boundChannelId;
    private final long voiceChannelId;

    public MusicState(AudioPlayer player, TrackScheduler scheduler, MusicSetting setting, long boundChannelId, long voiceChannelId) {
        this.player = player;
        this.scheduler = scheduler;
        this.setting = setting;
        this.lastInteract = System.currentTimeMillis();
        this.onStopLoadingCache = () -> {};
        this.boundChannelId = boundChannelId;
        this.voiceChannelId = voiceChannelId;
    }

    public void setLastInteract(long lastInteract) {
        this.lastInteract = lastInteract;
    }

    public void enqueue(QueueEntry entry) throws DuplicateTrackException, QueueFullException {
        this.scheduler.enqueue(entry);
    }

    public void skip(int amount) {
        this.player.stopTrack();
        this.scheduler.skip(amount);
    }

    public void shuffle() {
        this.scheduler.shuffleQueue();
    }

    public AudioPlayer getPlayer() {
        return this.player;
    }

    public MusicSetting getSetting() {
        return this.setting;
    }

    public long getBoundChannelId() {
        return boundChannelId;
    }

    public long getLastInteract() {
        return lastInteract;
    }

    public long getVoiceChannelId() {
        return voiceChannelId;
    }

    public void stopPlaying() {
        this.scheduler.clearQueue();
        this.player.stopTrack();
    }

    public QueueState getCurrentQueue() {
        List<QueueEntry> queue = this.scheduler.getCurrentQueue();
        AudioTrack track = this.player.getPlayingTrack();
        long position = track != null ? track.getPosition() : 0L;
        return new QueueState(queue, position);
    }

    /**
     * Retrieves remaining queue length in milliseconds.
     * @return Remaining length.
     */
    public long getRemainingLength() {
        long queueLength = this.scheduler.getQueueLength();
        AudioTrack track = this.player.getPlayingTrack();
        if (track != null) {
            queueLength -= track.getPosition();
        }
        return queueLength;
    }

    /**
     * Retrieves the queue length in milliseconds.
     * @return Queue length.
     */
    public long getQueueLength() {
        return this.scheduler.getQueueLength();
    }

    /**
     * Purges all waiting songs in the queue.
     * Does NOT remove the current song.
     */
    public void purgeWaitingQueue() {
        this.scheduler.purgeWaitingQueue();
    }

    /**
     * Sets handler on stop loading cache.
     * @param onStopLoadingCache Handler.
     */
    public void setOnStopLoadingCache(Runnable onStopLoadingCache) {
        this.onStopLoadingCache = onStopLoadingCache;
    }

    /**
     * When called, stops loading from the previous queue if it's still loading.
     */
    public void stopLoadingCache() {
        this.onStopLoadingCache.run();
    }
}
