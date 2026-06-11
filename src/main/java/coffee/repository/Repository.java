package coffee.repository;

import java.util.List;
import java.util.Optional;

/**
 * Generic Repository Interface
 * Demonstrates: bounded generics <T, ID>, interface abstraction
 */
public interface Repository<T, ID> {
    void save(T entity);
    Optional<T> findById(ID id);
    List<T> findAll();
    void delete(ID id);
    int count();
}
