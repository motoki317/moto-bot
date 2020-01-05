package commands.base;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import utils.MinecraftColor;

import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

public abstract class BotCommand {
    public abstract boolean guildOnly();

    /**
     * Command names including aliases. Used to process command inputs.
     * Examples:
     * {{"help", "h"}} for 1-argument command.
     * {{"guild", "g"}, {"levelRank", "lRank"}} for 2-arguments command.
     * @return Command names.
     */
    @NotNull
    protected abstract String[][] names();

    /**
     * Get names for this command including aliases.
     * Examples:
     * {"help", "h"} for 1-argument command.
     * {"guild levelRank", "guild lRank", "g levelRank", "g lRank"} for 2-arguments command.
     * @return Names. Values possibly includes spaces.
     */
    public Set<String> getNames() {
        return getNamesRec(this.names(), 0);
    }

    private static Set<String> getNamesRec(String[][] base, int i) {
        if (base.length - 1 == i) {
            return new HashSet<>(Arrays.asList(base[i]));
        }

        Set<String> ret = new HashSet<>();
        for (String latter : getNamesRec(base, i + 1)) {
            for (String current : base[i]) {
                ret.add(current + " " + latter);
            }
        }
        return ret;
    }

    /**
     * Get base arguments length of this command.
     * Examples:
     * "help" command: 1,
     * "g levelrank" command: 2
     * @return Length of base arguments.
     */
    public int getArgumentsLength() {
        return this.names().length;
    }

    /**
     * Command syntax. Used in help display.
     * Example:
     * "help [command name]"
     * @return Command syntax.
     */
    @NotNull
    public abstract String syntax();

    /**
     * Shows short help in help command.
     * @return Short help.
     */
    @NotNull
    public abstract String shortHelp();

    /**
     * Shows long help in help (cmd name) command.
     * @return Long help message.
     */
    @NotNull
    public abstract Message longHelp();

    /**
     * Process a command.
     * @param event Discord message received event.
     * @param args Argument array, separated by space characters.
     */
    public abstract void process(@NotNull MessageReceivedEvent event, @NotNull String[] args);

    protected static void respond(MessageReceivedEvent event, CharSequence message) {
        event.getChannel().sendMessage(message).queue();
    }
    protected static void respond(MessageReceivedEvent event, Message message) {
        event.getChannel().sendMessage(message).queue();
    }
    protected static void respond(MessageReceivedEvent event, MessageEmbed message) {
        event.getChannel().sendMessage(message).queue();
    }
    protected static void respond(MessageReceivedEvent event, CharSequence message, Consumer<? super Message> onSuccess) {
        event.getChannel().sendMessage(message).queue(onSuccess);
    }
    protected static void respond(MessageReceivedEvent event, Message message, Consumer<? super Message> onSuccess) {
        event.getChannel().sendMessage(message).queue(onSuccess);
    }
    protected static void respond(MessageReceivedEvent event, MessageEmbed message, Consumer<? super Message> onSuccess) {
        event.getChannel().sendMessage(message).queue(onSuccess);
    }

    /**
     * Respond error message when something seriously went wrong (not because of bad user action) while processing a command.
     * @param event Message received event.
     * @param message Description of the error.
     */
    public static void respondError(MessageReceivedEvent event, CharSequence message) {
        event.getChannel().sendMessage(
                new EmbedBuilder()
                .setColor(MinecraftColor.RED.getColor())
                // Heavy exclamation mark :exclamation: ❗
                .setAuthor("\u2757 Error!", null, event.getAuthor().getEffectiveAvatarUrl())
                .setDescription(message)
                .addField("What is this?", "An unexpected error occurred while processing your command. " +
                        "If the error persists, please contact the bot owner.", false)
                .setFooter("For more, visit the bot support server via info cmd.")
                .setTimestamp(Instant.now())
                .build()
        ).queue();
    }
}
