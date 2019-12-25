package listeners;

import net.dv8tion.jda.api.entities.User;
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
        // Do not respond to webhook/bot messages
        if (event.isWebhookMessage() || event.getAuthor().isBot()) return;

        this.responseManager.handle(event);
    }

    @Override
    public void onMessageReactionAdd(@Nonnull MessageReactionAddEvent event) {
        // Do not respond to bot messages
        long authorId = event.getUserIdLong();
        User author = event.getJDA().getUserById(authorId);
        long botId = event.getJDA().getSelfUser().getIdLong();
        if (authorId == botId || (author != null && author.isBot())) return;

        this.reactionManager.handle(event);
    }
}
