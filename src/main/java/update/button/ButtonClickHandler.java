package update.button;

import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import update.base.UserResponseListener;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class ButtonClickHandler implements UserResponseListener<ButtonClickEvent> {
    private final long interactionId;
    private final Function<ButtonClickEvent, Boolean> handler;
    private Runnable onDestroy;
    private long updatedAt;

    public ButtonClickHandler(long interactionId, Function<ButtonClickEvent, Boolean> handler, Runnable onDestroy) {
        this.interactionId = interactionId;
        this.handler = handler;
        this.onDestroy = onDestroy;
        this.updatedAt = System.currentTimeMillis();
    }

    @Override
    public boolean handle(ButtonClickEvent event) {
        this.updatedAt = System.currentTimeMillis();
        return this.handler.apply(event);
    }

    @Override
    public void onDestroy() {
        this.onDestroy.run();
    }

    @Override
    public void setOnDestroy(Runnable onDestroy) {
        this.onDestroy = onDestroy;
    }

    @Override
    public long getUpdatedAt() {
        return this.updatedAt;
    }

    @Override
    public long getMaxLive() {
        return TimeUnit.MINUTES.toMillis(10);
    }

    public long getInteractionId() {
        return this.interactionId;
    }
}
