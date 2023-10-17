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
    private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

    private String territory;
    @Nullable
    private Guild guild;
    private String acquired;
    @Nullable
    private Location location;

    public String getTerritory() {
        return territory;
    }

    @Nullable
    public Guild getGuild() {
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

    public static class Guild {
        private String name;
        private String prefix;

        public String getName() {
            return name;
        }

        public String getPrefix() {
            return prefix;
        }
    }

    public static class Location {
        private int[] start;
        private int[] end;

        public int[] getStart() {
            return start;
        }

        public int[] getEnd() {
            return end;
        }
    }

    @NotNull
    static Territory parse(String territory, String body) throws JsonProcessingException {
        var t = mapper.readValue(body, Territory.class);
        t.territory = territory;
        return t;
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
                this.guild.name,
                this.getAcquiredDate(),
                null,
                convertLocation(this.location)
        );
    }

    private static db.model.territory.Territory.Location convertLocation(@Nullable Location l) {
        if (l == null) {
            return new db.model.territory.Territory.Location(0, 0, 0, 0);
        }
        return new db.model.territory.Territory.Location(l.start[0], l.start[1], l.end[0], l.end[1]);
    }
}
