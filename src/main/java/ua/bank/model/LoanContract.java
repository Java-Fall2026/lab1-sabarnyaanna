package ua.bank.model;

import java.time.LocalDate;
import ua.bank.util.BankUtils;

public record LoanContract(
        String contractId,
        Borrower borrower,
        BankBranch branch,
        LoanType loanType,
        double principalAmount,
        LocalDate issueDate,
        int termMonths
) {
    public LoanContract {
        contractId = BankUtils.validateAndTrimText(contractId, "contractId");
        BankUtils.validateNotNull(borrower, "borrower");
        BankUtils.validateNotNull(branch, "branch");
        BankUtils.validateNotNull(loanType, "loanType");
        BankUtils.validatePrincipalAmount(principalAmount, borrower.getMonthlyIncome());
        BankUtils.validateIssueDate(issueDate);
        BankUtils.validateTermMonths(termMonths);
    }
}