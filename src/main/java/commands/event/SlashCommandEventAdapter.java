package commands.event;

import app.Bot;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import update.multipage.ButtonMultiPageHandler;
import utils.BotUtils;

import javax.annotation.Nullable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class SlashCommandEventAdapter implements CommandEvent {
    private final SlashCommandEvent event;
    private final Bot bot;
    @Nullable
    private InteractionHook hook;

    public SlashCommandEventAdapter(SlashCommandEvent event, Bot bot) {
        this.event = event;
        this.bot = bot;
        this.hook = null;
    }

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
    public TextChannel getTextChannel() {
        return event.getTextChannel();
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
        this.hook = event.deferReply().complete();
    }

    @Override
    public void reply(String message) {
        if (hook != null) {
            hook.editOriginal(message).queue();
        } else {
            event.reply(message).queue();
        }
    }

    @Override
    public void reply(Message message) {
        if (hook != null) {
            hook.editOriginal(message).queue();
        } else {
            event.reply(message).queue();
        }
    }

    @Override
    public void reply(MessageEmbed embed) {
        if (hook != null) {
            hook.editOriginalEmbeds(embed).queue();
        } else {
            event.replyEmbeds(embed).queue();
        }
    }

    @Override
    public void reply(String message, Consumer<SentMessage> callback) {
        if (hook != null) {
            hook.editOriginal(message).queue(m -> callback.accept(new InteractionHookAdapter(this.hook)));
        } else {
            event.reply(message).queue(h -> callback.accept(new InteractionHookAdapter(h)));
        }
    }

    @Override
    public void reply(Message message, Consumer<SentMessage> callback) {
        if (hook != null) {
            hook.editOriginal(message).queue(m -> callback.accept(new InteractionHookAdapter(this.hook)));
        } else {
            event.reply(message).queue(h -> callback.accept(new InteractionHookAdapter(h)));
        }
    }

    @Override
    public void reply(MessageEmbed embed, Consumer<SentMessage> callback) {
        if (hook != null) {
            hook.editOriginalEmbeds(embed).queue(m -> callback.accept(new InteractionHookAdapter(this.hook)));
        } else {
            event.replyEmbeds(embed).queue(h -> callback.accept(new InteractionHookAdapter(h)));
        }
    }

    @Override
    public void replyMultiPage(String message, Function<Integer, Message> pages, Supplier<Integer> maxPage) {
        if (hook != null) {
            hook.editOriginal(message)
                    .setActionRow(ButtonMultiPageHandler.getActionRow())
                    .queue(m ->
                            bot.getButtonClickManager().addEventListener(
                                    new ButtonMultiPageHandler(this.hook, pages, maxPage)));
        } else {
            event.reply(message)
                    .addActionRow(ButtonMultiPageHandler.getActionRow())
                    .queue(s ->
                            bot.getButtonClickManager().addEventListener(
                                    new ButtonMultiPageHandler(s, pages, maxPage)));
        }
    }

    @Override
    public void replyMultiPage(Message message, Function<Integer, Message> pages, Supplier<Integer> maxPage) {
        if (hook != null) {
            hook.editOriginal(message)
                    .setActionRow(ButtonMultiPageHandler.getActionRow())
                    .queue(m ->
                            bot.getButtonClickManager().addEventListener(
                                    new ButtonMultiPageHandler(this.hook, pages, maxPage)));
        } else {
            event.reply(message)
                    .addActionRow(ButtonMultiPageHandler.getActionRow())
                    .queue(s ->
                            bot.getButtonClickManager().addEventListener(
                                    new ButtonMultiPageHandler(s, pages, maxPage)));
        }
    }

    @Override
    public void replyMultiPage(MessageEmbed embed, Function<Integer, Message> pages, Supplier<Integer> maxPage) {
        if (hook != null) {
            hook.editOriginalEmbeds(embed)
                    .setActionRow(ButtonMultiPageHandler.getActionRow())
                    .queue(m ->
                            bot.getButtonClickManager().addEventListener(
                                    new ButtonMultiPageHandler(this.hook, pages, maxPage)));
        } else {
            event.replyEmbeds(embed)
                    .addActionRow(ButtonMultiPageHandler.getActionRow())
                    .queue(s ->
                            bot.getButtonClickManager().addEventListener(
                                    new ButtonMultiPageHandler(s, pages, maxPage)));
        }
    }
}
