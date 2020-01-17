package api.wynn.structs;

import api.wynn.structs.common.Request;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class WynnGuild {
    private String name;
    private String prefix;

    private List<Member> members;

    private String xp;
    private int level;

    private Date created;
    private String createdFriendly;

    private int territories;

    private Banner banner;

    private Request request;

    public String getName() {
        return name;
    }

    public String getPrefix() {
        return prefix;
    }

    public List<Member> getMembers() {
        return members;
    }

    public String getXp() {
        return xp;
    }

    public int getLevel() {
        return level;
    }

    public Date getCreated() {
        return created;
    }

    public String getCreatedFriendly() {
        return createdFriendly;
    }

    public int getTerritories() {
        return territories;
    }

    public Banner getBanner() {
        return banner;
    }

    public Request getRequest() {
        return request;
    }

    @Nullable
    public String getOwnerName() {
        return this.members.stream().filter(m -> "OWNER".equals(m.rank))
                .map(m -> m.name).findFirst().orElse(null);
    }

    public static class Member {
        private static final DateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        private String name;
        private String uuid;
        private String rank;
        private long contributed;
        private Date joined;
        private String joinedFriendly;

        @JsonCreator
        public Member(@JsonProperty("name") String name,
                      @JsonProperty("uuid") String uuid,
                      @JsonProperty("rank") String rank,
                      @JsonProperty("contributed") long contributed,
                      @JsonProperty("joined") String joined,
                      @JsonProperty("joinedFriendly") String joinedFriendly) {
            this.name = name;
            this.uuid = uuid;
            this.rank = rank;
            this.contributed = contributed;
            try {
                this.joined = apiFormat.parse(joined);
            } catch (ParseException e) {
                e.printStackTrace();
            }
            this.joinedFriendly = joinedFriendly;
        }

        public static DateFormat getApiFormat() {
            return apiFormat;
        }

        public String getName() {
            return name;
        }

        public String getUuid() {
            return uuid;
        }

        public String getRank() {
            return rank;
        }

        public long getContributed() {
            return contributed;
        }

        public Date getJoined() {
            return joined;
        }

        public String getJoinedFriendly() {
            return joinedFriendly;
        }
    }

    private static class Banner {
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
