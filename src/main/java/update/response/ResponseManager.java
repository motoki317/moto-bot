package update.response;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import update.base.UserResponseManager;

import java.util.function.Predicate;

public interface ResponseManager extends UserResponseManager<MessageReceivedEvent, Response> {
    void addEventListener(long channelId,
                             long userId,
                             Predicate<MessageReceivedEvent> onResponse);
}
