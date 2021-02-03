package music;

public enum RepeatState {
    OFF("", "No repeat.", false),
    ONE("Repeating one track", "Repeats the current song.", true),
    QUEUE("Repeating whole queue",
            "Repeats the whole queue; the current song will be added to the last of the queue, when finished.",
            true),
    RANDOM("Playing randomly", "Picks a random song from the queue and plays it.", false),
    RANDOM_REPEAT("Playing randomly and repeating",
            "Picks a random song from the queue and plays it. " +
                    "The current song will be added to the end of the queue, when finished.",
            true);

    private final String message;
    private final String description;
    private final boolean endlessMode;

    RepeatState(String message, String description, boolean endlessMode) {
        this.message = message;
        this.description = description;
        this.endlessMode = endlessMode;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }

    public boolean isEndlessMode() {
        return endlessMode;
    }
}
