package update.response;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import update.base.UserResponseManager;

public interface ResponseManager extends UserResponseManager<MessageReceivedEvent, Response> {}
