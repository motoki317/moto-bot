package db.repository.base;

import db.model.track.TrackChannel;
import db.model.track.TrackChannelId;
import db.model.track.TrackType;
import db.repository.Repository;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface TrackChannelRepository extends Repository<TrackChannel, TrackChannelId> {
    /**
     * Returns all track channel entries of the specified type.
     * @param type Type to find.
     * @return List of entries. null if something went wrong.
     */
    @Nullable List<TrackChannel> findAllOfType(TrackType type);

    /**
     * Returns all track channel entries with the guild id and channel id.
     * @param guildId Discord guild id.
     * @param channelId Discord channel id.
     * @return List of entries. null if something went wrong.
     */
    @Nullable List<TrackChannel> findAllOf(long guildId, long channelId);

    /**
     * Returns all track channel entries with the given guild name.
     * @param guildName Guild name.
     * @return List of entries.
     */
    @Nullable List<TrackChannel> findAllOfGuildNameAndType(String guildName, TrackType type);

    /**
     * Returns all track channel entries with the given player UUID.
     * @param playerUUID Player UUID with hyphens.
     * @return List of entries.
     */
    @Nullable List<TrackChannel> findAllOfPlayerUUIDAndType(String playerUUID, TrackType type);

    /**
     * Deletes all tracking of the specified guild.
     * @param guildId Guild ID
     * @return {@code true} if success.
     */
    boolean deleteAllOfGuild(long guildId);

    /**
     * Deletes all tracking of the specified channel.
     * @param channelId Channel ID
     * @return {@code true} if success.
     */
    boolean deleteAllOfChannel(long channelId);
}
