package update.button;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import update.multipage.ButtonMultiPageHandler;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class ButtonClickManagerImpl implements ButtonClickManager {
    private final Map<Long, ButtonClickHandler> waitingResponses;

    private final Object lock;

    public ButtonClickManagerImpl() {
        this.waitingResponses = new HashMap<>();
        this.lock = new Object();

        long delay = TimeUnit.MINUTES.toMillis(10);
        ButtonClickManagerImpl manager = this;
        new Timer().scheduleAtFixedRate(
                new TimerTask() {
                    @Override
                    public void run() {
                        manager.clearUp();
                    }
                },
                delay,
                delay
        );
    }

    @Override
    public void addEventListener(ButtonClickHandler r) {
        synchronized (this.lock) {
            this.waitingResponses.put(r.getMessageId(), r);
        }
    }

    @Override
    public void handle(ButtonClickEvent event) {
        long messageId = event.getMessage().getIdLong();

        synchronized (this.lock) {
            if (!this.waitingResponses.containsKey(messageId)) {
                return;
            }

            boolean remove = this.waitingResponses.get(messageId).handle(event);
            if (remove) {
                this.waitingResponses.get(messageId).onDestroy();
                this.waitingResponses.remove(messageId);
            }
        }
    }

    private void clearUp() {
        long now = System.currentTimeMillis();
        Predicate<ButtonClickHandler> removeIf = r -> (now - r.getUpdatedAt()) > r.getMaxLive();

        synchronized (this.lock) {
            this.waitingResponses.values().stream()
                    .filter(removeIf)
                    .forEach(ButtonClickHandler::onDestroy);
            this.waitingResponses.values().removeIf(removeIf);
        }
    }

    @Override
    public boolean setPage(long channelId, int newPage) {
        // ???
        ButtonMultiPageHandler handler = this.waitingResponses.values().stream()
                .filter(h -> h instanceof ButtonMultiPageHandler)
                .map(h -> (ButtonMultiPageHandler) h)
                .map(h -> {
                    CompletableFuture<Message> c = new CompletableFuture<>();
                    h.getMessage(c::complete);
                    return new AbstractMap.SimpleEntry<>(h, c);
                })
                .filter(p -> {
                    Message m;
                    try {
                        m = p.getValue().get();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        return false;
                    }
                    return m.getChannel().getIdLong() == channelId;
                })
                .max(Comparator.comparingLong(p -> {
                    try {
                        return p.getValue().get().getIdLong();
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                        return 0L;
                    }
                }))
                .map(AbstractMap.SimpleEntry::getKey)
                .orElse(null);

        if (handler == null) {
            return false;
        }

        handler.setPageAndUpdate(newPage);
        return true;
    }
}
