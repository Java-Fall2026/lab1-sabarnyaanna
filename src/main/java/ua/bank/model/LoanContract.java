package ua.bank.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;
import ua.bank.validation.ValidLoanLimit;

@ValidLoanLimit
public record LoanContract(
        @NotBlank(message = "Ідентифікатор договору не може бути порожнім")
        @Pattern(regexp = "^LN-\\d{4}-\\d{2,4}$", message = "Номер договору має відповідати формату LN-YYYY-XX")
        String contractId,

        @NotNull(message = "Позичальник є обов'язковим")
        @Valid
        Borrower borrower,

        @NotNull(message = "Відділення банку є обов'язковим")
        @Valid
        BankBranch branch,

        @NotNull(message = "Тип кредиту є обов'язковим")
        LoanType loanType,

        @Positive(message = "Сума кредиту повинна бути додатною")
        double principalAmount,

        @NotNull(message = "Дата видачі кредиту є обов'язковою")
        LocalDate issueDate,

        @Min(value = 1, message = "Мінімальний термін кредиту — 1 місяць")
        @Max(value = 360, message = "Максимальний термін кредиту — 360 місяців")
        int termMonths
) implements Comparable<LoanContract> {

    @JsonCreator
    public LoanContract(
            @JsonProperty("contractId") String contractId,
            @JsonProperty("borrower") Borrower borrower,
            @JsonProperty("branch") BankBranch branch,
            @JsonProperty("loanType") LoanType loanType,
            @JsonProperty("principalAmount") double principalAmount,
            @JsonProperty("issueDate") LocalDate issueDate,
            @JsonProperty("termMonths") int termMonths
    ) {
        this.contractId = contractId == null ? null : contractId.trim();
        this.borrower = borrower;
        this.branch = branch;
        this.loanType = loanType;
        this.principalAmount = principalAmount;
        this.issueDate = issueDate;
        this.termMonths = termMonths;
    }

    @Override
    public int compareTo(LoanContract other) {
        Objects.requireNonNull(other, "other LoanContract must not be null");
        return Comparator.comparing(LoanContract::contractId)
                .thenComparing(LoanContract::issueDate)
                .thenComparingDouble(LoanContract::principalAmount)
                .thenComparingInt(LoanContract::termMonths)
                .thenComparing(LoanContract::loanType)
                .thenComparing(LoanContract::borrower)
                .thenComparing(c -> c.branch().branchCode())
                .compare(this, other);
    }
}