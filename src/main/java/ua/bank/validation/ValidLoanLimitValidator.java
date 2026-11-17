package ua.bank.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import ua.bank.model.LoanContract;

public class ValidLoanLimitValidator implements ConstraintValidator<ValidLoanLimit, LoanContract> {

    private static final double MAX_INCOME_MULTIPLIER = 60.0;

    @Override
    public boolean isValid(LoanContract contract, ConstraintValidatorContext context) {
        if (contract == null || contract.borrower() == null) {
            return true; // null-перевірку виконує @NotNull на полях
        }
        double maxAllowed = contract.borrower().getMonthlyIncome() * MAX_INCOME_MULTIPLIER;
        return Double.compare(contract.principalAmount(), maxAllowed) <= 0;
    }
}