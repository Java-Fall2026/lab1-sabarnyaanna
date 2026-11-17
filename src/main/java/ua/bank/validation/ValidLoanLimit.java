package ua.bank.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = ValidLoanLimitValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLoanLimit {
    String message() default "Сума кредиту перевищує максимально допустимий ліміт (60 місячних доходів позичальника)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}