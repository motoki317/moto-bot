package heartbeat.base;

import org.jetbrains.annotations.NotNull;

/**
 * Interface for tasks to be scheduled at a fixed rate between each runs.
 */
public interface TaskBase {
    /**
     * Should return the name with which this task is logged.
     */
    @NotNull
    String getName();

    /**
     * Executes the task once.
     */
    void run();

    /**
     * First delay in ms.
     * @return First delay.
     */
    long getFirstDelay();

    /**
     * Interval in ms.
     * @return Interval.
     */
    long getInterval();
}
