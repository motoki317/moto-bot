package update;

import update.button.ButtonClickManager;
import update.response.ResponseManager;

// Abstract factory
public interface UpdaterFactory {
    ResponseManager getResponseManager();

    ButtonClickManager getButtonClickManager();
}
