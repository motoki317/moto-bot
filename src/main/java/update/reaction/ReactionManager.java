package update.reaction;

import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import update.base.UserResponseManager;

public interface ReactionManager extends UserResponseManager<MessageReactionAddEvent, ReactionResponse> {}
