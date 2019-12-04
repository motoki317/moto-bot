package commands.base;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class BotCommand {
    public abstract boolean guildOnly();
    public abstract String[] names();
    public abstract void process(MessageReceivedEvent event, String[] args);
    protected void respond(MessageReceivedEvent event, CharSequence message) {
        event.getChannel().sendMessage(message).queue();
    }
}
