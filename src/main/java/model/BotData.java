package model;

import java.util.ArrayList;
import java.util.List;

public class BotData {
    private List<TrackingChannel> trackingChannels;

    public List<TrackingChannel> getTrackingChannels() {
        return trackingChannels;
    }

    public BotData() {
        this.trackingChannels = new ArrayList<>();
    }

    /**
     * Ensures each field in this instance is not null.
     * If a field was null, initialize them with appropriate initial values.
     */
    public void ensureNonNull() {
        if (this.trackingChannels == null) {
            this.trackingChannels = new ArrayList<>();
        }
    }
}
