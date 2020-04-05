package update.reaction;

import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import update.base.UserResponseManager;

public interface ReactionManager extends UserResponseManager<MessageReactionAddEvent, ReactionResponse> {
    /**
     * Sets page to given page number (0-indexed) of the last message requested by user ID, and in given channel ID.
     * @param userId    User ID.
     * @param channelId Channel ID.
     * @param newPage   New page number (0-indexed).
     * @return {@code true} if success. {@code false} if message not found.
     */
    boolean setPage(long userId, long channelId, int newPage);
}
