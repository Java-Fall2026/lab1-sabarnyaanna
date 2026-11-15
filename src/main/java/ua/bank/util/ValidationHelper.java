package ua.bank.util;

import java.time.LocalDate;
import java.time.Period;

final class ValidationHelper {

    static final int MIN_BORROWER_AGE = 21;
    static final int MIN_TERM_MONTHS = 1;
    static final int MAX_TERM_MONTHS = 360;
    static final double MAX_INCOME_MULTIPLIER = 60.0;

    private ValidationHelper() {
    }

    static void requireNotNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null, got: null");
        }
    }

    static void requireNotBlank(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank, got: '" + value + "'");
        }
    }

    static void requirePositive(double value, String fieldName) {
        if (value <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than 0, got: " + value);
        }
    }

    static void requireInRange(int value, int min, int max, String fieldName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    fieldName + " must be between " + min + " and " + max + ", got: " + value);
        }
    }

    static void requireMaxAmount(double amount, double maxAllowed, String fieldName) {
        if (amount <= 0 || amount > maxAllowed) {
            throw new IllegalArgumentException(
                    fieldName + " must be greater than 0 and at most " + maxAllowed + ", got: " + amount);
        }
    }

    static void requireMinAge(LocalDate birthDate, int minAge, String fieldName) {
        requireNotNull(birthDate, fieldName);
        LocalDate today = LocalDate.now();
        if (birthDate.isAfter(today)) {
            throw new IllegalArgumentException(fieldName + " must not be in the future, got: " + birthDate);
        }
        int age = Period.between(birthDate, today).getYears();
        if (age < minAge) {
            throw new IllegalArgumentException(
                    "Borrower must be at least " + minAge + " years old, got age: " + age + " (birthDate: " + birthDate + ")");
        }
    }

    static void requireNotInFuture(LocalDate date, String fieldName) {
        requireNotNull(date, fieldName);
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(fieldName + " must not be in the future, got: " + date);
        }
    }

    static void requireDateAfter(LocalDate date, LocalDate referenceDate, String fieldName, String referenceName) {
        requireNotNull(date, fieldName);
        requireNotNull(referenceDate, referenceName);
        if (!date.isAfter(referenceDate)) {
            throw new IllegalArgumentException(
                    fieldName + " must be strictly after " + referenceName + " (" + referenceDate + "), got: " + date);
        }
    }
}