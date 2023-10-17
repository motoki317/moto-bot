package api.wynn.structs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WynnGuildLeaderboard {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final DateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

    private final List<Guild> data;

    public WynnGuildLeaderboard(String body) throws JsonProcessingException {
        this.data = new ArrayList<>();

        var json = mapper.readTree(body);
        for (var i = json.fields(); i.hasNext(); ) {
            var entry = i.next();
            var value = entry.getValue();
            this.data.add(mapper.readValue(value.toString(), Guild.class));
        }
    }

    public List<Guild> getData() {
        return data;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Guild {
        private String name;
        private String prefix;
        private long xp;
        private int territories;
        private int wars;
        private int level;
        private int members;
        private String created;

        // private long xp;
        @Nullable
        private Banner banner;

        public String getName() {
            return name;
        }

        public String getPrefix() {
            return prefix;
        }

        public long getXp() {
            return xp;
        }

        public int getLevel() {
            return level;
        }

        public String getCreated() {
            return created;
        }

        @Nullable
        @JsonIgnore
        public Date getCreatedDate() {
            try {
                return apiFormat.parse(this.created);
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Nullable
        public Banner getBanner() {
            return banner;
        }

        public int getTerritories() {
            return territories;
        }

        public int getMembers() {
            return members;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Banner {
            private String base;
            private int tier;
            private String structure;
            private List<Layer> layers;

            public String getBase() {
                return base;
            }

            public int getTier() {
                return tier;
            }

            public String getStructure() {
                return structure;
            }

            public List<Layer> getLayers() {
                return layers;
            }

            public static class Layer {
                private String colour;
                private String pattern;

                public String getColour() {
                    return colour;
                }

                public String getPattern() {
                    return pattern;
                }
            }
        }
    }
}
