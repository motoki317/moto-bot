package db.repository.base;

import db.ConnectionPool;
import db.model.guild.Guild;
import db.model.guild.GuildId;
import log.Logger;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public abstract class GuildRepository extends Repository<Guild, GuildId> {
    protected GuildRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Find all guilds with provided guild names.
     * @param guildNames Guild names.
     * @return List of guilds.
     */
    @Nullable
    public abstract List<Guild> findAllIn(@NotNull String... guildNames);

    /**
     * Find all matches with case insensitive and ignoring trailing spaces search.
     * Should probably want to call {@link #findOne(Object id)} first.
     * @param guildName Guild name.
     * @return List of guilds.
     */
    @Nullable
    public abstract List<Guild> findAllCaseInsensitive(@NotNull String guildName);

    /**
     * Find all guilds having the specified prefix. Case sensitive.
     * @param prefix Prefix.
     * @return List of guilds.
     */
    @Nullable
    public abstract List<Guild> findAllByPrefix(@NotNull String prefix);

    /**
     * Find all guilds having the specified prefix. Case <b>insensitive</b>.
     * @param prefix Prefix.
     * @return List of guilds.
     */
    @Nullable
    public abstract List<Guild> findAllByPrefixCaseInsensitive(@NotNull String prefix);
}
