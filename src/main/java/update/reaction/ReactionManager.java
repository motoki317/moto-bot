package update.reaction;

import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;

import java.util.function.Predicate;

public interface ReactionManager {
    void addReactionHandler(long messageId,
                            Predicate<MessageReactionAddEvent> onReaction);
    void addReactionHandler(long messageId,
                            long userId,
                            Predicate<MessageReactionAddEvent> onReaction);
    void addReactionHandler(ReactionResponse botResponse);
    void handle(MessageReactionAddEvent event);
}
