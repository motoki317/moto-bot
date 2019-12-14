package db.repository.base;

import org.jetbrains.annotations.NotNull;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.util.List;

public interface IRepository<T, ID> {
    /**
     * Creates a new entity in the repository.
     * @param entity Entity
     * @return True if succeeded.
     */
    @CheckReturnValue
    <S extends T> boolean create(@NotNull S entity);

    /**
     * Checks if an entity exists in the repository.
     * @param id Identification of an entity.
     * @return True if exists.
     */
    @CheckReturnValue
    boolean exists(@NotNull ID id);

    /**
     * Counts the total number of entity in the repository.
     * @return Number of entity.
     */
    @CheckReturnValue
    long count();

    /**
     * Finds one entity from the repository.
     * @param id Identification of an entity.
     * @return An instance if found. null if not.
     */
    @Nullable
    T findOne(@NotNull ID id);

    /**
     * Retrieves all entities in the repository.
     * @return List of all entities. Returns an empty list on failure.
     */
    @Nullable
    List<T> findAll();

    /**
     * Updates an entity.
     * @param entity Entity to update with the new data.
     * @return True if succeeded.
     */
    @CheckReturnValue
    boolean update(@NotNull T entity);

    /**
     * Deletes an entity.
     * @param id Identification of an entity.
     * @return True if succeeded.
     */
    @CheckReturnValue
    boolean delete(@NotNull ID id);
}
