package commands.event;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.concurrent.TimeUnit;

public interface SentMessage {
    long getId();
    void editMessage(String message);
    void editMessage(Message message);
    void editMessage(MessageEmbed embed);
    void deleteMessageAfter(long timeout, TimeUnit unit);
}
