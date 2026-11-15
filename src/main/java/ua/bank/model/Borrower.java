package ua.bank.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.logging.Logger;
import ua.bank.util.BankUtils;
import ua.common.BaseEntity;

public class Borrower extends BaseEntity {

    private static final Logger LOGGER = Logger.getLogger(Borrower.class.getName());

    private final String passportNum;
    private final String fullName;
    private final LocalDate birthDate;
    private double monthlyIncome;

    public Borrower(String passportNum, String fullName, LocalDate birthDate, double monthlyIncome) {
        super();
        this.passportNum = BankUtils.validateAndTrimText(passportNum, "passportNum");
        this.fullName = BankUtils.validateAndTrimText(fullName, "fullName");
        BankUtils.validateBorrowerAge(birthDate);
        this.birthDate = birthDate;
        setMonthlyIncome(monthlyIncome);
        LOGGER.info("Created Borrower: " + this.passportNum + " (" + this.fullName + ")");
    }

    public String getPassportNum() { return passportNum; }
    public String getFullName() { return fullName; }
    public LocalDate getBirthDate() { return birthDate; }
    public double getMonthlyIncome() { return monthlyIncome; }

    public final void setMonthlyIncome(double monthlyIncome) {
        BankUtils.validatePositiveAmount(monthlyIncome, "monthlyIncome");
        this.monthlyIncome = monthlyIncome;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return Objects.equals(passportNum, ((Borrower) o).passportNum);
    }

    @Override
    public int hashCode() { return Objects.hash(passportNum); }

    @Override
    public String toString() {
        return "Borrower{passportNum='" + passportNum + "', fullName='" + fullName +
                "', birthDate=" + birthDate + ", monthlyIncome=" + BankUtils.formatMoney(monthlyIncome) +
                ", createdAt=" + createdAt + '}';
    }
}