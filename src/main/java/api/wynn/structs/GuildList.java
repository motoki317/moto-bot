package api.wynn.structs;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class GuildList {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final List<GuildListEntry> guilds;

    public GuildList(String body) throws JsonProcessingException {
        this.guilds = new ArrayList<>();

        var json = mapper.readTree(body);
        for (int i = 0; i < json.size(); i++) {
            var entry = json.get(i);
            this.guilds.add(mapper.readValue(entry.toString(), GuildListEntry.class));
        }
    }

    public List<GuildListEntry> getGuilds() {
        return guilds;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GuildListEntry {
        private String name;
        private String prefix;

        public String getName() {
            return name;
        }

        public String getPrefix() {
            return prefix;
        }
    }
}
