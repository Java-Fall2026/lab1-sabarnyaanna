package ua.bank.model;

import java.util.Objects;
import ua.bank.util.BankUtils;
import ua.common.BaseEntity;

public class BankBranch extends BaseEntity {

    private final String branchCode;
    private final String city;

    private BankBranch(String branchCode, String city) {
        super();
        this.branchCode = BankUtils.validateAndTrimText(branchCode, "branchCode");
        this.city = BankUtils.validateAndTrimText(city, "city");
    }

    public static BankBranch of(String branchCode, String city) {
        return new BankBranch(branchCode, city);
    }

    public String getBranchCode() {
        return branchCode;
    }

    public String getCity() {
        return city;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankBranch that = (BankBranch) o;
        return Objects.equals(branchCode, that.branchCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(branchCode);
    }

    @Override
    public String toString() {
        return "BankBranch{" +
                "branchCode='" + branchCode + '\'' +
                ", city='" + city + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}