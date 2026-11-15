package ua.bank.util;

import java.time.LocalDate;
import ua.bank.model.LoanContract;
import ua.bank.model.LoanType;
import ua.bank.model.RepaymentStatus;

public final class BankUtils {

    private BankUtils() {
    }

    // --- Validation & formatting delegates ---

    public static String validateAndTrimText(String value, String fieldName) {
        ValidationHelper.requireNotBlank(value, fieldName);
        return FormatHelper.normalizeTrim(value);
    }

    public static void validateBorrowerAge(LocalDate birthDate) {
        ValidationHelper.requireMinAge(birthDate, ValidationHelper.MIN_BORROWER_AGE, "birthDate");
    }

    public static void validatePositiveAmount(double amount, String fieldName) {
        ValidationHelper.requirePositive(amount, fieldName);
    }

    public static void validatePrincipalAmount(double principalAmount, double monthlyIncome) {
        double maxAllowed = monthlyIncome * ValidationHelper.MAX_INCOME_MULTIPLIER;
        ValidationHelper.requireMaxAmount(principalAmount, maxAllowed, "principalAmount");
    }

    public static void validateNotNull(Object value, String fieldName) {
        ValidationHelper.requireNotNull(value, fieldName);
    }

    public static void validateIssueDate(LocalDate issueDate) {
        ValidationHelper.requireNotInFuture(issueDate, "issueDate");
    }

    public static void validateTermMonths(int termMonths) {
        ValidationHelper.requireInRange(
                termMonths,
                ValidationHelper.MIN_TERM_MONTHS,
                ValidationHelper.MAX_TERM_MONTHS,
                "termMonths");
    }

    public static void validateDueDate(LocalDate dueDate, LocalDate issueDate) {
        ValidationHelper.requireDateAfter(dueDate, issueDate, "dueDate", "issueDate");
    }

    public static String formatMoney(double amount) {
        return FormatHelper.formatMoney(amount);
    }

    // --- Computed methods from Variant 12 profile ---

    public static double monthlyPayment(LoanContract contract) {
        ValidationHelper.requireNotNull(contract, "contract");
        return contract.principalAmount() / contract.termMonths();
    }

    public static LocalDate lastDueDate(LoanContract contract) {
        ValidationHelper.requireNotNull(contract, "contract");
        return contract.issueDate().plusMonths(contract.termMonths());
    }

    // --- Switch-expressions (Lab 2) ---

    // 1. Switch-expression over all LoanType constants (returns coefficient / fee multiplier)
    public static double getRiskCoefficient(LoanType loanType) {
        ValidationHelper.requireNotNull(loanType, "loanType");
        return switch (loanType) {
            case MORTGAGE -> 1.05;
            case AUTO -> 1.12;
            case COMMERCIAL -> 1.20;
            case CONSUMER -> 1.35;
        };
    }

    // 2. Switch-expression for behavior selection by RepaymentStatus
    public static String getAvailableAction(RepaymentStatus status) {
        ValidationHelper.requireNotNull(status, "status");
        return switch (status) {
            case PENDING -> "Очікує оплати: доступна стандартна сплата без комісії";
            case PAID -> "Сплачено: дії за платежем закриті, можна завантажити квитанцію";
            case OVERDUE -> "Прострочено: нараховується пеня, потрібне термінове погашення";
        };
    }
}