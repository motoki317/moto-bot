package music.exception;

public class QueueFullException extends Exception {
    public QueueFullException(String message) {
        super(message);
    }
}
