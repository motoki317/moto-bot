package commands.event;

import app.Bot;
import commands.event.message.InteractionHookAdapter;
import commands.event.message.SentMessage;
import commands.event.message.SentMessageAdapter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
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
        // https://github.com/DV8FromTheWorld/JDA/blob/0de1348e241cddf50c9ec5b8751b8235989d774e/src/main/java/net/dv8tion/jda/api/interactions/commands/CommandInteraction.java#L216
        StringBuilder builder = new StringBuilder();
        builder.append("/").append(event.getName());
        if (event.getSubcommandGroup() != null)
            builder.append(" ").append(event.getSubcommandGroup());
        if (event.getSubcommandName() != null)
            builder.append(" ").append(event.getSubcommandName());
        for (OptionMapping o : event.getOptions()) {
            builder.append(" ");
            switch (o.getType()) {
                case CHANNEL:
                    builder.append("#").append(o.getAsGuildChannel().getName());
                    break;
                case USER:
                    builder.append("@").append(o.getAsUser().getName());
                    break;
                case ROLE:
                    builder.append("@").append(o.getAsRole().getName());
                    break;
                case MENTIONABLE: //client only allows user or role mentionable as of Aug 4, 2021
                    if (o.getAsMentionable() instanceof Role)
                        builder.append("@").append(o.getAsRole().getName());
                    else if (o.getAsMentionable() instanceof Member)
                        builder.append("@").append(o.getAsUser().getName());
                    else if (o.getAsMentionable() instanceof User)
                        builder.append("@").append(o.getAsUser().getName());
                    else
                        builder.append("@").append(o.getAsMentionable().getIdLong());
                    break;
                default:
                    builder.append(o.getAsString());
                    break;
            }
        }
        return builder.toString();
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
                                    new ButtonMultiPageHandler(new SentMessageAdapter(m), m.getIdLong(), pages, maxPage)));
        } else {
            event.reply(message)
                    .addActionRow(ButtonMultiPageHandler.getActionRow())
                    .queue(s ->
                            s.retrieveOriginal().queue(m ->
                                    bot.getButtonClickManager().addEventListener(
                                            new ButtonMultiPageHandler(new SentMessageAdapter(m), m.getIdLong(), pages, maxPage))));
        }
    }

    @Override
    public void replyMultiPage(Message message, Function<Integer, Message> pages, Supplier<Integer> maxPage) {
        if (hook != null) {
            hook.editOriginal(message)
                    .setActionRow(ButtonMultiPageHandler.getActionRow())
                    .queue(m ->
                            bot.getButtonClickManager().addEventListener(
                                    new ButtonMultiPageHandler(new SentMessageAdapter(m), m.getIdLong(), pages, maxPage)));
        } else {
            event.reply(message)
                    .addActionRow(ButtonMultiPageHandler.getActionRow())
                    .queue(s ->
                            s.retrieveOriginal().queue(m ->
                                    bot.getButtonClickManager().addEventListener(
                                            new ButtonMultiPageHandler(new SentMessageAdapter(m), m.getIdLong(), pages, maxPage))));
        }
    }

    @Override
    public void replyMultiPage(MessageEmbed embed, Function<Integer, Message> pages, Supplier<Integer> maxPage) {
        if (hook != null) {
            hook.editOriginalEmbeds(embed)
                    .setActionRow(ButtonMultiPageHandler.getActionRow())
                    .queue(m ->
                            bot.getButtonClickManager().addEventListener(
                                    new ButtonMultiPageHandler(new SentMessageAdapter(m), m.getIdLong(), pages, maxPage)));
        } else {
            event.replyEmbeds(embed)
                    .addActionRow(ButtonMultiPageHandler.getActionRow())
                    .queue(s ->
                            s.retrieveOriginal().queue(m ->
                                    bot.getButtonClickManager().addEventListener(
                                            new ButtonMultiPageHandler(new SentMessageAdapter(m), m.getIdLong(), pages, maxPage))));
        }
    }
}
