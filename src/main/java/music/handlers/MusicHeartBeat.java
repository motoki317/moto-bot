package music.handlers;

import app.Bot;
import db.repository.base.MusicQueueRepository;
import heartbeat.base.TaskBase;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MusicHeartBeat implements TaskBase {
    private static final long INTERVAL = TimeUnit.MINUTES.toMillis(1);

    private static final long QUEUE_MAX_AGE = TimeUnit.DAYS.toMillis(7);

    private final Logger logger;
    private final MusicQueueRepository musicQueueRepository;
    private final MusicAutoLeaveChecker autoLeaveChecker;

    // Check old queue cache every 60 minutes
    private int removeOldQueueCount = 0;

    public MusicHeartBeat(Bot bot, MusicAutoLeaveChecker autoLeaveChecker) {
        this.logger = bot.getLogger();
        this.musicQueueRepository = bot.getDatabase().getMusicQueueRepository();
        this.autoLeaveChecker = autoLeaveChecker;
    }

    @Override
    public @NotNull String getName() {
        return "Music";
    }

    @Override
    public void run() {
        this.autoLeaveChecker.checkAllGuilds();

        removeOldQueueCount++;
        if (removeOldQueueCount == 60) {
            removeOldQueueCount = 0;
            this.removeOldQueues();
        }
    }

    /**
     * Checks old queue caches and removes old enough ones
     */
    private void removeOldQueues() {
        Date threshold = new Date(System.currentTimeMillis() - QUEUE_MAX_AGE);
        boolean res = this.musicQueueRepository.deleteAllOlderThan(threshold);
        if (!res) {
            this.logger.log(0, "Music queue cache: failed to delete old queues");
        }
    }

    @Override
    public long getFirstDelay() {
        return INTERVAL;
    }

    @Override
    public long getInterval() {
        return INTERVAL;
    }
}
