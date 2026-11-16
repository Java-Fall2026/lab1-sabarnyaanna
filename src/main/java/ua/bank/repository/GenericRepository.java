package ua.bank.repository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericRepository<T> {

    private static final Logger log = LoggerFactory.getLogger(GenericRepository.class);

    protected final String entityName;
    private final IdentityExtractor<T> identityExtractor;
    private final Map<String, T> storage = new LinkedHashMap<>();

    public GenericRepository(String entityName, IdentityExtractor<T> identityExtractor) {
        this.entityName = Objects.requireNonNull(entityName, "entityName must not be null");
        this.identityExtractor = Objects.requireNonNull(identityExtractor, "identityExtractor must not be null");
    }

    public boolean add(T item) {
        if (item == null) {
            throw new IllegalArgumentException(entityName + " item must not be null");
        }
        String id = normalizeId(identityExtractor.extractIdentity(item));
        if (storage.containsKey(id)) {
            log.warn("[{}] Duplicate identity '{}' detected. Item was not added: {}", entityName, id, item);
            return false;
        }
        storage.put(id, item);
        log.info("[{}] Added item with identity '{}': {}", entityName, id, item);
        return true;
    }

    public boolean removeByIdentity(String identity) {
        if (identity == null || identity.trim().isEmpty()) {
            log.warn("[{}] Attempted to remove item with blank identity", entityName);
            return false;
        }
        String id = identity.trim();
        T removed = storage.remove(id);
        if (removed != null) {
            log.info("[{}] Removed item with identity '{}'", entityName, id);
            return true;
        }
        log.warn("[{}] Item with identity '{}' not found for removal", entityName, id);
        return false;
    }

    public boolean remove(T item) {
        if (item == null) {
            return false;
        }
        return removeByIdentity(normalizeId(identityExtractor.extractIdentity(item)));
    }

    public Optional<T> findByIdentity(String identity) {
        if (identity == null || identity.trim().isEmpty()) {
            log.warn("[{}] Search by identity called with blank value", entityName);
            return Optional.empty();
        }
        String id = identity.trim();
        Optional<T> result = storage.entrySet().stream()
                .filter(entry -> entry.getKey().equals(id))
                .map(Map.Entry::getValue)
                .findFirst();
        log.info("[{}] Search by identity='{}' -> found={}", entityName, id, result.isPresent());
        return result;
    }

    public List<T> getAll() {
        log.info("[{}] Retrieved all items (count={})", entityName, storage.size());
        return List.copyOf(storage.values());
    }

    /**
     * Повертає Stream<T> над поточними елементами сховища (лінивий конвеєр без створення копії списку).
     */
    public Stream<T> stream() {
        log.info("[{}] Opened stream over repository (current size={})", entityName, storage.size());
        return storage.values().stream();
    }

    public List<T> findAllSorted(Comparator<T> comparator) {
        Objects.requireNonNull(comparator, "comparator must not be null");
        List<T> sorted = storage.values().stream()
                .sorted(comparator)
                .toList();
        log.info("[{}] Sorted items with custom comparator -> count={}", entityName, sorted.size());
        return sorted;
    }

    public List<T> findAllSortedByIdentity(SortOrder order) {
        Objects.requireNonNull(order, "SortOrder must not be null");
        Comparator<T> idComparator = Comparator.comparing(identityExtractor::extractIdentity);
        if (order == SortOrder.DESC) {
            idComparator = idComparator.reversed();
        }
        List<T> sorted = storage.values().stream()
                .sorted(idComparator)
                .toList();
        log.info("[{}] Sorted by identity (order={}) -> count={}", entityName, order, sorted.size());
        return sorted;
    }

    public int size() {
        return storage.size();
    }

    private String normalizeId(String rawId) {
        if (rawId == null || rawId.trim().isEmpty()) {
            throw new IllegalArgumentException(entityName + " extracted identity must not be null or blank");
        }
        return rawId.trim();
    }
}