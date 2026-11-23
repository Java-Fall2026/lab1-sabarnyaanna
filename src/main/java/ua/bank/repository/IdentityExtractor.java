package ua.bank.repository;

@FunctionalInterface
public interface IdentityExtractor<T> {
    String identityOf(T item);
}