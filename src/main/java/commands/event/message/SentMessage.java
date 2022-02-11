package commands.event.message;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.interactions.components.ComponentLayout;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public interface SentMessage {
    void getMessage(Consumer<Message> callback);

    void editMessage(String message);

    void editMessage(Message message);

    void editMessage(MessageEmbed embed);

    void editMessage(String message, Consumer<SentMessage> callback);

    void editMessage(Message message, Consumer<SentMessage> callback);

    void editMessage(MessageEmbed embed, Consumer<SentMessage> callback);

    void editComponents(ComponentLayout... layouts);

    void delete();

    void deleteAfter(long timeout, TimeUnit unit);
}
