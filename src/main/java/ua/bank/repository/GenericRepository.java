package ua.bank.repository;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic in-memory repository supporting identity-based lookup and Stream sorting.
 *
 * @param <T> domain entity type
 */
public class GenericRepository<T> {

    private static final Logger log = LoggerFactory.getLogger(GenericRepository.class);

    private final String entityName;
    private final IdentityExtractor<T> identityExtractor;
    private final Map<String, T> storage = new LinkedHashMap<>();

    public GenericRepository(String entityName, IdentityExtractor<T> identityExtractor) {
        this.entityName = Objects.requireNonNull(entityName, "entityName must not be null");
        this.identityExtractor = Objects.requireNonNull(identityExtractor, "identityExtractor must not be null");
    }

    protected String getEntityName() {
        return entityName;
    }

    public boolean add(T item) {
        if (item == null) {
            log.error("[{}] Attempted to add null item", entityName);
            throw new IllegalArgumentException(entityName + " item must not be null");
        }
        String id = normalizeId(identityExtractor.identityOf(item));
        if (storage.containsKey(id)) {
            log.warn("[{}] Duplicate identity '{}' ignored", entityName, id);
            return false;
        }
        storage.put(id, item);
        log.info("[{}] Added item with identity '{}'", entityName, id);
        return true;
    }

    public int addAll(Collection<? extends T> items) {
        Objects.requireNonNull(items, "items must not be null");
        int added = 0;
        for (T item : items) {
            if (add(item)) {
                added++;
            }
        }
        return added;
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
        return removeByIdentity(normalizeId(identityExtractor.identityOf(item)));
    }

    public Optional<T> findByIdentity(String identity) {
        if (identity == null || identity.trim().isEmpty()) {
            log.warn("[{}] Search called with blank identity", entityName);
            return Optional.empty();
        }
        String id = identity.trim();
        T found = storage.get(id);
        if (found != null) {
            log.info("[{}] Found item by identity '{}'", entityName, id);
            return Optional.of(found);
        }
        log.warn("[{}] Item with identity '{}' not found", entityName, id);
        return Optional.empty();
    }

    public List<T> findAll() {
        log.info("[{}] Retrieved all items (count={})", entityName, storage.size());
        return List.copyOf(storage.values());
    }

    public Stream<T> stream() {
        return storage.values().stream();
    }

    public List<T> findAllSorted(Comparator<T> comparator) {
        Objects.requireNonNull(comparator, "comparator must not be null");
        List<T> sorted = stream()
                .sorted(comparator)
                .toList();
        log.info("[{}] Sorted items with custom comparator -> count={}", entityName, sorted.size());
        return sorted;
    }

    public List<T> findAllSortedByIdentity(SortOrder order) {
        Objects.requireNonNull(order, "SortOrder must not be null");
        Comparator<T> idComparator = Comparator.comparing(identityExtractor::identityOf);
        if (order == SortOrder.DESC) {
            idComparator = idComparator.reversed();
        }
        List<T> sorted = stream()
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
            log.error("[{}] Extracted identity is null or blank", entityName);
            throw new IllegalArgumentException(entityName + " extracted identity must not be null or blank");
        }
        return rawId.trim();
    }
}