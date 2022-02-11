package update.button;

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import update.base.UserResponseManager;

public interface ButtonClickManager extends UserResponseManager<ButtonClickEvent, ButtonClickHandler> {
    /**
     * Sets page to given page number (0-indexed) of the last message requested in the channel.
     * @param channelId Channel ID.
     * @param newPage   New page number (0-indexed).
     * @return {@code true} if success. {@code false} if message not found.
     */
    boolean setPage(long channelId, int newPage);
}
