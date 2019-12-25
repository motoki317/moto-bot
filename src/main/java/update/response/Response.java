package update.response;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.function.Predicate;

// Represents bot's response to user's response (input).
class Response {
    private final long channelId;
    private final long userId;

    private final Predicate<MessageReceivedEvent> onResponse;

    Response(long channelId,
                    long userId,
                    Predicate<MessageReceivedEvent> onResponse) {
        this.channelId = channelId;
        this.userId = userId;
        this.onResponse = onResponse;
    }

    // boolean returned by predicate indicates if manager should discard this response object.
    boolean handle(MessageReceivedEvent event) {
        return this.onResponse.test(event);
    }

    long getChannelId() {
        return channelId;
    }

    long getUserId() {
        return userId;
    }
}
