package ua.bank.model;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;

/**
 * Represents a bank loan contract with natural ordering across all record components.
 */
public record LoanContract(
        String contractId,
        Borrower borrower,
        BankBranch branch,
        LoanType loanType,
        double principalAmount,
        LocalDate issueDate,
        int termMonths
) implements Comparable<LoanContract> {

    private static final Comparator<LoanContract> NATURAL_COMPARATOR = Comparator
            .comparing(LoanContract::contractId)
            .thenComparing(LoanContract::borrower)
            .thenComparing(c -> c.branch().branchCode())
            .thenComparing(c -> c.branch().city())
            .thenComparing(LoanContract::loanType)
            .thenComparingDouble(LoanContract::principalAmount)
            .thenComparing(LoanContract::issueDate)
            .thenComparingInt(LoanContract::termMonths);

    public LoanContract {
        if (contractId == null || contractId.trim().isEmpty()) {
            throw new IllegalArgumentException("contractId must not be blank");
        }
        Objects.requireNonNull(borrower, "borrower must not be null");
        Objects.requireNonNull(branch, "branch must not be null");
        Objects.requireNonNull(loanType, "loanType must not be null");
        if (principalAmount <= 0) {
            throw new IllegalArgumentException("principalAmount must be positive");
        }
        Objects.requireNonNull(issueDate, "issueDate must not be null");
        if (termMonths <= 0) {
            throw new IllegalArgumentException("termMonths must be positive");
        }

        double maxAllowedLoan = borrower.getMonthlyIncome() * 60.0;
        if (principalAmount > maxAllowedLoan) {
            throw new IllegalArgumentException(
                    "Principal amount (" + principalAmount + ") exceeds 60x borrower monthly income (" + maxAllowedLoan + ")"
            );
        }

        contractId = contractId.trim();
    }

    @Override
    public int compareTo(LoanContract other) {
        if (other == null) {
            throw new NullPointerException("Cannot compare LoanContract to null");
        }
        return NATURAL_COMPARATOR.compare(this, other);
    }
}