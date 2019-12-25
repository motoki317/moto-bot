package update.multipage;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import update.reaction.ReactionResponse;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class MultipageHandler extends ReactionResponse {
    private static final char ARROW_LEFT = '\u2B05';
    private static final char ARROW_RIGHT = '\u27A1';
    private static final char WHITE_CHECK_MARK = '\u2705';
    private static final char X = '\u274C';

    private final Message message;

    private final Function<Integer, Message> pages;

    private final Supplier<Integer> maxPage;

    private int currentPage;

    public MultipageHandler(Message message, Function<Integer, Message> pages, Supplier<Integer> maxPage) {
        super(message.getIdLong(), (e) -> false);
        this.message = message;
        this.maxPage = maxPage;
        this.pages = pages;
        this.onReaction = customHandler();
        this.setOnDestroy(customOnDestroy());
    }

    public MultipageHandler(Message message, long userId, Function<Integer, Message> pages, Supplier<Integer> maxPage) {
        super(message.getIdLong(), userId, (e) -> false);
        this.message = message;
        this.maxPage = maxPage;
        this.pages = pages;
        this.onReaction = customHandler();
        this.setOnDestroy(customOnDestroy());
    }

    private Predicate<MessageReactionAddEvent> customHandler() {
        return (event) -> {
            event.getReaction().removeReaction().queue();

            String reactionName = event.getReactionEmote().getName();
            if (reactionName.length() == 0) {
                return false;
            }

            switch (reactionName.charAt(0)) {
                case ARROW_LEFT:
                    this.currentPage--;
                    break;
                case ARROW_RIGHT:
                    this.currentPage++;
                    break;
                case WHITE_CHECK_MARK:
                    break;
                case X:
                    return true;
                default:
                    return false;
            }

            int max = this.maxPage.get();
            this.currentPage = (this.currentPage + max) % max;

            this.message.editMessage(
                    this.pages.apply(this.currentPage)
            ).queue();
            return false;
        };
    }

    private Runnable customOnDestroy() {
        return () -> {
            List<MessageReaction> reactions = this.message.getReactions();
            for (MessageReaction reaction : reactions) {
                if (reaction.isSelf()) {
                    reaction.removeReaction().queue();
                }
            }
        };
    }
}
