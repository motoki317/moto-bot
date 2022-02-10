package commands.event;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

public interface SentMessage {
    long getId();
    void editMessage(String message);
    void editMessage(Message message);
    void editMessage(MessageEmbed embed);
}
