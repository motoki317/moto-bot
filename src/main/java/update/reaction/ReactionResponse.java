package update.reaction;

import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import update.base.UserResponseListener;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

// Represents bot's response to user's reaction to a message.
public class ReactionResponse implements UserResponseListener<MessageReactionAddEvent> {
    private final long messageId;

    private boolean userPrivate;
    private long userId;

    private final Predicate<MessageReactionAddEvent> onReaction;
    // Called when this instance is discarded by manager
    private Runnable onDestroy;

    private long updatedAt;
    private long maxLive;

    protected ReactionResponse(long messageId,
                     Predicate<MessageReactionAddEvent> onReaction) {
        this.messageId = messageId;
        this.onReaction = onReaction;
        this.onDestroy = () -> {};
        this.updatedAt = System.currentTimeMillis();
        this.maxLive = TimeUnit.MINUTES.toMillis(10);
    }

    protected ReactionResponse(long messageId,
                     long userId,
                     Predicate<MessageReactionAddEvent> onReaction) {
        this(messageId, onReaction);
        this.userPrivate = true;
        this.userId = userId;
    }

    // boolean returned by predicate indicates if manager should discard this response object.
    public boolean handle(MessageReactionAddEvent event) {
        this.updatedAt = System.currentTimeMillis();
        return this.onReaction.test(event);
    }

    long getMessageId() {
        return messageId;
    }

    boolean isUserPrivate() {
        return userPrivate;
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

    public void setMaxLive(long maxLive) {
        this.maxLive = maxLive;
    }

    public void onDestroy() {
        this.onDestroy.run();
    }

    public void setOnDestroy(Runnable onDestroy) {
        this.onDestroy = onDestroy;
    }
}
