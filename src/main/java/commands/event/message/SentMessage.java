package commands.event.message;

import app.Bot;
import commands.event.CommandEvent;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.ComponentLayout;
import update.multipage.ButtonMultiPageHandler;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public interface SentMessage {
    void getMessage(Consumer<Message> callback);

    void editMessage(String message);

    void editMessage(Message message);

    void editMessage(MessageEmbed embed);

    void editMessage(String message, Consumer<SentMessage> callback);

    void editMessage(Message message, Consumer<SentMessage> callback);

    void editMessage(MessageEmbed embed, Consumer<SentMessage> callback);

    void editComponents(ComponentLayout... layouts);

    void delete();

    void deleteAfter(long timeout, TimeUnit unit);

    default void editException(CharSequence message) {
        editMessage(CommandEvent.buildException(message));
    }

    default void editError(User author, String message) {
        editMessage(CommandEvent.buildError(author, message));
    }

    default void editMultiPage(Bot bot, Function<Integer, Message> pages, Supplier<Integer> maxPage) {
        editMessage(
                new MessageBuilder(pages.apply(0))
                        .setActionRows(ActionRow.of(ButtonMultiPageHandler.getActionRow())).build(),
                next ->
                        next.getMessage(m ->
                                bot.getButtonClickManager().addEventListener(
                                        new ButtonMultiPageHandler(new SentMessageAdapter(m), m.getIdLong(), pages, maxPage))));
    }
}
