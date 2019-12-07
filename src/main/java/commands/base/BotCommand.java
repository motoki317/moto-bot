package commands.base;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.function.Consumer;

public abstract class BotCommand {
    public abstract boolean guildOnly();
    public abstract String[] names();
    public abstract void process(MessageReceivedEvent event, String[] args);

    protected void respond(MessageReceivedEvent event, CharSequence message) {
        event.getChannel().sendMessage(message).queue();
    }
    protected void respond(MessageReceivedEvent event, Message message) {
        event.getChannel().sendMessage(message).queue();
    }
    protected void respond(MessageReceivedEvent event, MessageEmbed message) {
        event.getChannel().sendMessage(message).queue();
    }
    protected void respond(MessageReceivedEvent event, CharSequence message, Consumer<? super Message> onSuccess) {
        event.getChannel().sendMessage(message).queue(onSuccess);
    }
    protected void respond(MessageReceivedEvent event, Message message, Consumer<? super Message> onSuccess) {
        event.getChannel().sendMessage(message).queue(onSuccess);
    }
    protected void respond(MessageReceivedEvent event, MessageEmbed message, Consumer<? super Message> onSuccess) {
        event.getChannel().sendMessage(message).queue(onSuccess);
    }
}
