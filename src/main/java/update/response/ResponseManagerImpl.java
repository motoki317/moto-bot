package update.response;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.*;
import java.util.function.Predicate;

public class ResponseManagerImpl implements ResponseManager {
    private final Map<Long, List<Response>> waitingResponses;

    private final Object lock;

    public ResponseManagerImpl() {
        this.waitingResponses = new HashMap<>();
        this.lock = new Object();
    }

    @Override
    public void addEventListener(long channelId,
                                    long userId,
                                    Predicate<MessageReceivedEvent> onResponse) {
        Response botResponse = new Response(channelId, userId, onResponse);
        addEventListener(botResponse);
    }

    @Override
    public void addEventListener(Response botResponse) {
        synchronized (this.lock) {
            long userId = botResponse.getUserId();
            if (!this.waitingResponses.containsKey(userId)) {
                this.waitingResponses.put(userId, new ArrayList<>());
            }

            this.waitingResponses.get(userId).add(botResponse);
        }
    }

    @Override
    public void handle(MessageReceivedEvent event) {
        long userId = event.getAuthor().getIdLong();

        synchronized (this.lock) {
            if (!this.waitingResponses.containsKey(userId)) {
                return;
            }

            long channelId = event.getChannel().getIdLong();

            List<Response> responses = this.waitingResponses.get(userId);
            for (Iterator<Response> it = responses.iterator(); it.hasNext(); ) {
                Response r = it.next();

                if (r.getChannelId() == channelId) {
                    boolean res = r.handle(event);
                    if (res) {
                        it.remove();
                        r.onDestroy();
                    }
                    break;
                }
            }

            if (responses.isEmpty()) {
                this.waitingResponses.remove(userId);
            }
        }
    }

    @Override
    public void clearUp() {
        long now = System.currentTimeMillis();
        Predicate<Response> removeIf = r -> (now - r.getUpdatedAt()) > r.getMaxLive();

        synchronized (this.lock) {
            this.waitingResponses.values().stream()
                    .flatMap(Collection::stream)
                    .filter(removeIf)
                    .forEach(Response::onDestroy);
            this.waitingResponses.values().forEach(l -> l.removeIf(removeIf));
            this.waitingResponses.entrySet().removeIf(e -> e.getValue().isEmpty());
        }
    }
}
