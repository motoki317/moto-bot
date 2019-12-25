package update.reaction;

import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import update.base.UserResponseManager;

import java.util.function.Predicate;

public interface ReactionManager extends UserResponseManager<MessageReactionAddEvent, ReactionResponse> {
    void addEventListener(long messageId,
                            Predicate<MessageReactionAddEvent> onReaction);
    void addEventListener(long messageId,
                            long userId,
                            Predicate<MessageReactionAddEvent> onReaction);
}
