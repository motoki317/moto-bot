package api.structs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class Territory {
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private TimeZone wynnTimeZone;

    private String territory;
    private String guild;
    private String acquired;
    private String attacker;
    private Location location;

    public String getTerritory() {
        return territory;
    }

    public String getGuild() {
        return guild;
    }

    public Date getAcquired() throws ParseException {
        return format.parse(this.acquired);
    }

    @Nullable
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

    @NotNull
    static Territory parse(String body, TimeZone wynnTimeZone) throws JsonProcessingException {
        Territory instance = mapper.readValue(body, Territory.class);
        instance.wynnTimeZone = wynnTimeZone;
        return instance;
    }
}
