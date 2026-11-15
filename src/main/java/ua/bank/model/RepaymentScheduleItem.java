package ua.bank.model;

import java.time.LocalDate;
import java.util.Objects;
import ua.bank.util.BankUtils;
import ua.common.BaseEntity;

public class RepaymentScheduleItem extends BaseEntity {

    private final LoanContract contract;
    private final LocalDate dueDate;
    private final double amountDue;
    private RepaymentStatus status;

    private RepaymentScheduleItem(LoanContract contract, LocalDate dueDate, double amountDue, RepaymentStatus status) {
        super();
        BankUtils.validateNotNull(contract, "contract");
        this.contract = contract;
        BankUtils.validateDueDate(dueDate, contract.issueDate());
        this.dueDate = dueDate;
        BankUtils.validatePositiveAmount(amountDue, "amountDue");
        this.amountDue = amountDue;
        setStatus(status);
    }

    public static RepaymentScheduleItem of(LoanContract contract, LocalDate dueDate, double amountDue, RepaymentStatus status) {
        return new RepaymentScheduleItem(contract, dueDate, amountDue, status);
    }

    public LoanContract getContract() {
        return contract;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public double getAmountDue() {
        return amountDue;
    }

    public RepaymentStatus getStatus() {
        return status;
    }

    public final void setStatus(RepaymentStatus status) {
        BankUtils.validateNotNull(status, "status");
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RepaymentScheduleItem that = (RepaymentScheduleItem) o;
        return Objects.equals(contract, that.contract) && Objects.equals(dueDate, that.dueDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contract, dueDate);
    }

    @Override
    public String toString() {
        return "RepaymentScheduleItem{" +
                "contractId='" + contract.contractId() + '\'' +
                ", dueDate=" + dueDate +
                ", amountDue=" + BankUtils.formatMoney(amountDue) +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}