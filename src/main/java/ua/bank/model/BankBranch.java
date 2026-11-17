package ua.bank.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BankBranch(
        @NotBlank(message = "Код відділення не може бути порожнім")
        @Pattern(regexp = "^BR-\\d{2,4}$", message = "Код відділення повинен відповідати формату BR-XX")
        String branchCode,

        @NotBlank(message = "Назва міста не може бути порожньою")
        @Size(min = 2, max = 50, message = "Назва міста має містити від 2 до 50 символів")
        String city
) {
    @JsonCreator
    public BankBranch(
            @JsonProperty("branchCode") String branchCode,
            @JsonProperty("city") String city
    ) {
        this.branchCode = branchCode == null ? null : branchCode.trim();
        this.city = city == null ? null : city.trim();
    }

    public static BankBranch of(String branchCode, String city) {
        return new BankBranch(branchCode, city);
    }
}