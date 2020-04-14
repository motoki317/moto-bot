package commands.guild;

import db.model.guild.Guild;
import db.repository.base.GuildRepository;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuildPrefixesResolver {
    private final GuildRepository guildRepository;

    public GuildPrefixesResolver(GuildRepository guildRepository) {
        this.guildRepository = guildRepository;
    }

    /**
     * Retrieves a map containing guild name -> prefix data.
     * @param guildNames List of guild names.
     * @return Map from guild names to prefixes.
     */
    @NotNull
    public Map<String, String> resolveGuildPrefixes(List<String> guildNames) {
        Map<String, String> ret = new HashMap<>();
        if (guildNames.isEmpty()) {
            return ret;
        }

        List<Guild> guilds = this.guildRepository.findAllIn(guildNames.toArray(new String[]{}));
        if (guilds == null) {
            return ret;
        }

        for (Guild guild : guilds) {
            ret.put(guild.getName(), guild.getPrefix());
        }
        return ret;
    }
}
