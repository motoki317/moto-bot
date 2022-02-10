package log;

import commands.event.CommandEvent;

import java.util.HashMap;
import java.util.Map;

public class DiscordSpamChecker {
    // User ID to next cool-down expire time
    private final Map<Long, Long> coolDowns;

    public DiscordSpamChecker() {
        this.coolDowns = new HashMap<>();
    }

    public synchronized boolean isSpam(CommandEvent event, long nextCoolDown) {
        long userId = event.getAuthor().getIdLong();
        long now = System.currentTimeMillis();
        long lastCoolDownExpire = this.coolDowns.getOrDefault(userId, -1L);

        // Still on cool-down
        if (now < lastCoolDownExpire) {
            return true;
        }

        // Register next cool-down expire time
        long nextCoolDownExpire = event.getCreatedAt() + nextCoolDown;
        this.coolDowns.put(userId, nextCoolDownExpire);

        this.removeOldMessageCache(now);
        return false;
    }

    /**
     * Returns how much time is remained until the cool-down is expired for this user, in milliseconds.
     *
     * @param userId User ID.
     * @return Remaining cool-down in milliseconds.
     */
    public long nextCoolDownExpire(long userId) {
        return this.coolDowns.get(userId) - System.currentTimeMillis();
    }

    private void removeOldMessageCache(long now) {
        // Remove already expired cool-down
        this.coolDowns.entrySet().removeIf((e) -> e.getValue() <= now);
    }
}
