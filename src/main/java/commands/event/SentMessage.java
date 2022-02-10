package commands.event;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public interface SentMessage {
    long getId();

    void editMessage(String message);

    void editMessage(Message message);

    void editMessage(MessageEmbed embed);

    void editMessage(String message, Consumer<SentMessage> callback);

    void editMessage(Message message, Consumer<SentMessage> callback);

    void editMessage(MessageEmbed embed, Consumer<SentMessage> callback);

    void deleteAfter(long timeout, TimeUnit unit);
}