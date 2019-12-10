package heartbeat;

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

    private final Logger logger;

    private final Timer timer;

    private final List<Task> tasks;

    public HeartBeat(Bot bot) {
        this.logger = bot.getLogger();
        this.timer = new Timer();
        this.tasks = new ArrayList<>();

        this.tasks.add(new Task(
                this.timer,
                runnablePlayerTracker(bot),
                PLAYER_TERRITORY_TRACKER_DELAY,
                PLAYER_TERRITORY_TRACKER_DELAY
        ));
        this.tasks.add(new Task(
                this.timer,
                runnableTerritoryTracker(bot),
                PLAYER_TERRITORY_TRACKER_DELAY,
                PLAYER_TERRITORY_TRACKER_DELAY
        ));
    }

    @Override
    public void run() {
        this.logger.log(-1, "Starting heartbeat... (Thread id " + this.getId() + ")");

        this.tasks.forEach(Task::start);
    }

    private static Runnable runnablePlayerTracker(Bot bot) {
        PlayerTracker playerTracker = new PlayerTracker(bot);
        return playerTracker::run;
    }

    private static Runnable runnableTerritoryTracker(Bot bot) {
        TerritoryTracker territoryTracker = new TerritoryTracker(bot);
        return territoryTracker::run;
    }

    @Override
    protected void cleanUp() {
        this.logger.log(-1, "Stopping heartbeat... (Thread id " + this.getId() + ")");
        this.timer.cancel();
    }
}
