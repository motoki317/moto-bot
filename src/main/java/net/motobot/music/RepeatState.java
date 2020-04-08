package net.motobot.music;

import java.io.Serializable;

public enum RepeatState implements Serializable {
    OFF("", "No repeat.", false),
    ONE("Repeating one track", "Repeats currently playing track.", true),
    QUEUE("Repeating whole queue", "Repeats the whole queue; currently playing track will be added to the last of the queue, when finished.", true),
    RANDOM("Playing randomly", "Picks a random song from the queue and plays it.", false),
    RANDOM_REPEAT("Playing randomly and repeating", "Picks a random song from the queue and plays it, with the whole queue repeating.", true);

    private String message;
    private String description;
    private boolean endlessMode;

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
