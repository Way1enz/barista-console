package coffee.repository;

import coffee.model.MenuItem;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MenuRepository
 * Demonstrates: HashMap internals (O(1) get/put), generics, streams, TreeMap for sorted view
 */
public class MenuRepository implements Repository<MenuItem, String> {

    // HashMap: key=id, value=item — O(1) average lookup
    // Internally: array of buckets + linked list/tree for collision chains
    private final Map<String, MenuItem> store = new HashMap<>();

    @Override
    public void save(MenuItem item) {
        store.put(item.getId(), item);  // O(1) amortized — hash(key) → bucket index
    }

    @Override
    public Optional<MenuItem> findById(String id) {
        return Optional.ofNullable(store.get(id));  // Optional wraps potential null — no NPE
    }

    @Override
    public List<MenuItem> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public void delete(String id) {
        store.remove(id);
    }

    @Override
    public int count() { return store.size(); }

    public List<MenuItem> findByCategory(String category) {
        return store.values().stream()
                .filter(item -> item.getCategory().equalsIgnoreCase(category))
                .sorted()                          // uses MenuItem.compareTo (Comparable)
                .collect(Collectors.toList());
    }

    public List<MenuItem> findAvailable() {
        return store.values().stream()
                .filter(MenuItem::isAvailable)     // method reference
                .sorted(Comparator.comparing(MenuItem::getCategory)
                        .thenComparing(MenuItem::getPrice))
                .collect(Collectors.toList());
    }

    // TreeMap gives sorted-by-price view — O(log n) operations, but ordered
    public Map<Double, List<MenuItem>> groupByPrice() {
        return store.values().stream()
                .collect(Collectors.groupingBy(
                        MenuItem::getPrice,
                        TreeMap::new,
                        Collectors.toList()
                ));
    }

    // Returns items whose names match a search string — demonstrates Stream.filter + lambda
    public List<MenuItem> search(String query) {
        String q = query.toLowerCase();
        return store.values().stream()
                .filter(item -> item.getName().toLowerCase().contains(q)
                             || item.getCategory().toLowerCase().contains(q))
                .sorted()
                .collect(Collectors.toList());
    }
}
