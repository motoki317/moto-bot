package model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BotData {
    @JsonIgnore
    private final Object trackingChannelsLock = new Object();
    private List<TrackingChannel> trackingChannels;

    public void addTrackingChannel(TrackingChannel trackingChannel) {
        synchronized (this.trackingChannelsLock) {
            this.trackingChannels.add(trackingChannel);
        }
    }

    public Set<TrackingChannel> getTrackingChannelsByType(TrackingType type) {
        synchronized (this.trackingChannelsLock) {
           return this.trackingChannels.stream().filter(ch -> ch.getType() == type).collect(Collectors.toSet());
        }
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
