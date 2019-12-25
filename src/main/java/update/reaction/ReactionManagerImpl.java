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
    public void addEventListener(long messageId, Predicate<MessageReactionAddEvent> onReaction) {
        ReactionResponse botResponse = new ReactionResponse(messageId, onReaction);
        addEventListener(botResponse);
    }

    @Override
    public void addEventListener(long messageId, long userId, Predicate<MessageReactionAddEvent> onReaction) {
        ReactionResponse botResponse = new ReactionResponse(messageId, userId, onReaction);
        addEventListener(botResponse);
    }

    @Override
    public void addEventListener(ReactionResponse botResponse) {
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
                botResponse.onDestroy();
            }
        }
    }

    /**
     * Clears reaction handlers that hasn't been used for more than `maxLive` attribute of each object.
     */
    @Override
    public void clearUp() {
        long now = System.currentTimeMillis();
        Predicate<ReactionResponse> removeIf = r -> (now - r.getUpdatedAt()) > r.getMaxLive();

        synchronized (this.lock) {
            this.messageHandlers.values().stream()
                    .filter(removeIf)
                    .forEach(ReactionResponse::onDestroy);
            this.messageHandlers.values().removeIf(removeIf);
        }
    }
}
