package commands.event;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.InteractionHook;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public record InteractionHookAdapter(InteractionHook hook) implements SentMessage {
    @Override
    public void getId(Consumer<Long> callback) {
        hook.retrieveOriginal().queue(m -> callback.accept(m.getIdLong()));
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
    public void editMessage(String message, Consumer<SentMessage> callback) {
        hook.editOriginal(message).queue(s -> callback.accept(new InteractionHookAdapter(hook)));
    }

    @Override
    public void editMessage(Message message, Consumer<SentMessage> callback) {
        hook.editOriginal(message).queue(s -> callback.accept(new InteractionHookAdapter(hook)));
    }

    @Override
    public void editMessage(MessageEmbed embed, Consumer<SentMessage> callback) {
        hook.editOriginalEmbeds(embed).queue(s -> callback.accept(new InteractionHookAdapter(hook)));
    }

    @Override
    public void delete() {
        hook.deleteOriginal().queue();
    }

    @Override
    public void deleteAfter(long timeout, TimeUnit unit) {
        hook.deleteOriginal().queueAfter(timeout, unit);
    }
}
