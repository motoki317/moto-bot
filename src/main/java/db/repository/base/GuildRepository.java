package db.repository.base;

import db.model.guild.Guild;
import db.model.guild.GuildId;
import db.repository.Repository;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public interface GuildRepository extends Repository<Guild, GuildId> {
    /**
     * Find all guilds with provided guild names.
     * @param guildNames Guild names.
     * @return List of guilds.
     */
    @Nullable
    List<Guild> findAllIn(@NotNull String... guildNames);

    /**
     * Find all matches with case insensitive and ignoring trailing spaces search.
     * Should probably want to call {@link #findOne(Object id)} first.
     * @param guildName Guild name.
     * @return List of guilds.
     */
    @Nullable
    List<Guild> findAllCaseInsensitive(@NotNull String guildName);

    /**
     * Find all guilds having the specified prefix. Case sensitive.
     * @param prefix Prefix.
     * @return List of guilds.
     */
    @Nullable
    List<Guild> findAllByPrefix(@NotNull String prefix);

    /**
     * Find all guilds having the specified prefix. Case <b>insensitive</b>.
     * @param prefix Prefix.
     * @return List of guilds.
     */
    @Nullable
    List<Guild> findAllByPrefixCaseInsensitive(@NotNull String prefix);
}
