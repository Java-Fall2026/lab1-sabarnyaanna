package ua.bank.repository;

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

    /**
     * Додає елемент у репозиторій. Якщо елемент із таким identity вже існує — повертає false і не перезаписує його.
     */
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

    /**
     * Видаляє елемент за його унікальним identity.
     */
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

    /**
     * Видаляє переданий об'єкт, витягуючи його identity через IdentityExtractor.
     */
    public boolean remove(T item) {
        if (item == null) {
            return false;
        }
        String id = normalizeId(identityExtractor.extractIdentity(item));
        return removeByIdentity(id);
    }

    /**
     * Пошук об'єкта за унікальним полем (identity).
     */
    public Optional<T> findByIdentity(String identity) {
        if (identity == null || identity.trim().isEmpty()) {
            log.warn("[{}] Search called with blank identity", entityName);
            return Optional.empty();
        }
        String id = identity.trim();
        T found = storage.get(id);
        if (found != null) {
            log.info("[{}] Found item by identity '{}'", entityName, id);
        } else {
            log.info("[{}] No item found by identity '{}'", entityName, id);
        }
        return Optional.ofNullable(found);
    }

    /**
     * Повертає незмінну копію списку всіх елементів репозиторію.
     */
    public List<T> getAll() {
        log.info("[{}] Retrieved all items (count={})", entityName, storage.size());
        return List.copyOf(storage.values());
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