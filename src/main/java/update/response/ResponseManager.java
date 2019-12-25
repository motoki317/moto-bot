package update.response;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.function.Predicate;

public interface ResponseManager {
    void waitForUserResponse(long channelId,
                             long userId,
                             Predicate<MessageReceivedEvent> onResponse);
    void waitForUserResponse(Response botResponse);
    void handle(MessageReceivedEvent event);
}
