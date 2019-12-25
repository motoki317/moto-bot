package update.reaction;

import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

// Represents bot's response to user's reaction to a message.
class ReactionResponse {
    private final long messageId;

    private boolean userPrivate;
    private long userId;

    private final Predicate<MessageReactionAddEvent> onReaction;

    private long updatedAt;
    private long maxLive;

    ReactionResponse(long messageId,
                     Predicate<MessageReactionAddEvent> onReaction) {
        this.messageId = messageId;
        this.onReaction = onReaction;
        this.updatedAt = System.currentTimeMillis();
        this.maxLive = TimeUnit.MINUTES.toMillis(10);
    }

    ReactionResponse(long messageId,
                     long userId,
                     Predicate<MessageReactionAddEvent> onReaction) {
        this(messageId, onReaction);
        this.userPrivate = true;
        this.userId = userId;
    }

    // boolean returned by predicate indicates if manager should discard this response object.
    boolean handle(MessageReactionAddEvent event) {
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

    long getUpdatedAt() {
        return updatedAt;
    }

    long getMaxLive() {
        return maxLive;
    }

    public void setMaxLive(long maxLive) {
        this.maxLive = maxLive;
    }
}
