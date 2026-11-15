package ua.bank.util;

import java.time.LocalDate;
import ua.bank.model.LoanContract;

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

    public static String validateAndNormalizeLoanType(String loanType) {
        ValidationHelper.requireNotBlank(loanType, "loanType");
        String normalized = FormatHelper.normalizeUpper(loanType);
        ValidationHelper.requireAllowedValue(normalized, ValidationHelper.ALLOWED_LOAN_TYPES, "loanType");
        return normalized;
    }

    public static String validateAndNormalizeRepaymentStatus(String status) {
        ValidationHelper.requireNotBlank(status, "status");
        String normalized = FormatHelper.normalizeUpper(status);
        ValidationHelper.requireAllowedValue(normalized, ValidationHelper.ALLOWED_REPAYMENT_STATUSES, "status");
        return normalized;
    }

    public static String formatMoney(double amount) {
        return FormatHelper.formatMoney(amount);
    }

    public static String capitalize(String text) {
        return FormatHelper.capitalize(text);
    }

    // --- Computed methods from Variant 12 profile ---

    public static double monthlyPayment(LoanContract contract) {
        ValidationHelper.requireNotNull(contract, "contract");
        return contract.getPrincipalAmount() / contract.getTermMonths();
    }

    public static LocalDate lastDueDate(LoanContract contract) {
        ValidationHelper.requireNotNull(contract, "contract");
        return contract.getIssueDate().plusMonths(contract.getTermMonths());
    }
}