package heartbeat;

import api.WynnApi;
import app.Bot;
import heartbeat.tracking.PlayerTracker;
import heartbeat.tracking.TerritoryTracker;
import log.Logger;
import utils.StoppableThread;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

public class HeartBeat extends StoppableThread {
    private static final long PLAYER_TERRITORY_TRACKER_DELAY = TimeUnit.SECONDS.toMillis(30);
    private static final long UPDATERS_INTERVAL = TimeUnit.MINUTES.toMillis(10);

    private final Logger logger;

    private final Timer timer;

    private final List<Task> tasks;

    public HeartBeat(Bot bot) {
        this.logger = bot.getLogger();
        this.timer = new Timer();
        this.tasks = new ArrayList<>();

        final Object dbLock = new Object();
        this.tasks.add(new Task(
                this.timer,
                runnablePlayerTracker(bot, dbLock),
                PLAYER_TERRITORY_TRACKER_DELAY,
                PLAYER_TERRITORY_TRACKER_DELAY
        ));
        this.tasks.add(new Task(
                this.timer,
                runnableTerritoryTracker(bot, dbLock),
                PLAYER_TERRITORY_TRACKER_DELAY,
                PLAYER_TERRITORY_TRACKER_DELAY
        ));

        this.tasks.add(new Task(
                this.timer,
                () -> {
                    bot.getReactionManager().clearUp();
                    bot.getResponseManager().clearUp();
                },
                UPDATERS_INTERVAL,
                UPDATERS_INTERVAL
        ));

        this.tasks.add(new Task(
                this.timer,
                WynnApi::updateCache,
                TimeUnit.MINUTES.toMillis(10),
                TimeUnit.MINUTES.toMillis(10)
        ));
    }

    @Override
    public void run() {
        this.logger.log(-1, "Starting heartbeat... (Thread id " + this.getId() + ")");

        this.tasks.forEach(Task::start);
    }

    private static Runnable runnablePlayerTracker(Bot bot, Object dbLock) {
        PlayerTracker playerTracker = new PlayerTracker(bot, dbLock);
        return () -> {
            long start = System.nanoTime();
            playerTracker.run();
            long end = System.nanoTime();
            bot.getLogger().log(-1, String.format("Player tracker took %.6f ms to run.", ((double) (end - start)) / 1_000_000D));
        };
    }

    private static Runnable runnableTerritoryTracker(Bot bot, Object dbLock) {
        TerritoryTracker territoryTracker = new TerritoryTracker(bot, dbLock);
        return () -> {
            long start = System.nanoTime();
            territoryTracker.run();
            long end = System.nanoTime();
            bot.getLogger().log(-1, String.format("Territory tracker took %.6f ms to run.", ((double) (end - start)) / 1_000_000D));
        };
    }

    @Override
    protected void cleanUp() {
        this.logger.log(-1, "Stopping heartbeat... (Thread id " + this.getId() + ")");
        this.timer.cancel();
    }
}
