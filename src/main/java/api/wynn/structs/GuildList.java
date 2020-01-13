package api.wynn.structs;

import api.wynn.structs.common.Request;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class GuildList {
    private List<String> guilds;
    private Request request;

    @JsonProperty("guilds")
    public List<String> getGuilds() {
        return guilds;
    }

    @JsonProperty("request")
    public Request getRequest() {
        return request;
    }
}
