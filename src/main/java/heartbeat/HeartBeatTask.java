package heartbeat;

import heartbeat.base.TaskBase;
import log.Logger;

import java.util.Timer;
import java.util.TimerTask;

public class HeartBeatTask {
    private final Timer timer;
    private final Logger logger;
    private final TaskBase task;

    public HeartBeatTask(Logger logger, TaskBase task) {
        this.timer = new Timer();
        this.logger = logger;
        this.task = task;
    }

    public void start() {
        timer.schedule(this.getTimerTask(), this.task.getFirstDelay());
    }

    public void clearUp() {
        timer.cancel();
    }

    private TimerTask getTimerTask() {
        HeartBeatTask t = this;
        return new TimerTask() {
            @Override
            public void run() {
                long start = System.nanoTime();
                try {
                    t.task.run();
                } catch (Exception e) {
                    t.logger.logException(String.format("HeatBeat: %s: caught exception", t.task.getName()), e);
                }
                long end = System.nanoTime();
                t.logger.log(-1, String.format("HeartBeat: %s took %.6f ms to run.",
                        t.task.getName(),
                        ((double) (end - start)) / 1_000_000D)
                );
                t.reschedule();
            }
        };
    }

    private void reschedule() {
        // Re-instantiate the same timer task each time to queue multiple times
        this.timer.schedule(this.getTimerTask(), this.task.getInterval());
    }
}
