package api.wynn.structs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Nullable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WynnGuild {
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS");

    private static final String[] ranks = new String[]{
            "owner",
            "chief",
            "strategist",
            "captain",
            "recruiter",
            "recruit",
    };

    private final String name;
    private final String prefix;

    private final int level;
    private final int xpPercent;

    private final int territories;
    private final int wars;

    private final Date created;

    private final List<Member> members;

    private final int online;

    private final Banner banner;

    // seasonRanks;

    private final Date requestedAt;

    public WynnGuild(String body) throws JsonProcessingException, ParseException {
        var json = mapper.readTree(body);

        this.name = json.get("name").asText();
        this.prefix = json.get("prefix").asText();
        this.level = json.get("level").asInt();
        this.xpPercent = json.get("xpPercent").asInt();
        this.territories = json.get("territories").asInt();
        this.wars = json.get("wars").asInt();
        this.created = format.parse(json.get("created").asText());

        this.members = new ArrayList<>();
        var members = json.get("members");
        // int total = members.get("total").asInt();
        for (String rank : ranks) {
            var rankMembers = members.get(rank);
            for (var fields = rankMembers.fields(); fields.hasNext(); ) {
                var f = fields.next();
                var name = f.getKey();
                var p = f.getValue();
                var member = new Member(
                        rank,
                        name,
                        p.get("uuid").asText(),
                        p.get("online").asBoolean(),
                        p.get("server").asText(),
                        p.get("contributed").asLong(),
                        // p.get("contributionRank").asInt(),
                        format.parse(p.get("joined").asText())
                );
                this.members.add(member);
            }
        }

        this.online = json.get("online").asInt();

        this.banner = mapper.readValue(json.get("banner").toString(), Banner.class);

        this.requestedAt = new Date();
    }

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getLevel() {
        return level;
    }

    public int getXpPercent() {
        return xpPercent;
    }

    public int getTerritories() {
        return territories;
    }

    public int getWars() {
        return wars;
    }

    public Date getCreated() {
        return created;
    }

    public List<Member> getMembers() {
        return members;
    }

    public int getOnline() {
        return online;
    }

    public Banner getBanner() {
        return banner;
    }

    public Date getRequestedAt() {
        return requestedAt;
    }

    @Nullable
    public String getOwnerName() {
        return this.members.stream().filter(m -> "owner".equals(m.rank))
                .map(m -> m.name).findFirst().orElse(null);
    }

    public record Member(String rank,
                         String name,
                         String uuid,
                         boolean online,
                         @Nullable String server,
                         long contributed,
                         // int contributionRank,
                         Date joined) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Banner {
        private String base;
        private int tier;
        private List<Layer> layers;

        public String getBase() {
            return base;
        }

        public int getTier() {
            return tier;
        }

        public List<Layer> getLayers() {
            return layers;
        }

        private static class Layer {
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
