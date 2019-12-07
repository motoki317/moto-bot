package commands;

import app.Bot;
import commands.base.GenericCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import utils.BotUtils;

import java.time.Instant;
import java.util.function.Consumer;

public class Ping extends GenericCommand {
    private final Bot bot;

    public Ping(Bot bot) {
        this.bot = bot;
    }

    @Override
    public String[] names() {
        return new String[]{"ping"};
    }

    @Override
    public void process(MessageReceivedEvent event, String[] args) {
        EmbedBuilder eb = new EmbedBuilder();

        eb.setThumbnail(event.getAuthor().getEffectiveAvatarUrl());
        eb.setTitle("Pong!");

        eb.setDescription("Here's some latency data. " +
                "If discord API and/or message total ping are taking more than 250ms, they may be considered slow.");

        eb.addField(
                "Discord API Heartbeat",
                String.format("%s ms (Average %s ms)", event.getJDA().getGatewayPing(), this.bot.getManager().getAverageGatewayPing()),
                false
        );

        eb.setFooter("Requested by " + BotUtils.getUserFullName(event.getAuthor()) +
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
                    "Load (discord → bot)\n" +
                            load + " ms\n" +
                            "Send (bot → discord)\n" +
                            send + " ms\n" +
                            "Total or Actual Ping (Load + Send)\n" +
                            total + " ms",
                    false
            );

            message.editMessage(eb.build()).queue();
        };
    }
}
