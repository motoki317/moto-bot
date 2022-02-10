package update.selection;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.Nullable;
import update.response.Response;
import utils.MinecraftColor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Implements a response handler, choosing by number specified by user.
 */
public class SelectionHandler extends Response {
    private final List<String> choiceList;
    @Nullable
    private Message askMessage;

    public SelectionHandler(long channelId, long userId, List<String> choiceList, Consumer<String> onChosen) {
        super(channelId, userId, event -> customOnResponse(event, choiceList, onChosen));
        this.choiceList = choiceList;
        this.setOnDestroy(customOnDestroy());
    }

    /**
     * Gets message to ask the user to make a choice.
     *
     * @param channel   Message channel.
     * @param user      User object to get user name & discriminator.
     * @param onSuccess Callback on message send success.
     */
    public void sendMessage(MessageChannel channel, User user, Runnable onSuccess) {
        List<String> messages = new ArrayList<>();
        for (int i = 0; i < this.choiceList.size(); i++) {
            String choice = this.choiceList.get(i);
            messages.add(
                    String.format("%s. `%s`", i + 1, choice)
            );
        }

        Message message = new MessageBuilder(
                new EmbedBuilder()
                        .setAuthor("Choose one from below.", null, user.getEffectiveAvatarUrl())
                        .setDescription("Type a number, or 'cancel' in this channel to make a choice. " +
                                "Your command will then be processed, unless you type in 'cancel.'")
                        .addField("Choices", String.join("\n", messages), false)
                        .setFooter(String.format("Command by %s#%s", user.getName(), user.getDiscriminator()))
                        .setTimestamp(Instant.now())
                        .build()
        ).build();

        channel.sendMessage(message).queue(msg -> {
            this.askMessage = msg;
            onSuccess.run();
        });
    }

    private Runnable customOnDestroy() {
        return () -> {
            if (this.askMessage != null) {
                this.askMessage.delete().queue();
            }
        };
    }

    private static boolean customOnResponse(MessageReceivedEvent event, List<String> choiceList, Consumer<String> onChosen) {
        String messageRaw = event.getMessage().getContentRaw();
        User user = event.getAuthor();
        if (messageRaw.equalsIgnoreCase("cancel")) {
            // Try to remove user message
            try {
                event.getMessage().delete().queue();
            } catch (Exception ignored) {
            }

            event.getTextChannel().sendMessageEmbeds(
                            new EmbedBuilder()
                                    .setColor(MinecraftColor.RED.getColor())
                                    .setDescription("Cancelled.")
                                    .setFooter(String.format("Command by %s#%s", user.getName(), user.getDiscriminator()))
                                    .setTimestamp(Instant.now())
                                    .build()
                    ).delay(5, TimeUnit.SECONDS)
                    .flatMap(Message::delete)
                    .queue();
            return true;
        }

        int num;
        try {
            num = Integer.parseInt(messageRaw);
        } catch (Exception e) {
            return false;
        }

        // Try to remove user message
        try {
            event.getMessage().delete().queue();
        } catch (Exception ignored) {
        }

        if (num <= 0 || choiceList.size() < num) {
            event.getTextChannel().sendMessageEmbeds(
                            new EmbedBuilder()
                                    .setColor(MinecraftColor.RED.getColor())
                                    .setDescription("That is not a valid input. " +
                                            "Please input a number between 1 and " + choiceList.size() + ".")
                                    .setFooter(String.format("Command by %s#%s", user.getName(), user.getDiscriminator()))
                                    .setTimestamp(Instant.now())
                                    .build()
                    ).delay(5, TimeUnit.SECONDS)
                    .flatMap(Message::delete)
                    .queue();
            return false;
        }

        // valid input
        onChosen.accept(choiceList.get(num - 1));
        return true;
    }
}
