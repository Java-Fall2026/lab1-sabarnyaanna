package ua.bank.repository;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericRepository<T> {

    private static final Logger log = LoggerFactory.getLogger(GenericRepository.class);

    private final String entityName;
    private final IdentityExtractor<T> identityExtractor;
    private final Map<String, T> storage = new LinkedHashMap<>();

    public GenericRepository(String entityName, IdentityExtractor<T> identityExtractor) {
        this.entityName = Objects.requireNonNull(entityName, "entityName must not be null");
        this.identityExtractor = Objects.requireNonNull(identityExtractor, "identityExtractor must not be null");
    }

    public void add(T item) {
        if (item == null) {
            log.error("[{}] Attempted to add null item", entityName);
            throw new IllegalArgumentException(entityName + " item must not be null");
        }
        String id = normalizeId(identityExtractor.identityOf(item));
        if (storage.containsKey(id)) {
            DuplicateIdentityException ex = new DuplicateIdentityException(entityName, id);
            log.error("[{}] {}", entityName, ex.getMessage());
            throw ex;
        }
        storage.put(id, item);
        log.info("[{}] Added item with identity '{}'", entityName, id);
    }

    public boolean addIfAbsent(T item) {
        if (item == null) {
            log.error("[{}] Attempted to call addIfAbsent with null item", entityName);
            throw new IllegalArgumentException(entityName + " item must not be null");
        }
        String id = normalizeId(identityExtractor.identityOf(item));
        if (storage.containsKey(id)) {
            log.warn("[{}] Duplicate identity '{}' skipped by addIfAbsent", entityName, id);
            return false;
        }
        storage.put(id, item);
        log.info("[{}] Added item with identity '{}' via addIfAbsent", entityName, id);
        return true;
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
            log.warn("[{}] Attempted to remove null item", entityName);
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