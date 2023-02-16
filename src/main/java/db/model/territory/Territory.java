package db.model.territory;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Date;

public class Territory implements TerritoryId {
    @NotNull
    private final String name;
    @NotNull
    private final String guild;
    private final Date acquired;
    @Nullable
    private final String attacker; // TODO: delete
    private final Location location;

    public Territory(@NotNull String name, @NotNull String guild, Date acquired, @Nullable String attacker, @NotNull Location location) {
        this.name = name;
        this.guild = guild;
        this.acquired = acquired;
        this.attacker = attacker;
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public static class Location {
        private final int startX;
        private final int startZ;
        private final int endX;
        private final int endZ;

        public Location(int startX, int startZ, int endX, int endZ) {
            this.startX = startX;
            this.startZ = startZ;
            this.endX = endX;
            this.endZ = endZ;
        }

        public int getStartX() {
            return startX;
        }

        public int getStartZ() {
            return startZ;
        }

        public int getEndX() {
            return endX;
        }

        public int getEndZ() {
            return endZ;
        }
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    public String getGuild() {
        return guild;
    }

    public Date getAcquired() {
        return acquired;
    }

    @Nullable
    public String getAttacker() {
        return attacker;
    }
}
