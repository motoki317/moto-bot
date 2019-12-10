package heartbeat;

import java.util.Timer;
import java.util.TimerTask;

class Task {
    private final Timer timer;
    private final Runnable task;
    private final long firstDelay;
    private final long interval;

    Task(Timer timer, Runnable task, long firstDelay, long interval) {
        this.timer = timer;
        this.task = task;
        this.firstDelay = firstDelay;
        this.interval = interval;
    }

    void start() {
        timer.schedule(this.getTimerTask(), this.firstDelay);
    }

    private TimerTask getTimerTask() {
        Task task = this;
        return new TimerTask() {
            @Override
            public void run() {
                task.task.run();
                task.reschedule();
            }
        };
    }

    private void reschedule() {
        this.timer.schedule(this.getTimerTask(), this.interval);
    }
}
