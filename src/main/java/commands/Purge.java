package commands;

import commands.base.GuildCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import utils.MinecraftColor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Purge extends GuildCommand {
    @NotNull
    @Override
    protected String[][] names() {
        return new String[][]{{"purge"}};
    }

    @Override
    public @NotNull String syntax() {
        return "purge <num>";
    }

    @Override
    public @NotNull String shortHelp() {
        return "Deletes specified number of the most recent messages in the channel.";
    }

    @Override
    public @NotNull Message longHelp() {
        return new MessageBuilder(
                new EmbedBuilder()
                .setAuthor("Purge Command Help")
                .setDescription(String.join("\n",
                        this.shortHelp(),
                        "Specify a number between 1 and 100."
                ))
                .addField("Syntax", this.syntax(), false)
                .build()
        ).build();
    }

    @NotNull
    @Override
    protected Permission[] getRequiredPermissions() {
        return new Permission[]{Permission.MESSAGE_MANAGE};
    }

    @Override
    public long getCoolDown() {
        return TimeUnit.SECONDS.toMillis(10);
    }

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        if (args.length <= 1) {
            respond(event, "Please specify how many messages to purge!");
        }

        long messageId = event.getMessageIdLong();
        int limit;
        try {
            limit = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            respond(event, "Please specify a valid number of messages to purge!");
            return;
        }

        if (limit < 1 || 100 < limit) {
            respond(event, "Please specify a number between 1 and 100!");
            return;
        }

        try {
            event.getMessage().delete().complete();
        } catch (Exception e) {
            respondError(event, "An error occurred while deleting your command message. " +
                    "Please make sure the bot has correct permission to delete channel messages: " + e.getMessage());
            return;
        }

        MessageChannel channel = event.getChannel();
        channel.getHistoryBefore(messageId, limit).queue(history -> {
            try {
                CompletableFuture.allOf(
                        channel.purgeMessages(history.getRetrievedHistory()).toArray(new CompletableFuture[]{})
                ).get();
                respond(event, new MessageBuilder(
                        new EmbedBuilder()
                        .setColor(MinecraftColor.DARK_GREEN.getColor())
                        .setDescription(String.format("Successfully deleted %s message(s)!", limit))
                        .build()
                ).build(), message -> message.delete().queueAfter(3, TimeUnit.SECONDS));
            } catch (Exception e) {
                respondError(event, "An error occurred while purging messages. " +
                        "Please make sure the bot has correct permission to delete channel messages: " + e.getMessage());
            }
        }, failure -> respondError(event,
                String.format("An error occurred while retrieving history: %s", failure.getMessage()))
        );
    }
}
