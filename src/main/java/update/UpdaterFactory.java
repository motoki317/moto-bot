package update;

import update.reaction.ReactionManager;
import update.response.ResponseManager;

// Abstract factory
public interface UpdaterFactory {
    ReactionManager getReactionManager();
    ResponseManager getResponseManager();
}
