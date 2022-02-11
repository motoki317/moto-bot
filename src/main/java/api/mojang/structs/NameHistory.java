package api.mojang.structs;

import org.jetbrains.annotations.NotNull;
import utils.UUID;

import java.util.Comparator;
import java.util.List;

public record NameHistory(@NotNull UUID uuid, @NotNull List<NameHistoryEntry> history) {
    public record NameHistoryEntry(String username, long changedToAt) {
    }

    public NameHistory(@NotNull UUID uuid, @NotNull List<NameHistoryEntry> history) {
        this.uuid = uuid;
        this.history = history;
        this.history.sort(Comparator.comparingLong(h -> h.changedToAt));
    }

    /**
     * Retrieves username at given unix millis time.
     *
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
