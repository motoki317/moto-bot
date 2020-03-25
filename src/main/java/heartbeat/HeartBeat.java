package heartbeat;

import app.Bot;
import heartbeat.base.TaskBase;
import heartbeat.tasks.*;
import log.Logger;
import utils.StoppableThread;

import java.util.ArrayList;
import java.util.List;

public class HeartBeat extends StoppableThread {
    private final Logger logger;

    private final List<HeartBeatTask> tasks;

    public HeartBeat(Bot bot) {
        this.logger = bot.getLogger();
        this.tasks = new ArrayList<>();

        final Object dbLock = new Object();

        addTask(new PlayerTracker(bot, dbLock));
        addTask(new TerritoryTracker(bot, dbLock));
        addTask(new GuildTracker(bot));
        addTask(new GuildLeaderboardTracker(bot));
        addTask(new TrackingManager(bot));
        addTask(new PlayerUUIDRetriever(bot));
    }

    private void addTask(TaskBase task) {
        this.tasks.add(new HeartBeatTask(this.logger, task));
    }

    @Override
    public void run() {
        this.logger.debug("Starting heartbeat... (Thread id " + this.getId() + ")");
        this.tasks.forEach(HeartBeatTask::start);
    }

    @Override
    protected void cleanUp() {
        this.logger.log(-1, "Stopping heartbeat... (Thread id " + this.getId() + ")");
        this.tasks.forEach(HeartBeatTask::clearUp);
    }
}
