package commands;

import commands.base.GenericCommand;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class Ping extends GenericCommand {
    @Override
    public String[] names() {
        return new String[]{"ping"};
    }

    @Override
    public void process(MessageReceivedEvent event, String[] args) {
        respond(event, "Pong!");
    }
}
