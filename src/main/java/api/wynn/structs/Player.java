package api.wynn.structs;

import api.wynn.structs.common.RequestV2;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import utils.JsonUtils;

import javax.annotation.Nullable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Player {
    private static final DateFormat apiFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final ObjectMapper mapper = new ObjectMapper();

    private final RequestV2 request;

    private String username;
    private String uuid;
    private String rank;

    private MetaInfo metaInfo;
    private WynnClass[] classes;
    private GuildInfo guildInfo;
    private GlobalInfo globalInfo;

    public Player(String body) throws JsonProcessingException {
        this.request = new RequestV2(body);
        JsonNode json = mapper.readTree(body);
        JsonNode data = json.get("data").get(0);

        for (Iterator<Map.Entry<String, JsonNode>> i = data.fields(); i.hasNext(); ) {
            Map.Entry<String, JsonNode> e = i.next();

            switch (e.getKey()) {
                case "username" -> this.username = e.getValue().asText();
                case "uuid" -> this.uuid = e.getValue().asText();
                case "rank" -> this.rank = e.getValue().asText();
                case "meta" -> this.metaInfo = new MetaInfo(e.getValue());
                case "classes" -> {
                    this.classes = new WynnClass[e.getValue().size()];
                    for (int j = 0; j < e.getValue().size(); j++) {
                        this.classes[j] = new WynnClass(e.getValue().get(j));
                    }
                }
                case "guild" -> this.guildInfo = new GuildInfo(e.getValue());
                case "global" -> this.globalInfo = new GlobalInfo(this, e.getValue());
            }
        }
    }

    public RequestV2 getRequest() {
        return request;
    }

    public String getUsername() {
        return username;
    }

    public String getUuid() {
        return uuid;
    }

    public String getRank() {
        return rank;
    }

    public MetaInfo getMetaInfo() {
        return metaInfo;
    }

    public WynnClass[] getClasses() {
        return classes;
    }

    public GuildInfo getGuildInfo() {
        return guildInfo;
    }

    public GlobalInfo getGlobalInfo() {
        return globalInfo;
    }

    // ----- Inner classes -----

    public static class MetaInfo {
        public long getFirstJoin() {
            return firstJoin;
        }

        public long getLastJoin() {
            return lastJoin;
        }

        public String getServer() {
            return server;
        }

        public boolean isVeteran() {
            return isVeteran;
        }

        public int getPlaytime() {
            return playtime;
        }

        public boolean isTagDisplayed() {
            return tagDisplay;
        }

        @Nullable
        public String getTag() {
            return tag;
        }

        /**
         * First join time in unix millis
         */
        private long firstJoin;
        /**
         * Last join time in unix millis
         */
        private long lastJoin;
        /**
         * Online server name or null
         */
        private final String server;
        private final boolean isVeteran;

        private final int playtime;

        private final boolean tagDisplay;
        @Nullable
        private final String tag;

        private MetaInfo(JsonNode meta) {
            try {
                firstJoin = apiFormat.parse(meta.get("firstJoin").asText()).getTime();
                lastJoin = apiFormat.parse(meta.get("lastJoin").asText()).getTime();
            } catch (ParseException e) {
                e.printStackTrace();
                firstJoin = -1;
                lastJoin = -1;
            }
            server = meta.get("location").get("online").asBoolean() ? meta.get("location").get("server").asText() : null;
            isVeteran = meta.get("veteran").asBoolean();
            playtime = meta.get("playtime").asInt();
            tagDisplay = meta.get("tag").get("display").asBoolean();
            tag = JsonUtils.getNullableString(meta.get("tag"), "value");
        }
    }

    public static class WynnClass {
        private final String name;
        private final int level;
        private final int quests;
        private final List<String> questNames;
        private final int itemsIdentified;
        private final int mobsKilled;

        private final int pvpKills;
        private final int pvpDeaths;

        private final int chestsFound;
        private final long blocksWalked;
        private final int logins;
        private final int deaths;
        private final int playtime;

        private final Skills skills;

        private final Professions professions;

        private final Dungeons dungeons;

        private final int discoveries;
        private final int eventsWon;
        private final boolean preEconomyUpdate;

        private WynnClass(JsonNode data) {
            name = data.get("name").asText();
            level = data.get("level").asInt();
            quests = data.get("quests").get("completed").asInt();
            questNames = new ArrayList<>();
            JsonNode questList = data.get("quests").get("list");
            for (int i = 0; i < questList.size(); i++)
                questNames.add(questList.get(i).asText());
            itemsIdentified = data.get("itemsIdentified").asInt();
            mobsKilled = data.get("mobsKilled").asInt();
            pvpKills = data.get("pvp").get("kills").asInt();
            pvpDeaths = data.get("pvp").get("deaths").asInt();
            chestsFound = data.get("chestsFound").asInt();
            blocksWalked = data.get("blocksWalked").asLong();
            logins = data.get("logins").asInt();
            deaths = data.get("deaths").asInt();
            playtime = data.get("playtime").asInt();

            skills = new Skills(data.get("skills"));

            professions = new Professions(data.get("professions"));

            dungeons = new Dungeons(data.get("dungeons"));

            discoveries = data.get("discoveries").asInt();
            eventsWon = data.get("eventsWon").asInt();
            preEconomyUpdate = data.get("preEconomyUpdate").asBoolean();
        }

        public String getName() {
            return name;
        }

        public int getTotalLevel() {
            return level;
        }

        public int getCombatLevel() {
            return this.professions.profLevels.get("combat").level;
        }

        public int getProfessionsLevel() {
            return this.getTotalLevel() - this.getCombatLevel();
        }

        public int getQuests() {
            return quests;
        }

        public List<String> getQuestNames() {
            return questNames;
        }

        public int getItemsIdentified() {
            return itemsIdentified;
        }

        public int getMobsKilled() {
            return mobsKilled;
        }

        public int getPvpKills() {
            return pvpKills;
        }

        public int getPvpDeaths() {
            return pvpDeaths;
        }

        public int getChestsFound() {
            return chestsFound;
        }

        public long getBlocksWalked() {
            return blocksWalked;
        }

        public int getLogins() {
            return logins;
        }

        public int getDeaths() {
            return deaths;
        }

        public int getPlaytime() {
            return playtime;
        }

        public Skills getSkills() {
            return skills;
        }

        public int getDiscoveries() {
            return discoveries;
        }

        public int getEventsWon() {
            return eventsWon;
        }

        public boolean isPreEconomyUpdate() {
            return preEconomyUpdate;
        }

        public Professions getProfessions() {
            return professions;
        }

        public Dungeons getDungeons() {
            return dungeons;
        }

        public static class Skills {
            public Map<String, Integer> getData() {
                return data;
            }

            private final Map<String, Integer> data;

            private Skills(JsonNode node) {
                data = new HashMap<>();
                Iterator<String> it = node.fieldNames();
                while (it.hasNext()) {
                    String skillName = it.next();
                    data.put(skillName, node.get(skillName).asInt());
                }
            }
        }

    }

    public static class Professions {
        private final Map<String, ProfessionLevel> profLevels;

        private Professions(JsonNode professions) {
            profLevels = new HashMap<>();
            Iterator<String> it = professions.fieldNames();
            while (it.hasNext()) {
                String profName = it.next();
                profLevels.put(profName,
                        new ProfessionLevel(
                                professions.get(profName).get("level").asInt(),
                                professions.get(profName).get("xp").asText()
                        )
                );
            }
        }

        public Map<String, ProfessionLevel> getProfLevels() {
            return profLevels;
        }
    }

    public static class ProfessionLevel {
        private final int level;
        private final String xp;

        ProfessionLevel(int level, String xp) {
            this.level = level;
            this.xp = xp;
        }

        public int getLevel() {
            return level;
        }

        public String getXp() {
            return xp;
        }
    }

    public static class Dungeons {
        private final int totalCompleted;
        private final Map<String, Integer> list;

        private Dungeons(JsonNode data) {
            list = new HashMap<>();
            totalCompleted = data.get("completed").asInt();
            for (int i = 0; i < data.get("list").size(); i++) {
                JsonNode child = data.get("list").get(i);
                list.put(child.get("name").asText(), child.get("completed").asInt());
            }
        }

        public int getTotalCompleted() {
            return totalCompleted;
        }

        public Map<String, Integer> getList() {
            return list;
        }
    }

    public static class GuildInfo {
        @Nullable
        public String getName() {
            return name;
        }

        @Nullable
        public String getRank() {
            return rank;
        }

        @Nullable
        private final String name;
        @Nullable
        private final String rank;

        private GuildInfo(JsonNode guild) {
            this.name = JsonUtils.getNullableString(guild, "name");
            this.rank = JsonUtils.getNullableString(guild, "rank");
        }
    }

    public static class GlobalInfo {
        private final Player parent;

        private final int chestsFound;
        private final long blocksWalked;
        private final int itemsIdentified;
        private final int mobsKilled;

        private final int totalLevelCombat;
        private final int totalLevelProfession;

        private final int pvpKills;
        private final int pvpDeaths;

        private final int logins;
        private final int deaths;
        private final int discoveries;
        private final int eventsWon;

        private GlobalInfo(Player parent, JsonNode data) {
            this.parent = parent;
            itemsIdentified = data.get("itemsIdentified").asInt();
            mobsKilled = data.get("mobsKilled").asInt();

            totalLevelCombat = data.get("totalLevel").get("combat").asInt();
            totalLevelProfession = data.get("totalLevel").get("profession").asInt();

            pvpKills = data.get("pvp").get("kills").asInt();
            pvpDeaths = data.get("pvp").get("deaths").asInt();
            chestsFound = data.get("chestsFound").asInt();
            blocksWalked = data.get("blocksWalked").asLong();
            logins = data.get("logins").asInt();
            deaths = data.get("deaths").asInt();

            discoveries = data.get("discoveries").asInt();
            eventsWon = data.get("eventsWon").asInt();
        }

        public int getPlayTime() {
            int c = 0;
            for (WynnClass wc : this.parent.classes)
                c += wc.playtime;
            return c;
        }

        public int getChestsFound() {
            return chestsFound;
        }

        public long getBlocksWalked() {
            return blocksWalked;
        }

        public int getItemsIdentified() {
            return itemsIdentified;
        }

        public int getMobsKilled() {
            return mobsKilled;
        }

        public int getTotalLevelCombat() {
            return totalLevelCombat;
        }

        public int getTotalLevelProfession() {
            return totalLevelProfession;
        }

        public int getTotalLevelCombined() {
            return totalLevelCombat + totalLevelProfession;
        }

        public int getPvpKills() {
            return pvpKills;
        }

        public int getPvpDeaths() {
            return pvpDeaths;
        }

        public int getLogins() {
            return logins;
        }

        public int getDeaths() {
            return deaths;
        }

        public int getDiscoveries() {
            return discoveries;
        }

        public int getEventsWon() {
            return eventsWon;
        }
    }
}
