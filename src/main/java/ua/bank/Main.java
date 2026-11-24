package ua.bank;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.bank.io.BankCsvReader;
import ua.bank.model.BankBranch;
import ua.bank.model.Borrower;
import ua.bank.model.LoanContract;
import ua.bank.model.LoanType;
import ua.bank.repository.BorrowerRepository;
import ua.bank.repository.LoanContractRepository;
import ua.bank.repository.SortOrder;

/**
 * Application entry point for Lab 5 demonstrating Stream API sorting,
 * text and range filtering, grouping, and aggregation.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Starting application (Lab 5: Stream API, Comparable/Comparator, Aggregation)");

        // 1. Load data from CSV files
        List<BankBranch> branches = BankCsvReader.readBranches("data/branches.csv");
        List<Borrower> csvBorrowers = BankCsvReader.readBorrowers("data/borrowers.csv");

        BorrowerRepository borrowerRepo = new BorrowerRepository();
        borrowerRepo.addAll(csvBorrowers);

        LoanContractRepository contractRepo = new LoanContractRepository();
        if (!branches.isEmpty() && borrowerRepo.size() >= 2) {
            List<Borrower> allBorrowers = borrowerRepo.findAll();
            BankBranch b1 = branches.get(0);
            BankBranch b2 = branches.size() > 1 ? branches.get(1) : b1;

            contractRepo.add(new LoanContract("LN-2026-03", allBorrowers.get(0), b1, LoanType.MORTGAGE,
                    800000.0, LocalDate.of(2026, 3, 10), 120));
            contractRepo.add(new LoanContract("LN-2026-01", allBorrowers.get(1), b2, LoanType.AUTO,
                    450000.0, LocalDate.of(2026, 1, 15), 48));
            contractRepo.add(new LoanContract("LN-2026-02", allBorrowers.get(0), b1, LoanType.CONSUMER,
                    120000.0, LocalDate.of(2026, 2, 20), 24));
        }

        // 2. Demonstrate findAllSortedByIdentity(ASC)
        System.out.println("=== Договори, відсортовані за identity (ASC) ===");
        contractRepo.findAllSortedByIdentity(SortOrder.ASC)
                .forEach(c -> System.out.println(c.contractId() + " (" + c.loanType() + "): " + c.principalAmount()));

        // 3. Demonstrate borrower sorting
        System.out.println("\n=== Позичальники, відсортовані за доходом (DESC) ===");
        borrowerRepo.findAllSortedByIncome(SortOrder.DESC)
                .forEach(b -> System.out.println(b.getPassportNum() + " | " + b.getFullName() + " | " + b.getMonthlyIncome()));

        // 4. Demonstrate text search
        System.out.println("\n=== Текстовий пошук позичальників за підрядком 'ов' ===");
        borrowerRepo.searchByNameContaining("ов")
                .forEach(System.out::println);

        // 5. Demonstrate groupByLoanType
        System.out.println("\n=== Групування договорів за типом кредиту (groupByLoanType) ===");
        Map<LoanType, List<LoanContract>> grouped = contractRepo.groupByLoanType();
        grouped.forEach((type, list) -> System.out.println(type + " -> " + list.size() + " договорів"));

        // 6. Demonstrate aggregation
        System.out.println("\n=== Загальна сума кредитів ===");
        System.out.println("Загальний портфель: " + contractRepo.getTotalPrincipalAmount());
        System.out.println("Статистика доходів позичальників: " + borrowerRepo.getIncomeStatistics());

        log.info("Application finished successfully");
    }
}