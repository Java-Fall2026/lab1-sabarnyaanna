package ua.bank;

import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.bank.exception.InvalidDataException;
import ua.bank.io.BankCsvReader;
import ua.bank.model.BankBranch;
import ua.bank.model.Borrower;
import ua.bank.model.LoanContract;
import ua.bank.model.LoanType;
import ua.bank.util.BankUtils;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Starting application (Lab 3: Exceptions, CSV I/O, Logging)");

        // 1. Зчитування даних з CSV-файлів
        List<BankBranch> branches = BankCsvReader.readBranches("data/branches.csv");
        List<Borrower> borrowers = BankCsvReader.readBorrowers("data/borrowers.csv");

        System.out.println("\nУспішно зчитані відділення (" + branches.size() + "):");
        branches.forEach(System.out::println);

        System.out.println("\nУспішно зчитані позичальники (" + borrowers.size() + "):");
        borrowers.forEach(System.out::println);

        // 2. Демонстрація обробки відсутнього файлу
        System.out.println("\n=== Спроба прочитати неіснуючий файл ===");
        List<BankBranch> missingBranches = BankCsvReader.readBranches("data/missing_file.csv");
        if (missingBranches.isEmpty()) {
            System.out.println("Файл data/missing_file.csv не знайдено, завантажено 0 записів.");
        }

        // 3. Пряма демонстрація власного виключення InvalidDataException
        System.out.println("\n=== Демонстрація InvalidDataException ===");
        try {
            BankCsvReader.parseBorrowerLine("XX000111,Тест Користувач,1990-01-01,not_a_number", 99);
        } catch (InvalidDataException e) {
            log.warn("Caught custom InvalidDataException: {}", e.getMessage());
            System.out.println("Помилка розбору рядка CSV: " + e.getMessage());
        }

        // 4. Створення кредитного договору та демонстрація порушення правил домену (у сеттері та конструкторі)
        System.out.println("\n=== Порушення правил домену в сеттері та конструкторі ===");
        if (!branches.isEmpty() && !borrowers.isEmpty()) {
            Borrower validBorrower = borrowers.get(0);
            BankBranch validBranch = branches.get(0);

            try {
                validBorrower.setMonthlyIncome(-1);
            } catch (IllegalArgumentException e) {
                log.warn("Rejected invalid monthlyIncome in setter for {}: {}",
                        validBorrower.getPassportNum(), e.getMessage());
                System.out.println("Не вдалося оновити дохід позичальника "
                        + validBorrower.getPassportNum() + ": " + e.getMessage());
            }

            LoanContract contract = new LoanContract(
                    "LN-2026-301", validBorrower, validBranch, LoanType.MORTGAGE,
                    500000.0, LocalDate.of(2026, 10, 15), 24
            );
            log.info("Created contract {}, monthlyPayment={}",
                    contract.contractId(), BankUtils.formatMoney(BankUtils.monthlyPayment(contract)));
            System.out.println("Створено договір: " + contract.contractId()
                    + ", щомісячний платіж: " + BankUtils.formatMoney(BankUtils.monthlyPayment(contract)));

            try {
                new LoanContract("LN-ERR", validBorrower, validBranch, LoanType.AUTO,
                        9_000_000.0, LocalDate.of(2026, 10, 15), 12);
            } catch (IllegalArgumentException e) {
                log.error("Domain business rule violated during contract creation: {}", e.getMessage());
                System.out.println("Помилка створення кредитного договору: " + e.getMessage());
            }
        }

        log.info("Application finished successfully");
    }
}