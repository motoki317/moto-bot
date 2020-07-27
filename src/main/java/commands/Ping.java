package commands;

import app.Bot;
import commands.base.GenericCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.NotNull;
import utils.BotUtils;
import utils.FormatUtils;

import java.time.Instant;
import java.util.function.Consumer;

public class Ping extends GenericCommand {
    private final Bot bot;

    public Ping(Bot bot) {
        this.bot = bot;
    }

    @NotNull
    @Override
    public String[][] names() {
        return new String[][]{{"ping"}};
    }

    @NotNull
    @Override
    public String syntax() {
        return "ping";
    }

    @NotNull
    @Override
    public String shortHelp() {
        return "Pong!";
    }

    @NotNull
    @Override
    public Message longHelp() {
        return new MessageBuilder(
                "Checks the bot's ping.\n" +
                        "It is preferable that the ping takes no longer than 250 ms, and be stable."
        ).build();
    }

    @Override
    public void process(@NotNull MessageReceivedEvent event, @NotNull String[] args) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setAuthor("Pong!", null, event.getAuthor().getEffectiveAvatarUrl());

        eb.setDescription("Here's some latency data. " +
                "If Discord API and/or message total ping are taking more than 250 ms, they may be considered slow.");

        eb.addField(
                "Discord API Heartbeat",
                String.format("%s ms (Average %s ms)", event.getJDA().getGatewayPing(), this.bot.getManager().getAverageGatewayPing()),
                false
        );

        eb.setFooter("Requested by " + FormatUtils.getUserFullName(event.getAuthor()) +
                ", Current Shard: " + this.bot.getShardId(event.getJDA()));
        eb.setTimestamp(Instant.now());

        long messageCreationTime = BotUtils.getIdCreationTime(event.getMessageIdLong());
        Consumer<Message> callback = onMessageCreate(eb, messageCreationTime, System.currentTimeMillis());
        respond(event, eb.build(), callback);
    }

    private Consumer<Message> onMessageCreate(EmbedBuilder eb, long messageCreationTime, long receivedTime) {
        return message -> {
            long load = receivedTime - messageCreationTime;
            long send = BotUtils.getIdCreationTime(message.getIdLong()) - receivedTime;
            long total = load + send;

            eb.addField(
                    "Message",
                    "Load (Discord → bot)\n" +
                            load + " ms\n" +
                            "Send (bot → Discord)\n" +
                            send + " ms\n" +
                            "Total or Actual Ping (Load + Send)\n" +
                            total + " ms",
                    false
            );

            message.editMessage(eb.build()).queue();
        };
    }
}
