package log;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import utils.BotUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DiscordSpamChecker {
    // 1 second
    private static final long SPAM_PREVENTION = TimeUnit.SECONDS.toMillis(1);

    private static final long CACHE_TIME = TimeUnit.MINUTES.toMillis(1);

    // User ID to last message time
    private final Map<Long, Long> messages;

    public DiscordSpamChecker() {
        this.messages = new HashMap<>();
    }

    public boolean isSpam(MessageReceivedEvent event) {
        long userId = event.getAuthor().getIdLong();
        long time = BotUtils.getIdCreationTime(event.getMessageIdLong());
        long lastMessageTime = this.messages.getOrDefault(userId, -1L);

        if (lastMessageTime == -1L) {
            this.messages.put(userId, time);
            return false;
        }

        long diff = time - lastMessageTime;
        if (diff < SPAM_PREVENTION) {
            return true;
        }
        this.messages.put(userId, lastMessageTime);

        this.removeOldMessageCache(time);
        return false;
    }

    private void removeOldMessageCache(long currentTime) {
        this.messages.entrySet().removeIf((e) -> CACHE_TIME < (currentTime - e.getValue()));
    }
}
