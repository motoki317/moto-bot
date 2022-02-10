package update;

import update.button.ButtonClickManager;
import update.reaction.ReactionManager;
import update.response.ResponseManager;

// Abstract factory
public interface UpdaterFactory {
    ReactionManager getReactionManager();
    ResponseManager getResponseManager();
    ButtonClickManager getButtonClickManager();
}
