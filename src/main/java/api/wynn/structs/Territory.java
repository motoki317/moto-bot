package api.wynn.structs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Territory {
    private static final ObjectMapper mapper = new ObjectMapper();

    private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private String territory;
    @Nullable
    private String guild;
    private String acquired;
    @Nullable
    private Location location;

    public String getTerritory() {
        return territory;
    }

    @Nullable
    public String getGuild() {
        return guild;
    }

    public String getAcquired() {
        return this.acquired;
    }

    private Date getAcquiredDate() throws ParseException {
        return format.parse(this.acquired);
    }

    @Nullable
    public Location getLocation() {
        return location;
    }

    public static class Location {
        private int startX;
        private int startZ;
        private int endX;
        private int endZ;

        public int getStartX() {
            return startX;
        }

        // 'y' should be 'z' here but wynn's api faults
        @JsonProperty("startY")
        public int getStartZ() {
            return startZ;
        }

        public int getEndX() {
            return endX;
        }

        @JsonProperty("endY")
        public int getEndZ() {
            return endZ;
        }
    }

    @NotNull
    static Territory parse(String body) throws JsonProcessingException {
        return mapper.readValue(body, Territory.class);
    }

    /**
     * Converts this instance to db model instance.
     * @return DB model instance.
     * @throws ParseException if acquired parameter was in an unexpected format.
     */
    @NotNull
    public db.model.territory.Territory convert() throws ParseException {
        if (this.guild == null) {
            throw new RuntimeException("guild is null");
        }
        return new db.model.territory.Territory(
                this.territory,
                this.guild,
                this.getAcquiredDate(),
                null,
                convertLocation(this.location)
        );
    }

    private static db.model.territory.Territory.Location convertLocation(@Nullable Location l) {
        if (l == null) {
            return new db.model.territory.Territory.Location(0, 0, 0, 0);
        }
        return new db.model.territory.Territory.Location(l.startX, l.startZ, l.endX, l.endZ);
    }
}
