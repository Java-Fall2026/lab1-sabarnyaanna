package ua.bank;

import java.time.LocalDate;
import ua.bank.model.BankBranch;
import ua.bank.model.Borrower;
import ua.bank.model.LoanContract;
import ua.bank.model.LoanType;
import ua.bank.model.RepaymentScheduleItem;
import ua.bank.model.RepaymentStatus;
import ua.bank.util.BankUtils;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== 1. Створення record-об'єктів та звернення до їхніх компонентів ===");
        Borrower borrower = new Borrower(
                "AA123456",
                "Олена Ковальчук",
                LocalDate.of(1995, 5, 14),
                35000.0
        );
        BankBranch branch = BankBranch.of("  BR-012  ", "  Чернівці  ");
        LoanContract contract = new LoanContract(
                "LN-2026-001",
                borrower,
                branch,
                LoanType.MORTGAGE,
                600000.0,
                LocalDate.of(2026, 10, 1),
                24
        );
        System.out.println("Компоненти record BankBranch: код = " + branch.branchCode() + ", місто = " + branch.city());
        System.out.println("Компоненти record LoanContract: ID = " + contract.contractId()
                + ", тип = " + contract.loanType()
                + ", сума = " + BankUtils.formatMoney(contract.principalAmount()));

        System.out.println("\n=== 2. Робота з enum: values(), поля константи та valueOf() у try/catch ===");
        System.out.println("Усі доступні типи кредитів (LoanType.values()):");
        for (LoanType type : LoanType.values()) {
            System.out.printf(" - %s (%s): базова ставка = %.0f%%%n",
                    type.name(), type.getDescription(), type.getAnnualRate() * 100);
        }

        try {
            LoanType invalidType = LoanType.valueOf("CRYPTO_LOAN");
            System.out.println(invalidType);
        } catch (IllegalArgumentException e) {
            System.out.println("Помилка LoanType.valueOf(\"CRYPTO_LOAN\"): " + e.getMessage());
        }

        System.out.println("\n=== 3. Робота обох switch-виразів у BankUtils ===");
        double riskCoef = BankUtils.getRiskCoefficient(contract.loanType());
        System.out.println("Коефіцієнт ризику для " + contract.loanType() + ": " + riskCoef);

        RepaymentScheduleItem paymentItem = RepaymentScheduleItem.of(
                contract,
                LocalDate.of(2026, 11, 1),
                25000.0,
                RepaymentStatus.PENDING
        );
        System.out.println("Дія для статусу " + paymentItem.getStatus() + ": "
                + BankUtils.getAvailableAction(paymentItem.getStatus()));

        paymentItem.setStatus(RepaymentStatus.OVERDUE);
        System.out.println("Дія після зміни на " + paymentItem.getStatus() + ": "
                + BankUtils.getAvailableAction(paymentItem.getStatus()));

        System.out.println("\n=== 4. Валідація у компактному конструкторі record (IllegalArgumentException) ===");
        try {
            new BankBranch("   ", "Київ");
        } catch (IllegalArgumentException e) {
            System.out.println("Помилка створення BankBranch: " + e.getMessage());
        }

        try {
            new LoanContract(
                    "LN-ERR-01",
                    borrower,
                    branch,
                    LoanType.AUTO,
                    150000.0,
                    LocalDate.of(2026, 10, 5),
                    500 // некоректний термін > 360 міс.
            );
        } catch (IllegalArgumentException e) {
            System.out.println("Помилка створення LoanContract: " + e.getMessage());
        }

        System.out.println("\n=== 5. Два однакові record-об'єкти: згенеровані equals() та hashCode() ===");
        BankBranch branchCopy = new BankBranch("BR-012", "Чернівці");
        System.out.println("branch == branchCopy: " + (branch == branchCopy));
        System.out.println("branch.equals(branchCopy): " + branch.equals(branchCopy));
        System.out.println("branch.hashCode() == branchCopy.hashCode(): "
                + (branch.hashCode() == branchCopy.hashCode())
                + " (" + branch.hashCode() + ")");

        System.out.println("\n=== 6. Вивід record через автоматично згенерований toString() ===");
        System.out.println(branch);
        System.out.println(contract);
    }
}