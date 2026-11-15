package ua.bank.model;

import java.time.LocalDate;
import java.util.Objects;
import ua.bank.util.BankUtils;
import ua.common.BaseEntity;

public class LoanContract extends BaseEntity {

    private final String contractId;
    private final Borrower borrower;
    private final BankBranch branch;
    private final String loanType;
    private final double principalAmount;
    private final LocalDate issueDate;
    private final int termMonths;

    public LoanContract(
            String contractId,
            Borrower borrower,
            BankBranch branch,
            String loanType,
            double principalAmount,
            LocalDate issueDate,
            int termMonths) {
        super();
        this.contractId = BankUtils.validateAndTrimText(contractId, "contractId");
        BankUtils.validateNotNull(borrower, "borrower");
        BankUtils.validateNotNull(branch, "branch");
        this.borrower = borrower;
        this.branch = branch;
        this.loanType = BankUtils.validateAndNormalizeLoanType(loanType);
        BankUtils.validatePrincipalAmount(principalAmount, borrower.getMonthlyIncome());
        this.principalAmount = principalAmount;
        BankUtils.validateIssueDate(issueDate);
        this.issueDate = issueDate;
        BankUtils.validateTermMonths(termMonths);
        this.termMonths = termMonths;
    }

    public String getContractId() {
        return contractId;
    }

    public Borrower getBorrower() {
        return borrower;
    }

    public BankBranch getBranch() {
        return branch;
    }

    public String getLoanType() {
        return loanType;
    }

    public double getPrincipalAmount() {
        return principalAmount;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public int getTermMonths() {
        return termMonths;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LoanContract that = (LoanContract) o;
        return Objects.equals(contractId, that.contractId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contractId);
    }

    @Override
    public String toString() {
        return "LoanContract{" +
                "contractId='" + contractId + '\'' +
                ", borrower=" + borrower.getFullName() + " (" + borrower.getPassportNum() + ")" +
                ", branch=" + branch.getBranchCode() +
                ", loanType='" + loanType + '\'' +
                ", principalAmount=" + BankUtils.formatMoney(principalAmount) +
                ", issueDate=" + issueDate +
                ", termMonths=" + termMonths +
                ", createdAt=" + createdAt +
                '}';
    }
}