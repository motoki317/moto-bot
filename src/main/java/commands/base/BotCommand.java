package commands.base;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import utils.MinecraftColor;

import java.time.Instant;
import java.util.function.Consumer;

public abstract class BotCommand {
    public abstract boolean guildOnly();

    /**
     * Command names including aliases. Used to process command inputs.
     * @return Command names.
     */
    public abstract String[] names();

    /**
     * Command syntax. Used in help display.
     * @return Command syntax.
     */
    public abstract String syntax();

    /**
     * Shows short help in help command.
     * @return Short help.
     */
    public abstract String shortHelp();

    /**
     * Shows long help in help (cmd name) command.
     * @return Long help message.
     */
    public abstract Message longHelp();
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

    /**
     * Respond error message when something seriously went wrong (not because of bad user action) while processing a command.
     * @param event Message received event.
     * @param message Description of the error.
     */
    protected void respondError(MessageReceivedEvent event, CharSequence message) {
        event.getChannel().sendMessage(
                new EmbedBuilder()
                .setColor(MinecraftColor.RED.getColor())
                .setAuthor(":exclamation: Error!", null, event.getAuthor().getEffectiveAvatarUrl())
                .setDescription(message)
                .addField("What is this?", "An unexpected error occurred while processing your command. " +
                        "If the error persists, please contact the bot owner.", false)
                .setFooter("For more, visit the bot support server via info cmd.")
                .setTimestamp(Instant.now())
                .build()
        ).queue();
    }
}
