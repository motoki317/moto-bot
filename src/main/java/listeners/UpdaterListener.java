package listeners;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import update.reaction.ReactionManager;
import update.response.ResponseManager;

import javax.annotation.Nonnull;

public class UpdaterListener extends ListenerAdapter {
    private final ResponseManager responseManager;
    private final ReactionManager reactionManager;

    public UpdaterListener(ResponseManager responseManager, ReactionManager reactionManager) {
        this.responseManager = responseManager;
        this.reactionManager = reactionManager;
    }

    @Override
    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        this.responseManager.handle(event);
    }

    @Override
    public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event) {
        this.reactionManager.handle(event);
    }
}
