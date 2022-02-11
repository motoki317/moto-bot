package music;

import java.util.List;

/**
 * Current queue state record
 */
public record QueueState(List<QueueEntry> queue, long position) {
}
