package commands.event;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public record SentMessageAdapter(Message m) implements SentMessage {
    @Override
    public long getId() {
        return m.getIdLong();
    }

    @Override
    public void editMessage(String message) {
        m.editMessage(message).queue();
    }

    @Override
    public void editMessage(Message message) {
        m.editMessage(message).queue();
    }

    @Override
    public void editMessage(MessageEmbed embed) {
        m.editMessageEmbeds(embed).queue();
    }

    @Override
    public void editMessage(String message, Consumer<SentMessage> callback) {
        m.editMessage(message).queue(s -> callback.accept(new SentMessageAdapter(s)));
    }

    @Override
    public void editMessage(Message message, Consumer<SentMessage> callback) {
        m.editMessage(message).queue(s -> callback.accept(new SentMessageAdapter(s)));
    }

    @Override
    public void editMessage(MessageEmbed embed, Consumer<SentMessage> callback) {
        m.editMessageEmbeds(embed).queue(s -> callback.accept(new SentMessageAdapter(s)));
    }

    @Override
    public void delete() {
        m.delete().queue();
    }

    @Override
    public void deleteAfter(long timeout, TimeUnit unit) {
        m.delete().queueAfter(timeout, unit);
    }
}
