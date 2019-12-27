package db.model.territory;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Date;

public class Territory implements TerritoryId {
    @NotNull
    private String name;
    private String guild;
    private Date acquired;
    @Nullable
    private String attacker;
    private Location location;

    public Territory(@NotNull String name, String guild, Date acquired, @Nullable String attacker, @NotNull Location location) {
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
        private int startX;
        private int startZ;
        private int endX;
        private int endZ;

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
