package ua.bank.repository;

@FunctionalInterface
public interface IdentityExtractor<T> {
    String extractIdentity(T item);
}