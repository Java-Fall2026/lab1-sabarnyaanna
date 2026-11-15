package ua.bank.model;

import ua.bank.util.BankUtils;

public record BankBranch(String branchCode, String city) {

    public BankBranch {
        branchCode = BankUtils.validateAndTrimText(branchCode, "branchCode");
        city = BankUtils.validateAndTrimText(city, "city");
    }

    public static BankBranch of(String branchCode, String city) {
        return new BankBranch(branchCode, city);
    }
}