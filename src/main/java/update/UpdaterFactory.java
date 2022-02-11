package update;

import update.button.ButtonClickManager;

// Abstract factory
public interface UpdaterFactory {
    ButtonClickManager getButtonClickManager();
}
