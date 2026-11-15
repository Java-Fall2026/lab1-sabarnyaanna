package ua.bank;

import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ua.bank.exception.InvalidDataException;
import ua.bank.io.BankCsvReader;
import ua.bank.model.BankBranch;
import ua.bank.model.Borrower;
import ua.bank.model.LoanContract;
import ua.bank.model.LoanType;
import ua.bank.util.BankUtils;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        LOGGER.info("=== Запуск програми (Лабораторна робота №3) ===");

        // 1. Зчитування даних з CSV-файлів (із пропуском некоректних рядків через InvalidDataException)
        List<BankBranch> branches = BankCsvReader.readBranches("data/branches.csv");
        List<Borrower> borrowers = BankCsvReader.readBorrowers("data/borrowers.csv");

        System.out.println("\nУспішно зчитані відділення (" + branches.size() + "):");
        branches.forEach(System.out::println);

        System.out.println("\nУспішно зчитані позичальники (" + borrowers.size() + "):");
        borrowers.forEach(System.out::println);

        // 2. Демонстрація обробки відсутнього файлу (логування SEVERE/ERROR)
        System.out.println("\n=== Спроба прочитати неіснуючий файл ===");
        BankCsvReader.readBranches("data/missing_file.csv");

        // 3. Пряма демонстрація власного виключення InvalidDataException
        System.out.println("\n=== Демонстрація InvalidDataException ===");
        try {
            BankCsvReader.parseBorrowerLine("XX000111,Тест Користувач,1990-01-01,not_a_number", 99);
        } catch (InvalidDataException e) {
            LOGGER.log(Level.WARNING, "Перехоплено власне виключення: " + e.getMessage());
        }

        // 4. Створення кредитного договору зі зчитаних даних та обробка порушення правил домену
        System.out.println("\n=== Порушення правил домену при створенні договору ===");
        if (!branches.isEmpty() && !borrowers.isEmpty()) {
            Borrower validBorrower = borrowers.get(0);
            BankBranch validBranch = branches.get(0);

            LoanContract contract = new LoanContract(
                    "LN-2026-301", validBorrower, validBranch, LoanType.MORTGAGE,
                    500000.0, LocalDate.of(2026, 10, 15), 24
            );
            LOGGER.info("Створено договір: " + contract.contractId()
                    + ", щомісячний платіж = " + BankUtils.formatMoney(BankUtils.monthlyPayment(contract)));

            try {
                // Порушення ліміту суми (> monthlyIncome * 60)
                new LoanContract("LN-ERR", validBorrower, validBranch, LoanType.AUTO,
                        9_000_000.0, LocalDate.of(2026, 10, 15), 12);
            } catch (IllegalArgumentException e) {
                LOGGER.log(Level.SEVERE, "Порушення бізнес-правила домену: " + e.getMessage());
            }
        }

        LOGGER.info("=== Роботу програми успішно завершено ===");
    }
}