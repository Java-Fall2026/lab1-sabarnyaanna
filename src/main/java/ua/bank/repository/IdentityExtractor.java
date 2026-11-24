package ua.bank.repository;

/**
 * Functional interface for extracting a unique string identity from an entity.
 *
 * @param <T> domain entity type
 */
@FunctionalInterface
public interface IdentityExtractor<T> {
    String identityOf(T item);
}