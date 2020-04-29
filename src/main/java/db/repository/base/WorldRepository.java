package db.repository.base;

import db.ConnectionPool;
import db.model.world.World;
import db.model.world.WorldId;
import log.Logger;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public abstract class WorldRepository extends Repository<World, WorldId> {
    protected WorldRepository(ConnectionPool db, Logger logger) {
        super(db, logger);
    }

    /**
     * Retrieves all main worlds (WC.* or EU.*).
     * @return List of all main worlds.
     */
    @Nullable
    public abstract List<World> findAllMainWorlds();

    /**
     * Retrieves all war worlds (WAR.*).
     * @return List o all war worlds.
     */
    @Nullable
    public abstract List<World> findAllWarWorlds();

    /**
     * Updates all worlds to the given worlds, and removes all worlds not in the given worlds.
     * Only updates
     * @param worlds Current list of worlds.
     * @return {@code true} if success.
     */
    @CheckReturnValue
    public abstract boolean updateAll(Collection<World> worlds);
}
