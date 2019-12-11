package db.repository.base;

import java.util.List;

public interface IRepository<T, ID> {
    <S extends T> void create(S entity);
    boolean exists(ID id);
    long count();
    T findOne(ID id);
    List<T> findAll();
    void update(T entity);
    void delete(ID id);
}
