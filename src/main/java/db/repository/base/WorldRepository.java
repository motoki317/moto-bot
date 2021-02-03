package db.repository.base;

import db.model.world.World;
import db.model.world.WorldId;
import db.repository.Repository;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

public interface WorldRepository extends Repository<World, WorldId> {
    /**
     * Retrieves all main worlds (WC.* or EU.*).
     * @return List of all main worlds.
     */
    @Nullable
    List<World> findAllMainWorlds();

    /**
     * Retrieves all war worlds (WAR.*).
     * @return List o all war worlds.
     */
    @Nullable
    List<World> findAllWarWorlds();

    /**
     * Updates all worlds to the given worlds, and removes all worlds not in the given worlds.
     * Only updates
     * @param worlds Current list of worlds.
     * @return {@code true} if success.
     */
    @CheckReturnValue
    boolean updateAll(Collection<World> worlds);
}
