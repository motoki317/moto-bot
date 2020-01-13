package heartbeat;

import app.Bot;
import heartbeat.base.TaskBase;
import heartbeat.tracking.GuildTracker;
import heartbeat.tracking.PlayerTracker;
import heartbeat.tracking.TerritoryTracker;
import log.Logger;
import utils.StoppableThread;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.function.BiConsumer;

public class HeartBeat extends StoppableThread {
    private final Logger logger;

    private final Timer timer;

    private final List<Task> tasks;

    public HeartBeat(Bot bot) {
        this.logger = bot.getLogger();
        this.timer = new Timer();
        this.tasks = new ArrayList<>();

        BiConsumer<TaskBase, String> addTask = (task, name) -> this.tasks.add(new Task(
                this.timer,
                () -> {
                    long start = System.nanoTime();
                    task.run();
                    long end = System.nanoTime();
                    bot.getLogger().log(-1, String.format("HeartBeat: %s took %.6f ms to run.",
                            name,
                            ((double) (end - start)) / 1_000_000D)
                    );
                },
                task.getFirstDelay(),
                task.getInterval()
        ));

        final Object dbLock = new Object();

        addTask.accept(new PlayerTracker(bot, dbLock), "Player Tracker");
        addTask.accept(new TerritoryTracker(bot, dbLock), "Territory Tracker");
        addTask.accept(new GuildTracker(bot), "Guild Tracker");
    }

    @Override
    public void run() {
        this.logger.debug("Starting heartbeat... (Thread id " + this.getId() + ")");

        this.tasks.forEach(Task::start);
    }

    @Override
    protected void cleanUp() {
        this.logger.log(-1, "Stopping heartbeat... (Thread id " + this.getId() + ")");
        this.timer.cancel();
    }
}
