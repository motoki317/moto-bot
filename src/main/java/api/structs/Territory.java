package api.structs;

import java.util.Date;

public class Territory {
    private String territory;
    private String guild;
    private Date acquired;
    private String attacker;
    private Location location;

    public String getTerritory() {
        return territory;
    }

    public String getGuild() {
        return guild;
    }

    public Date getAcquired() {
        return acquired;
    }

    public String getAttacker() {
        return attacker;
    }

    public Location getLocation() {
        return location;
    }

    private static class Location {
        private int startX;
        private int startY;
        private int endX;
        private int endY;

        public int getStartX() {
            return startX;
        }

        public int getStartY() {
            return startY;
        }

        public int getEndX() {
            return endX;
        }

        public int getEndY() {
            return endY;
        }
    }
}
