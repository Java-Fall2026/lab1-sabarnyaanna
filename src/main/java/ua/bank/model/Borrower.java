package ua.bank.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Objects;
import ua.bank.util.BankUtils;
import ua.common.BaseEntity;

@JsonIgnoreProperties({"createdAt"})
public class Borrower extends BaseEntity implements Comparable<Borrower> {

    @NotBlank(message = "Номер паспорта не може бути порожнім")
    @Pattern(regexp = "^[A-ZА-ЯІЇЄ]{2}\\d{6}$", message = "Номер паспорта повинен мати 2 великі літери та 6 цифр")
    private final String passportNum;

    @NotBlank(message = "ПІБ позичальника не може бути порожнім")
    @Size(min = 3, max = 100, message = "ПІБ позичальника має містити від 3 до 100 символів")
    private final String fullName;

    @NotNull(message = "Дата народження є обов'язковою")
    @Past(message = "Дата народження повинна бути в минулому")
    private final LocalDate birthDate;

    @Positive(message = "Місячний дохід повинен бути більшим за 0")
    private double monthlyIncome;

    @JsonCreator
    public Borrower(
            @JsonProperty("passportNum") String passportNum,
            @JsonProperty("fullName") String fullName,
            @JsonProperty("birthDate") LocalDate birthDate,
            @JsonProperty("monthlyIncome") double monthlyIncome
    ) {
        super();
        this.passportNum = passportNum == null ? null : passportNum.trim();
        this.fullName = fullName == null ? null : fullName.trim();
        this.birthDate = birthDate;
        setMonthlyIncome(monthlyIncome);
    }

    public String getPassportNum() { return passportNum; }
    public String getFullName() { return fullName; }
    public LocalDate getBirthDate() { return birthDate; }
    public double getMonthlyIncome() { return monthlyIncome; }

    public final void setMonthlyIncome(double monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    @Override
    public int compareTo(Borrower other) {
        Objects.requireNonNull(other, "other Borrower must not be null");
        return this.passportNum.compareTo(other.passportNum);
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
                "', birthDate=" + birthDate + ", monthlyIncome=" + BankUtils.formatMoney(monthlyIncome) + '}';
    }
}