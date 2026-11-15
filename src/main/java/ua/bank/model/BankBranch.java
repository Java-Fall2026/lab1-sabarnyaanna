package ua.bank.model;

import java.util.logging.Logger;
import ua.bank.util.BankUtils;

public record BankBranch(String branchCode, String city) {

    private static final Logger LOGGER = Logger.getLogger(BankBranch.class.getName());

    public BankBranch {
        branchCode = BankUtils.validateAndTrimText(branchCode, "branchCode");
        city = BankUtils.validateAndTrimText(city, "city");
        LOGGER.info("Created BankBranch: " + branchCode + " (" + city + ")");
    }

    public static BankBranch of(String branchCode, String city) {
        return new BankBranch(branchCode, city);
    }
}