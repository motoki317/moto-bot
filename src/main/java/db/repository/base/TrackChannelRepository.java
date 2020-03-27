package db.repository.base;

import db.ConnectionPool;
import db.model.track.TrackChannel;
import db.model.track.TrackChannelId;
import db.model.track.TrackType;
import log.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class TrackChannelRepository extends Repository<TrackChannel, TrackChannelId> {
    protected TrackChannelRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Returns all track channel entries of the specified type.
     * @param type Type to find.
     * @return List of entries. null if something went wrong.
     */
    @Nullable
    public abstract List<TrackChannel> findAllOfType(TrackType type);

    /**
     * Returns all track channel entries with the guild id and channel id.
     * @param guildId Discord guild id.
     * @param channelId Discord channel id.
     * @return List of entries. null if something went wrong.
     */
    @Nullable
    public abstract List<TrackChannel> findAllOf(long guildId, long channelId);

    /**
     * Returns all track channel entries with the given guild name.
     * @param guildName Guild name.
     * @return List of entries.
     */
    @Nullable
    public abstract List<TrackChannel> findAllOfGuildNameAndType(String guildName, TrackType type);

    /**
     * Returns all track channel entries with the given player UUID.
     * @param playerUUID Player UUID with hyphens.
     * @return List of entries.
     */
    @Nullable
    public abstract List<TrackChannel> findAllOfPlayerUUIDAndType(String playerUUID, TrackType type);
}
