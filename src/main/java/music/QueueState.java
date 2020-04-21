package music;

import java.util.Deque;

/**
 * Current queue state record
 */
class QueueState {
    private final Deque<QueueEntry> queue;
    private final long position;

    QueueState(Deque<QueueEntry> queue, long position) {
        this.queue = queue;
        this.position = position;
    }

    Deque<QueueEntry> getQueue() {
        return queue;
    }

    long getPosition() {
        return position;
    }
}
