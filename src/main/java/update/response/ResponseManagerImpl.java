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
    public void waitForUserResponse(long channelId,
                                    long userId,
                                    Predicate<MessageReceivedEvent> onResponse) {
        Response botResponse = new Response(channelId, userId, onResponse);
        waitForUserResponse(botResponse);
    }

    @Override
    public void waitForUserResponse(Response botResponse) {
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
                    }
                    break;
                }
            }

            if (responses.isEmpty()) {
                this.waitingResponses.remove(userId);
            }
        }
    }
}
