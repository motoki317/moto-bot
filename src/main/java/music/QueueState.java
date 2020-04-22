package music;

import java.util.List;

/**
 * Current queue state record
 */
public class QueueState {
    private final List<QueueEntry> queue;
    private final long position;

    QueueState(List<QueueEntry> queue, long position) {
        this.queue = queue;
        this.position = position;
    }

    public List<QueueEntry> getQueue() {
        return queue;
    }

    public long getPosition() {
        return position;
    }
}
