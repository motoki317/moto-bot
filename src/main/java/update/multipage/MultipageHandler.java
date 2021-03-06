package update.multipage;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import update.reaction.ReactionResponse;

import java.util.function.Function;
import java.util.function.Supplier;

public class MultipageHandler extends ReactionResponse {
    private static final String ARROW_LEFT = "\u2B05";
    private static final String ARROW_RIGHT = "\u27A1";
    private static final String WHITE_CHECK_MARK = "\u2705";
    private static final String X = "\u274C";

    private static final String[] reactions = {ARROW_LEFT, ARROW_RIGHT, WHITE_CHECK_MARK, X};

    private final Message message;

    private final Function<Integer, Message> pages;

    private final Supplier<Integer> maxPage;

    private int currentPage;

    public MultipageHandler(Message message, long userId, Function<Integer, Message> pages, Supplier<Integer> maxPage) {
        // intentionally setting all multi page handlers to not be user-private (anyone can press the reaction buttons)
        super(message.getIdLong(), message.getChannel().getIdLong(), userId, false, (e) -> false);
        this.message = message;
        this.maxPage = maxPage;
        this.pages = pages;
        // set custom on reaction handler and on destroy handler
        this.onReaction = this::handlePage;
        this.setOnDestroy(() -> this.deletePagingReactions(message.getJDA().getSelfUser()));

        addPagingReactions(message);
    }

    private boolean handlePage(MessageReactionAddEvent event) {
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

        int mod = this.maxPage.get() + 1;
        this.currentPage = (this.currentPage + mod) % mod;
        this.message.editMessage(
                this.pages.apply(this.currentPage)
        ).queue();
        return false;
    }

    /**
     * Sets page to the given page (0-indexed) and updates the message.
     * @param newPage 0-indexed new page number.
     */
    public void setPageAndUpdate(int newPage) {
        int mod = this.maxPage.get() + 1;
        this.currentPage = ((newPage % mod) + mod) % mod;
        this.message.editMessage(
                this.pages.apply(this.currentPage)
        ).queue();
    }

    /**
     * Adds paging reactions to a message.
     * @param message Message to add reactions to.
     */
    private static void addPagingReactions(Message message) {
        for (String reaction : reactions) {
            message.addReaction(reaction).queue();
        }
    }

    /**
     * Deletes paging reactions from the sent message.
     * @param self Bot user.
     */
    private void deletePagingReactions(User self) {
        for (String reaction : reactions) {
            this.message.removeReaction(reaction, self).queue();
        }
    }
}
