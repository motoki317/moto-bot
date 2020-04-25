package commands.guild;

import db.model.guild.Guild;
import db.repository.base.GuildRepository;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import update.response.ResponseManager;
import update.selection.SelectionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GuildNameResolver {
    private final ResponseManager responseManager;
    private final GuildRepository guildRepository;

    public GuildNameResolver(ResponseManager responseManager, GuildRepository guildRepository) {
        this.responseManager = responseManager;
        this.guildRepository = guildRepository;
    }

    /**
     * Resolves guild name from user input.
     * @param guildName Guild name, or possibly guild prefix.
     * @param textChannel For guild selection.
     * @param author For guild selection.
     * @param onResolve On resolve, guild name and guild prefix are given.
     * @param onFailure On error, reason is given.
     */
    public void resolve(@NotNull String guildName,
                        TextChannel textChannel,
                        User author,
                        BiConsumer<@NotNull String, @Nullable String> onResolve,
                        Consumer<@NotNull String> onFailure) {
        List<Guild> guilds = findGuilds(guildName);
        if (guilds == null) {
            onFailure.accept("Something went wrong while retrieving guilds data...");
            return;
        }

        String resolvedName;
        String guildPrefix;
        if (guilds.size() == 0) {
            // Unknown guild, but pass it on as prefix unknown in case the bot haven't loaded the guild data yet
            resolvedName = guildName;
            guildPrefix = null;
        } else if (guilds.size() == 1) {
            resolvedName = guilds.get(0).getName();
            guildPrefix = guilds.get(0).getPrefix();
        } else {
            // Choose a guild
            List<String> guildNames = guilds.stream().map(Guild::getName).collect(Collectors.toList());
            Map<String, String> guildPrefixes = guilds.stream().collect(Collectors.toMap(Guild::getName, Guild::getPrefix));
            SelectionHandler handler = new SelectionHandler(
                    textChannel.getIdLong(),
                    author.getIdLong(),
                    guildNames,
                    name -> onResolve.accept(name, guildPrefixes.get(name))
            );
            handler.sendMessage(textChannel, author, () -> this.responseManager.addEventListener(handler));
            return;
        }

        onResolve.accept(resolvedName, guildPrefix);
    }

    /**
     * Finds guild with specified name (both full name or prefix was possibly specified).
     * @param specified Specified name.
     * @return List of found guilds. null if something went wrong.
     */
    @Nullable
    List<Guild> findGuilds(@NotNull String specified) {
        // Case sensitive full name search
        List<Guild> ret;
        Guild guild = this.guildRepository.findOne(() -> specified);
        if (guild != null) {
            ret = new ArrayList<>();
            ret.add(guild);
            return ret;
        }

        // Case insensitive full name search
        ret = this.guildRepository.findAllCaseInsensitive(specified);
        if (ret == null) {
            return null;
        }
        if (ret.size() > 0) {
            return ret;
        }

        // Prefix search
        if (specified.length() == 3) {
            // Case sensitive prefix search
            ret = this.guildRepository.findAllByPrefix(specified);
            if (ret == null) {
                return null;
            }
            if (ret.size() > 0) {
                return ret;
            }

            // Case insensitive prefix search
            ret = this.guildRepository.findAllByPrefixCaseInsensitive(specified);
            if (ret == null) {
                return null;
            }
            if (ret.size() > 0) {
                return ret;
            }
        }

        return ret;
    }
}
