package commands.event;

import app.Bot;
import commands.event.message.SentMessage;
import commands.event.message.SentMessageAdapter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import update.multipage.MultipageHandler;
import utils.BotUtils;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public record MessageReceivedEventAdapter(MessageReceivedEvent event, Bot bot) implements CommandEvent {
    @Override
    public Bot getBot() {
        return bot;
    }

    @Override
    public JDA getJDA() {
        return event.getJDA();
    }

    @Override
    public long getCreatedAt() {
        return BotUtils.getIdCreationTime(event.getMessageIdLong());
    }

    @Override
    public String getContentRaw() {
        return event.getMessage().getContentRaw();
    }

    @Override
    public boolean isFromGuild() {
        return event.isFromGuild();
    }

    @Override
    public Guild getGuild() {
        return event.getGuild();
    }

    @Override
    public MessageChannel getChannel() {
        return event.getChannel();
    }

    @Override
    public TextChannel getTextChannel() {
        return event.getTextChannel();
    }

    @Override
    public User getAuthor() {
        return event.getAuthor();
    }

    @Override
    public Member getMember() {
        return event.getMember();
    }

    @Override
    public void acknowledge() {
        event.getChannel().sendTyping().queue();
    }

    @Override
    public void reply(String message) {
        event.getChannel().sendMessage(message).queue();
    }

    @Override
    public void reply(Message message) {
        event.getChannel().sendMessage(message).queue();
    }

    @Override
    public void reply(MessageEmbed embed) {
        event.getChannel().sendMessageEmbeds(embed).queue();
    }

    @Override
    public void reply(String message, Consumer<SentMessage> callback) {
        event.getChannel().sendMessage(message).queue(m -> callback.accept(new SentMessageAdapter(m)));
    }

    @Override
    public void reply(Message message, Consumer<SentMessage> callback) {
        event.getChannel().sendMessage(message).queue(m -> callback.accept(new SentMessageAdapter(m)));
    }

    @Override
    public void reply(MessageEmbed embed, Consumer<SentMessage> callback) {
        event.getChannel().sendMessageEmbeds(embed).queue(m -> callback.accept(new SentMessageAdapter(m)));
    }

    @Override
    public void replyMultiPage(String message, Function<Integer, Message> pages, Supplier<Integer> maxPage) {
        event.getChannel().sendMessage(message).queue(m ->
                bot.getReactionManager().addEventListener(
                        new MultipageHandler(m, event.getAuthor().getIdLong(), pages, maxPage)));
    }

    @Override
    public void replyMultiPage(Message message, Function<Integer, Message> pages, Supplier<Integer> maxPage) {
        event.getChannel().sendMessage(message).queue(m ->
                bot.getReactionManager().addEventListener(
                        new MultipageHandler(m, event.getAuthor().getIdLong(), pages, maxPage)));
    }

    @Override
    public void replyMultiPage(MessageEmbed embed, Function<Integer, Message> pages, Supplier<Integer> maxPage) {
        event.getChannel().sendMessageEmbeds(embed).queue(m ->
                bot.getReactionManager().addEventListener(
                        new MultipageHandler(m, event.getAuthor().getIdLong(), pages, maxPage)));
    }
}
