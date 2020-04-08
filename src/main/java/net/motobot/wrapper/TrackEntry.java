package net.motobot.wrapper;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class TrackEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    public enum TrackType {
        TERRITORY_ALL("Territory Tracking (All)"), // Normal Territory Tracker
        TERRITORY_SPECIFIC("Territory Tracking"), // + Guild Specific
        WAR_ALL("War Tracking (All)"), // Normal War Tracker
        WAR_SPECIFIC("War Tracking"), // + Guild Specific
        WAR_PLAYER("Player War Tracking"), // Player Specific War Tracker (Send both war/territory take messages?)
        SERVER_START("Server Start Tracking"), // Server Uptime Tracker
        SERVER_CLOSE("Server Close Tracking"),
        SERVER_START_ALL("Server Start Tracking (All)"),
        SERVER_CLOSE_ALL("Server Close Tracking (ALl");

        String displayName;
        Set<TrackType> conflictTypes;

        TrackType(String displayName) {
            this.displayName = displayName;
            this.conflictTypes = new HashSet<>();
        }

        static {
            TERRITORY_ALL.conflictTypes.add(TERRITORY_SPECIFIC);
            TERRITORY_SPECIFIC.conflictTypes.add(TERRITORY_ALL);
            WAR_ALL.conflictTypes.add(WAR_SPECIFIC);
            WAR_ALL.conflictTypes.add(WAR_PLAYER);
            WAR_SPECIFIC.conflictTypes.add(WAR_ALL);
            WAR_SPECIFIC.conflictTypes.add(WAR_PLAYER);
            WAR_PLAYER.conflictTypes.add(WAR_ALL);
            WAR_PLAYER.conflictTypes.add(WAR_SPECIFIC);

            SERVER_START.conflictTypes.add(SERVER_START_ALL);
            SERVER_START_ALL.conflictTypes.add(SERVER_START);
            SERVER_CLOSE.conflictTypes.add(SERVER_CLOSE_ALL);
            SERVER_CLOSE_ALL.conflictTypes.add(SERVER_CLOSE);
        }
    }

    private TrackType type;
    private long channelId;
    private String guildName;
    private String playerName;

    public TrackEntry(TrackType type, long channelId, String guildName, String playerName) {
        this.type = type;
        this.channelId = channelId;
        this.guildName = guildName;
        this.playerName = playerName;
    }

    public String getDisplayName() {
        StringBuilder msg = new StringBuilder(type.displayName);
        switch (type) {
            case TERRITORY_SPECIFIC:
            case WAR_SPECIFIC:
                msg.append(" for guild ").append(guildName);
                break;
            case WAR_PLAYER:
                msg.append(" for player ").append(playerName);
                break;
        }

        return msg.toString();
    }

    public TrackType getType() {
        return type;
    }

    public long getChannelId() {
        return channelId;
    }

    public String getGuildName() {
        return guildName;
    }

    public String getPlayerName() {
        return playerName;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TrackEntry
                && ((TrackEntry) obj).type == this.type
                && ((TrackEntry) obj).channelId == this.channelId
                && (((TrackEntry) obj).guildName != null ? ((TrackEntry) obj).guildName.equals(this.guildName) : this.guildName == null)
                && (((TrackEntry) obj).playerName != null ? ((TrackEntry) obj).playerName.equals(this.playerName) : this.playerName == null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, channelId, guildName, playerName);
    }

    @Override
    public String toString() {
        return "Track Entry | Type: " + type.name() + ", Channel: " + channelId +
                (guildName != null ? ", Guild: " + guildName : "") +
                (playerName != null ? ", Player: " + playerName : "");
    }
}
