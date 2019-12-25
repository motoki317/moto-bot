package update.reaction;

import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class ReactionManagerImpl implements ReactionManager {
    private final Map<Long, ReactionResponse> messageHandlers;

    private final Object lock;

    public ReactionManagerImpl() {
        this.messageHandlers = new HashMap<>();
        this.lock = new Object();
    }

    @Override
    public void addReactionHandler(long messageId, Predicate<MessageReactionAddEvent> onReaction) {
        ReactionResponse botResponse = new ReactionResponse(messageId, onReaction);
        addReactionHandler(botResponse);
    }

    @Override
    public void addReactionHandler(long messageId, long userId, Predicate<MessageReactionAddEvent> onReaction) {
        ReactionResponse botResponse = new ReactionResponse(messageId, userId, onReaction);
        addReactionHandler(botResponse);
    }

    @Override
    public void addReactionHandler(ReactionResponse botResponse) {
        synchronized (this.lock) {
            this.messageHandlers.put(botResponse.getMessageId(), botResponse);
        }
    }

    @Override
    public void handle(MessageReactionAddEvent event) {
        long messageId = event.getMessageIdLong();

        synchronized (this.lock) {
            if (!this.messageHandlers.containsKey(messageId)) {
                return;
            }

            ReactionResponse botResponse = this.messageHandlers.get(messageId);
            if (botResponse.isUserPrivate()) {
                long userId = event.getUserIdLong();
                if (botResponse.getUserId() != userId) {
                    return;
                }
            }

            boolean res = botResponse.handle(event);

            if (res) {
                this.messageHandlers.remove(messageId);
            }
        }
    }

    /**
     * Clears reaction handlers that hasn't been used for more than `maxLive` attribute of each object.
     */
    public void clearUp() {
        long now = System.currentTimeMillis();
        this.messageHandlers.values().removeIf(r -> (now - r.getUpdatedAt()) > r.getMaxLive());
    }
}
