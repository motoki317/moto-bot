package commands.event;

import app.Bot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import update.multipage.ButtonMultiPageHandler;
import utils.BotUtils;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public record SlashCommandEventAdapter(SlashCommandEvent event, Bot bot) implements CommandEvent {
    @Override
    public JDA getJDA() {
        return event.getJDA();
    }

    @Override
    public long getCreatedAt() {
        return BotUtils.getIdCreationTime(event.getIdLong());
    }

    @Override
    public String getContentRaw() {
        return event.getCommandString();
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
    public User getAuthor() {
        return event.getUser();
    }

    @Override
    public Member getMember() {
        return event.getMember();
    }

    @Override
    public void acknowledge() {
        event.deferReply().queue();
    }

    @Override
    public void reply(String message) {
        event.reply(message).queue();
    }

    @Override
    public void reply(Message message) {
        event.reply(message).queue();
    }

    @Override
    public void reply(MessageEmbed embed) {
        event.replyEmbeds(embed).queue();
    }

    @Override
    public void reply(String message, Consumer<SentMessage> callback) {
        event.reply(message).queue(h -> callback.accept(new InteractionHookAdapter(h)));
    }

    @Override
    public void reply(Message message, Consumer<SentMessage> callback) {
        event.reply(message).queue(h -> callback.accept(new InteractionHookAdapter(h)));
    }

    @Override
    public void reply(MessageEmbed embed, Consumer<SentMessage> callback) {
        event.replyEmbeds(embed).queue(h -> callback.accept(new InteractionHookAdapter(h)));
    }

    @Override
    public void replyMultiPage(String message, Function<Integer, Message> pages, Supplier<Integer> maxPage) {
        event.reply(message)
                .addActionRow(ButtonMultiPageHandler.getActionRow())
                .queue(s ->
                        bot.getButtonClickManager().addEventListener(
                                new ButtonMultiPageHandler(s, pages, maxPage)));
    }

    @Override
    public void replyMultiPage(Message message, Function<Integer, Message> pages, Supplier<Integer> maxPage) {
        event.reply(message)
                .addActionRow(ButtonMultiPageHandler.getActionRow())
                .queue(s ->
                        bot.getButtonClickManager().addEventListener(
                                new ButtonMultiPageHandler(s, pages, maxPage)));
    }

    @Override
    public void replyMultiPage(MessageEmbed embed, Function<Integer, Message> pages, Supplier<Integer> maxPage) {
        event.replyEmbeds(embed)
                .addActionRow(ButtonMultiPageHandler.getActionRow())
                .queue(s ->
                        bot.getButtonClickManager().addEventListener(
                                new ButtonMultiPageHandler(s, pages, maxPage)));
    }
}
