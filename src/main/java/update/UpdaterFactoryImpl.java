package update;

import update.button.ButtonClickManager;
import update.button.ButtonClickManagerImpl;
import update.response.ResponseManager;
import update.response.ResponseManagerImpl;

public class UpdaterFactoryImpl implements UpdaterFactory {
    @Override
    public ResponseManager getResponseManager() {
        return new ResponseManagerImpl();
    }

    @Override
    public ButtonClickManager getButtonClickManager() {
        return new ButtonClickManagerImpl();
    }
}
