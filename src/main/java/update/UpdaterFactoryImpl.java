package update;

import update.button.ButtonClickManager;
import update.button.ButtonClickManagerImpl;

public class UpdaterFactoryImpl implements UpdaterFactory {
    @Override
    public ButtonClickManager getButtonClickManager() {
        return new ButtonClickManagerImpl();
    }
}
