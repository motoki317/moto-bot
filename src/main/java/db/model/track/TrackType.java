package db.model.track;

import java.util.*;

public enum TrackType {
    WAR_ALL("War Tracking (All Guilds)"),
    WAR_SPECIFIC("War Tracking (Specific Guild)"),
    WAR_PLAYER("War Tracking (Specific Player)"),

    TERRITORY_ALL("Territory Tracking (All Guilds)"),
    TERRITORY_SPECIFIC("Territory Tracking (Specific Guild)"),

    SERVER_START("Server Start Tracking (WC/EU Servers)"),
    SERVER_CLOSE("Server Close Tracking (WC/EU Servers)"),

    SERVER_START_ALL("Server Start Tracking (All Servers)"),
    SERVER_CLOSE_ALL("Server Close Tracking (All Servers)"),

    GUILD_CREATE("Guild Creation"),
    GUILD_DELETE("Guild Deletion");

    private String displayName;

    TrackType(String displayName) {
        this.displayName = displayName;
    }

    private static List<Set<TrackType>> conflictGroups;

    static {
        conflictGroups = new ArrayList<>();

        Set<TrackType> war = new HashSet<>(Arrays.asList(WAR_ALL, WAR_PLAYER, WAR_SPECIFIC));
        Set<TrackType> territory = new HashSet<>(Arrays.asList(TERRITORY_ALL, TERRITORY_SPECIFIC));
        Set<TrackType> serverStart = new HashSet<>(Arrays.asList(SERVER_START, SERVER_START_ALL));
        Set<TrackType> serverClose = new HashSet<>(Arrays.asList(SERVER_CLOSE, SERVER_CLOSE_ALL));
        Set<TrackType> guild = new HashSet<>(Arrays.asList(GUILD_CREATE, GUILD_DELETE));

        conflictGroups.add(war);
        conflictGroups.add(territory);
        conflictGroups.add(serverStart);
        conflictGroups.add(serverClose);
        conflictGroups.add(guild);
    }

    public String getDisplayName() {
        return displayName;
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
