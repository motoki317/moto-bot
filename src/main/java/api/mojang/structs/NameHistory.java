package api.mojang.structs;

import org.jetbrains.annotations.NotNull;
import utils.UUID;

import java.util.Comparator;
import java.util.List;

public class NameHistory {
    public static class NameHistoryEntry {
        private final String username;
        // unix millis
        private final long changedToAt;

        public NameHistoryEntry(String username, long changedToAt) {
            this.username = username;
            this.changedToAt = changedToAt;
        }

        public String getUsername() {
            return username;
        }

        public long getChangedToAt() {
            return changedToAt;
        }
    }

    private final UUID uuid;
    private final List<NameHistoryEntry> history;

    public NameHistory(UUID uuid, List<NameHistoryEntry> history) {
        this.uuid = uuid;
        this.history = history;
        this.history.sort(Comparator.comparingLong(h -> h.changedToAt));
    }

    @NotNull
    public UUID getUuid() {
        return uuid;
    }

    @NotNull
    public List<NameHistoryEntry> getHistory() {
        return history;
    }

    /**
     * Retrieves username at given unix millis time.
     * @param unixMillis Unix milliseconds.
     * @return Username at given time.
     */
    @NotNull
    public String getNameAt(long unixMillis) {
        for (int i = 0; i < this.history.size() - 1; i++) {
            NameHistoryEntry older = this.history.get(i);
            NameHistoryEntry newer = this.history.get(i + 1);
            if (older.changedToAt <= unixMillis && unixMillis < newer.changedToAt) {
                return older.username;
            }
        }

        return this.history.get(this.history.size() - 1).username;
    }
}
