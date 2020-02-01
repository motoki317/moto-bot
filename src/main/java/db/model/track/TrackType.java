package db.model.track;

import java.util.*;
import java.util.concurrent.TimeUnit;

public enum TrackType {
    WAR_ALL("War Tracking (All Guilds)", TimeUnit.DAYS.toMillis(30)),
    WAR_SPECIFIC("War Tracking (Specific Guild)", TimeUnit.DAYS.toMillis(90)),
    WAR_PLAYER("War Tracking (Specific Player)", TimeUnit.DAYS.toMillis(90)),

    TERRITORY_ALL("Territory Tracking (All Guilds)", TimeUnit.DAYS.toMillis(30)),
    TERRITORY_SPECIFIC("Territory Tracking (Specific Guild)", TimeUnit.DAYS.toMillis(90)),

    SERVER_START("Server Start Tracking (WC/EU Servers)", TimeUnit.DAYS.toMillis(365)),
    SERVER_CLOSE("Server Close Tracking (WC/EU Servers)", TimeUnit.DAYS.toMillis(365)),

    SERVER_START_ALL("Server Start Tracking (All Servers)", TimeUnit.DAYS.toMillis(365)),
    SERVER_CLOSE_ALL("Server Close Tracking (All Servers)", TimeUnit.DAYS.toMillis(365)),

    GUILD_CREATE("Guild Creation", TimeUnit.DAYS.toMillis(365)),
    GUILD_DELETE("Guild Deletion", TimeUnit.DAYS.toMillis(365));

    private String displayName;
    // default expire time in millis
    private long defaultExpireTime;

    TrackType(String displayName, long defaultExpireTime) {
        this.displayName = displayName;
        this.defaultExpireTime = defaultExpireTime;
    }

    private static List<Set<TrackType>> conflictGroups;

    static {
        conflictGroups = new ArrayList<>();

        Set<TrackType> war = new HashSet<>(Arrays.asList(WAR_ALL, WAR_PLAYER, WAR_SPECIFIC));
        Set<TrackType> territory = new HashSet<>(Arrays.asList(TERRITORY_ALL, TERRITORY_SPECIFIC));
        Set<TrackType> serverStart = new HashSet<>(Arrays.asList(SERVER_START, SERVER_START_ALL));
        Set<TrackType> serverClose = new HashSet<>(Arrays.asList(SERVER_CLOSE, SERVER_CLOSE_ALL));

        conflictGroups.add(war);
        conflictGroups.add(territory);
        conflictGroups.add(serverStart);
        conflictGroups.add(serverClose);
    }

    /**
     * Get display name (e.g. "War Tracking (All Guilds)") for this track type.
     * @return Display name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get default expire time in millis for this track type.
     * @return Time in millis.
     */
    public long getDefaultExpireTime() {
        return defaultExpireTime;
    }

    public Set<TrackType> getConflictTypes() {
        Set<TrackType> conflicts = new HashSet<>();
        conflictGroups.forEach(group -> {
            if (group.contains(this)) {
                conflicts.addAll(group);
            }
        });
        conflicts.remove(this);
        return conflicts;
    }

    @Override
    public String toString() {
        return this.name();
    }
}
