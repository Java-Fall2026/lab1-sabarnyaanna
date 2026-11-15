package ua.bank;

import java.time.LocalDate;
import ua.bank.model.BankBranch;
import ua.bank.model.Borrower;
import ua.bank.model.LoanContract;
import ua.bank.model.RepaymentScheduleItem;
import ua.bank.util.BankUtils;

public class Main {

    public static void main(String[] args) {
        System.out.println("=== 1. Створення об'єктів конструктором та фабричним методом of(...) ===");
        Borrower borrower = new Borrower(
                "  AA123456  ",
                "  Олена Ковальчук  ",
                LocalDate.of(1995, 5, 14),
                35000.0
        );
        BankBranch branch = BankBranch.of("  BR-012  ", "Чернівці");
        System.out.println("Створено через конструктор: " + borrower);
        System.out.println("Створено через фабрику of(): " + branch);

        System.out.println("\n=== 2. Нормалізація в дії ===");
        LoanContract contract = new LoanContract(
                "LN-2026-001",
                borrower,
                branch,
                "  mortgage  ", // вхідний рядок у нижньому регістрі з пробілами
                600000.0,
                LocalDate.of(2026, 10, 1),
                24
        );
        System.out.println("Вхідний тип кредиту: '  mortgage  ' -> Нормалізований: '" + contract.getLoanType() + "'");

        RepaymentScheduleItem firstPayment = RepaymentScheduleItem.of(
                contract,
                LocalDate.of(2026, 11, 1),
                25000.0,
                "  pending "
        );
        System.out.println("Вхідний статус платежу: '  pending ' -> Нормалізований: '" + firstPayment.getStatus() + "'");

        System.out.println("\n=== 3. Три порушення правил валідації (try / catch) ===");
        // Порушення 1: вік позичальника менше 21 року
        try {
            new Borrower("BB987654", "Іван Петренко", LocalDate.now().minusYears(19), 20000.0);
        } catch (IllegalArgumentException e) {
            System.out.println("Помилка 1 (вік): " + e.getMessage());
        }

        // Порушення 2: сума кредиту перевищує 60 місячних доходів (35000 * 60 = 2_100_000)
        try {
            new LoanContract(
                    "LN-ERR-01",
                    borrower,
                    branch,
                    "AUTO",
                    2_500_000.0,
                    LocalDate.of(2026, 10, 10),
                    36
            );
        } catch (IllegalArgumentException e) {
            System.out.println("Помилка 2 (ліміт суми): " + e.getMessage());
        }

        // Порушення 3: неіснуючий тип кредиту
        try {
            new LoanContract(
                    "LN-ERR-02",
                    borrower,
                    branch,
                    "STUDENT_LOAN",
                    50000.0,
                    LocalDate.of(2026, 10, 10),
                    12
            );
        } catch (IllegalArgumentException e) {
            System.out.println("Помилка 3 (тип кредиту): " + e.getMessage());
        }

        System.out.println("\n=== 4. Спроба зіпсувати коректний об'єкт через сеттер ===");
        try {
            borrower.setMonthlyIncome(-5000.0);
        } catch (IllegalArgumentException e) {
            System.out.println("Сеттер заблокував некоректний дохід: " + e.getMessage());
        }

        try {
            firstPayment.setStatus("CANCELLED");
        } catch (IllegalArgumentException e) {
            System.out.println("Сеттер заблокував некоректний статус: " + e.getMessage());
        }

        // Коректна зміна через сеттер
        firstPayment.setStatus("paid");
        System.out.println("Статус платежу успішно оновлено на: " + firstPayment.getStatus());

        System.out.println("\n=== 5. Порівняння об'єктів: ==, equals, hashCode ===");
        Borrower sameBorrower = new Borrower(
                "AA123456",
                "Олена Ковальчук (Інший запис)",
                LocalDate.of(1995, 5, 14),
                40000.0
        );
        Borrower differentBorrower = new Borrower(
                "CC555444",
                "Максим Сидоренко",
                LocalDate.of(1990, 3, 20),
                28000.0
        );

        System.out.println("borrower == sameBorrower: " + (borrower == sameBorrower));
        System.out.println("borrower.equals(sameBorrower): " + borrower.equals(sameBorrower));
        System.out.println("hashCode 1: " + borrower.hashCode() + ", hashCode 2: " + sameBorrower.hashCode());
        System.out.println("borrower.equals(differentBorrower): " + borrower.equals(differentBorrower));

        System.out.println("\n=== 6. Виклик обчислюваних методів із BankUtils ===");
        double monthly = BankUtils.monthlyPayment(contract);
        LocalDate lastDate = BankUtils.lastDueDate(contract);
        System.out.println("Щомісячний платіж за договором " + contract.getContractId() + ": " + BankUtils.formatMoney(monthly));
        System.out.println("Дата останнього платежу: " + lastDate);

        System.out.println("\n=== 7. Вивід усіх сутностей через toString() ===");
        System.out.println(borrower);
        System.out.println(branch);
        System.out.println(contract);
        System.out.println(firstPayment);

        // 8. Compilation error demonstration (package-private access & private constructor):
        // ValidationHelper.requirePositive(100, "amount"); // Error: ValidationHelper is not public in ua.bank.util; cannot be accessed from outside package
        // BankBranch invalidBranch = new BankBranch("BR-99", "Kyiv"); // Error: BankBranch(String, String) has private access in ua.bank.model.BankBranch
    }
}