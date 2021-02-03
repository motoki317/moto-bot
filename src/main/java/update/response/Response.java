package update.response;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import update.base.UserResponseListener;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

// Represents bot's response to user's response (input).
public class Response implements UserResponseListener<MessageReceivedEvent> {
    private final long channelId;
    private final long userId;

    private final Predicate<MessageReceivedEvent> onResponse;
    // Called when this instance is discarded by manager
    private Runnable onDestroy;

    private final long updatedAt;
    private final long maxLive;

    public Response(long channelId,
                    long userId,
                    Predicate<MessageReceivedEvent> onResponse) {
        this.channelId = channelId;
        this.userId = userId;
        this.onResponse = onResponse;
        this.onDestroy = () -> {};
        this.updatedAt = System.currentTimeMillis();
        this.maxLive = TimeUnit.MINUTES.toMillis(10);
    }

    // boolean returned by predicate indicates if manager should discard this response object.
    public boolean handle(MessageReceivedEvent event) {
        return this.onResponse.test(event);
    }

    long getChannelId() {
        return channelId;
    }

    long getUserId() {
        return userId;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public long getMaxLive() {
        return maxLive;
    }

    public void onDestroy() {
        this.onDestroy.run();
    }

    public void setOnDestroy(Runnable onDestroy) {
        this.onDestroy = onDestroy;
    }
}
