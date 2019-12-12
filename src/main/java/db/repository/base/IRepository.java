package db.repository.base;

import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public interface IRepository<T, ID> {
    <S extends T> void create(@NotNull S entity);
    boolean exists(@NotNull ID id);
    long count();
    @Nullable
    T findOne(@NotNull ID id);
    @NotNull
    List<T> findAll();
    void update(@NotNull T entity);
    void delete(@NotNull ID id);
}
