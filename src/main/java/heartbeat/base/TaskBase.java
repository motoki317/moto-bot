package heartbeat.base;

/**
 * Interface for tasks to be scheduled at a fixed rate between each runs.
 */
public interface TaskBase {
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
