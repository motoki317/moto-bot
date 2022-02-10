package commands.event;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import utils.MinecraftColor;

import java.time.Instant;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface CommandEvent {
    JDA getJDA();

    /**
     * Retrieves the time this event was created.
     *
     * @return Time in unix milliseconds.
     */
    long getCreatedAt();

    /**
     * Retrieves the raw text contents of this event.
     * Note that prefix is NOT stripped.
     *
     * @return Raw text content.
     */
    String getContentRaw();

    /**
     * Checks if this event was fired from a guild.
     *
     * @return {@code true} if from guild.
     */
    boolean isFromGuild();

    Guild getGuild();

    MessageChannel getChannel();

    User getAuthor();

    Member getMember();

    /**
     * Notifies the user that the bot acknowledged the command action.
     */
    void acknowledge();

    void reply(String message);

    void reply(Message message);

    void reply(MessageEmbed embed);

    void reply(String message, Consumer<SentMessage> callback);

    void reply(Message message, Consumer<SentMessage> callback);

    void reply(MessageEmbed embed, Consumer<SentMessage> callback);

    void replyMultiPage(String message, Function<Integer, Message> pages, Supplier<Integer> maxPage);

    void replyMultiPage(Message message, Function<Integer, Message> pages, Supplier<Integer> maxPage);

    void replyMultiPage(MessageEmbed embed, Function<Integer, Message> pages, Supplier<Integer> maxPage);

    /**
     * Reply exception in red embed message.
     *
     * @param message Description of the exception.
     */
    default void replyException(CharSequence message) {
        reply(new EmbedBuilder()
                .setColor(MinecraftColor.RED.getColor())
                .setDescription(message)
                .build());
    }

    /**
     * Reply with error message when something seriously went wrong (not because of bad user action) while processing a command.
     *
     * @param message Description of the error.
     */
    default void replyError(String message) {
        reply(new EmbedBuilder()
                .setColor(MinecraftColor.RED.getColor())
                // Heavy exclamation mark :exclamation: ❗
                .setAuthor("\u2757 Error!", null, getAuthor().getEffectiveAvatarUrl())
                .setDescription(message)
                .addField("What is this?", "An unexpected error occurred while processing your command. " +
                        "If the error persists, please contact the bot owner.", false)
                .setFooter("For more, visit the bot support server via info cmd.")
                .setTimestamp(Instant.now())
                .build());
    }
}
