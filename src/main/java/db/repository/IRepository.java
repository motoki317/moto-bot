package db.repository;

public interface IRepository<T> {
    <S extends T> void create(S entity);
    boolean exists(T entity);
    long count();
    T findOne(T entity);
    Iterable<T> findAll();
    void update(T entity);
    void delete(T entity);
}
