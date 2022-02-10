package commands.event.message;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.interactions.components.ComponentLayout;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public record ButtonClickEventAdapter(ButtonClickEvent event) implements SentMessage {
    @Override
    public void getId(Consumer<Long> callback) {
        callback.accept(event.getIdLong());
    }

    @Override
    public void editMessage(String message) {
        event.editMessage(message).queue();
    }

    @Override
    public void editMessage(Message message) {
        event.editMessage(message).queue();
    }

    @Override
    public void editMessage(MessageEmbed embed) {
        event.editMessageEmbeds(embed).queue();
    }

    @Override
    public void editMessage(String message, Consumer<SentMessage> callback) {
        event.editMessage(message).queue(h -> callback.accept(new InteractionHookAdapter(h)));
    }

    @Override
    public void editMessage(Message message, Consumer<SentMessage> callback) {
        event.editMessage(message).queue(h -> callback.accept(new InteractionHookAdapter(h)));
    }

    @Override
    public void editMessage(MessageEmbed embed, Consumer<SentMessage> callback) {
        event.editMessageEmbeds(embed).queue(h -> callback.accept(new InteractionHookAdapter(h)));
    }

    @Override
    public void editComponents(ComponentLayout... layouts) {
        event.editComponents(layouts).queue();
    }

    @Override
    public void delete() {
        event.getMessage().delete().queue();
    }

    @Override
    public void deleteAfter(long timeout, TimeUnit unit) {
        event.getMessage().delete().queueAfter(timeout, unit);
    }
}
