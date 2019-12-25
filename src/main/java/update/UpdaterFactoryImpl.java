package update;

import update.reaction.ReactionManager;
import update.reaction.ReactionManagerImpl;
import update.response.ResponseManager;
import update.response.ResponseManagerImpl;

public class UpdaterFactoryImpl implements UpdaterFactory {
    @Override
    public ReactionManager getReactionManager() {
        return new ReactionManagerImpl();
    }

    @Override
    public ResponseManager getResponseManager() {
        return new ResponseManagerImpl();
    }
}
