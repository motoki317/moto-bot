package update.multipage;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import update.reaction.ReactionResponse;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class MultipageHandler extends ReactionResponse {
    private static final String ARROW_LEFT = "\u2B05";
    private static final String ARROW_RIGHT = "\u27A1";
    private static final String WHITE_CHECK_MARK = "\u2705";
    private static final String X = "\u274C";

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
        this.setOnDestroy(customOnDestroy(message.getJDA().getSelfUser()));

        addPagingReactions(message);
    }

    public MultipageHandler(Message message, long userId, Function<Integer, Message> pages, Supplier<Integer> maxPage) {
        super(message.getIdLong(), userId, (e) -> false);
        this.message = message;
        this.maxPage = maxPage;
        this.pages = pages;
        this.onReaction = customHandler();
        this.setOnDestroy(customOnDestroy(message.getJDA().getSelfUser()));

        addPagingReactions(message);
    }

    private Predicate<MessageReactionAddEvent> customHandler() {
        return (event) -> {
            User author = event.getJDA().getUserById(event.getUserIdLong());
            if (author != null) {
                try {
                    event.getReaction().removeReaction(author).queue();
                } catch (PermissionException ignored) {
                }
            }

            String reactionName = event.getReactionEmote().getName();
            switch (reactionName) {
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

    private Runnable customOnDestroy(User self) {
        return () -> {
            String[] reactions = {ARROW_LEFT, ARROW_RIGHT, WHITE_CHECK_MARK, X};
            for (String reaction : reactions) {
                this.message.removeReaction(reaction, self).queue();
            }
        };
    }

    /**
     * Adds paging reactions to a message.
     * @param message Message to add reactions to.
     */
    private static void addPagingReactions(Message message) {
        String[] reactions = {ARROW_LEFT, ARROW_RIGHT, WHITE_CHECK_MARK, X};
        for (String reaction : reactions) {
            message.addReaction(reaction).queue();
        }
    }
}
