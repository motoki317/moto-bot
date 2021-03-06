package update.reaction;

import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import update.multipage.MultipageHandler;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class ReactionManagerImpl implements ReactionManager {
    // message id to reaction response handler
    private final Map<Long, ReactionResponse> messageHandlers;

    private final Object lock;

    public ReactionManagerImpl() {
        this.messageHandlers = new HashMap<>();
        this.lock = new Object();

        long delay = TimeUnit.MINUTES.toMillis(10);
        ReactionManagerImpl manager = this;
        new Timer().scheduleAtFixedRate(
                new TimerTask() {
                    @Override
                    public void run() {
                        manager.clearUp();
                    }
                },
                delay,
                delay
        );
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
    private void clearUp() {
        long now = System.currentTimeMillis();
        Predicate<ReactionResponse> removeIf = r -> (now - r.getUpdatedAt()) > r.getMaxLive();

        synchronized (this.lock) {
            this.messageHandlers.values().stream()
                    .filter(removeIf)
                    .forEach(ReactionResponse::onDestroy);
            this.messageHandlers.values().removeIf(removeIf);
        }
    }

    @Override
    public boolean setPage(long userId, long channelId, int newPage) {
        MultipageHandler handler = this.messageHandlers.values().stream()
                .filter(h -> h.getUserId() == userId)
                .filter(h -> h.getChannelId() == channelId)
                .filter(h -> h instanceof MultipageHandler)
                .map(h -> (MultipageHandler) h)
                .max(Comparator.comparingLong(ReactionResponse::getMessageId))
                .orElse(null);

        if (handler == null) {
            return false;
        }

        handler.setPageAndUpdate(newPage);
        return true;
    }
}
