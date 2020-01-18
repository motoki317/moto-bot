package api.wynn.structs;

import api.wynn.structs.common.Request;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.Nullable;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class GuildLeaderboard {
    private static final DateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private Request request;
    private List<Guild> data;

    public Request getRequest() {
        return request;
    }

    public List<Guild> getData() {
        return data;
    }

    public static class Guild {
        private String name;
        private String prefix;
        private long xp;
        private int level;
        private String created;
        private Banner banner;
        private int territories;
        private int membersCount;
        private int num;

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

        public Banner getBanner() {
            return banner;
        }

        public int getTerritories() {
            return territories;
        }

        public int getMembersCount() {
            return membersCount;
        }

        public int getNum() {
            return num;
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
}
