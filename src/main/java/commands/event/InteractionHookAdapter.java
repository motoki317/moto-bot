package commands.event;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.util.concurrent.TimeUnit;

public record InteractionHookAdapter(InteractionHook hook) implements SentMessage {
    @Override
    public long getId() {
        return hook.retrieveOriginal().complete().getIdLong();
    }

    @Override
    public void editMessage(String message) {
        hook.editOriginal(message).queue();
    }

    @Override
    public void editMessage(Message message) {
        hook.editOriginal(message).queue();
    }

    @Override
    public void editMessage(MessageEmbed embed) {
        hook.editOriginalEmbeds(embed).queue();
    }

    @Override
    public void deleteAfter(long timeout, TimeUnit unit) {
        hook.deleteOriginal().queueAfter(timeout, unit);
    }
}
