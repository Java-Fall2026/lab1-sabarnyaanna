package ua.bank.repository;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
    private final Object lock = new Object();

    public GenericRepository(String entityName, IdentityExtractor<T> identityExtractor) {
        this.entityName = Objects.requireNonNull(entityName, "entityName must not be null");
        this.identityExtractor = Objects.requireNonNull(identityExtractor, "identityExtractor must not be null");
    }

    public String identityOf(T item) {
        if (item == null) {
            throw new IllegalArgumentException(entityName + " item must not be null");
        }
        return normalizeId(identityExtractor.identityOf(item));
    }

    public void add(T item) {
        String id = identityOf(item);
        synchronized (lock) {
            if (storage.containsKey(id)) {
                DuplicateIdentityException ex = new DuplicateIdentityException(entityName, id);
                log.error("[{}] {}", entityName, ex.getMessage());
                throw ex;
            }
            storage.put(id, item);
            log.info("[{}] Added item with identity '{}'", entityName, id);
        }
    }

    public boolean addIfAbsent(T item) {
        String id = identityOf(item);
        synchronized (lock) {
            if (storage.containsKey(id)) {
                log.warn("[{}] Duplicate identity '{}' skipped by addIfAbsent", entityName, id);
                return false;
            }
            storage.put(id, item);
            log.info("[{}] Added item with identity '{}' via addIfAbsent", entityName, id);
            return true;
        }
    }

    public int addAll(Collection<? extends T> items) {
        if (items == null) {
            log.error("[{}] Attempted to call addAll with null collection", entityName);
            throw new IllegalArgumentException("items collection must not be null");
        }
        int addedCount = 0;
        for (T item : items) {
            if (addIfAbsent(item)) {
                addedCount++;
            }
        }
        log.info("[{}] Batch addAll finished: added {} of {} items", entityName, addedCount, items.size());
        return addedCount;
    }

    /**
     * Замінює існуючий запис за його identity. Якщо запис не знайдено — кидає NoSuchElementException.
     */
    public T replace(T item) {
        String id = identityOf(item);
        synchronized (lock) {
            if (!storage.containsKey(id)) {
                log.warn("[{}] Cannot replace missing item with identity '{}'", entityName, id);
                throw new NoSuchElementException(entityName + " with identity '" + id + "' not found");
            }
            storage.put(id, item);
            log.info("[{}] Replaced item with identity '{}'", entityName, id);
            return item;
        }
    }

    public boolean removeByIdentity(String identity) {
        if (identity == null || identity.trim().isEmpty()) {
            log.warn("[{}] Attempted to remove item with blank identity", entityName);
            return false;
        }
        String id = identity.trim();
        synchronized (lock) {
            T removed = storage.remove(id);
            if (removed != null) {
                log.info("[{}] Removed item with identity '{}'", entityName, id);
                return true;
            }
            log.warn("[{}] Item with identity '{}' not found for removal", entityName, id);
            return false;
        }
    }

    public boolean remove(T item) {
        if (item == null) {
            return false;
        }
        return removeByIdentity(identityOf(item));
    }

    public Optional<T> findByIdentity(String identity) {
        if (identity == null || identity.trim().isEmpty()) {
            log.warn("[{}] Search called with blank identity", entityName);
            return Optional.empty();
        }
        String id = identity.trim();
        T found;
        synchronized (lock) {
            found = storage.get(id);
        }
        if (found != null) {
            log.info("[{}] Found item by identity '{}'", entityName, id);
            return Optional.of(found);
        }
        log.warn("[{}] Item with identity '{}' not found", entityName, id);
        return Optional.empty();
    }

    public List<T> findAll() {
        synchronized (lock) {
            log.info("[{}] Retrieved all items (count={})", entityName, storage.size());
            return List.copyOf(storage.values());
        }
    }

    /**
     * Повертає Stream<T> над потокобезпечним знімком поточного стану сховища.
     */
    public Stream<T> stream() {
        List<T> snapshot;
        synchronized (lock) {
            snapshot = List.copyOf(storage.values());
        }
        return snapshot.stream();
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
        synchronized (lock) {
            return storage.size();
        }
    }

    private String normalizeId(String rawId) {
        if (rawId == null || rawId.trim().isEmpty()) {
            log.error("[{}] Extracted identity is null or blank", entityName);
            throw new IllegalArgumentException(entityName + " extracted identity must not be null or blank");
        }
        return rawId.trim();
    }
}