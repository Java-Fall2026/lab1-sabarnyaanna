package ua.bank;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.bank.model.BankBranch;
import ua.bank.model.Borrower;
import ua.bank.model.LoanContract;
import ua.bank.model.LoanType;
import ua.bank.repository.BorrowerRepository;
import ua.bank.repository.LoanContractRepository;
import ua.bank.repository.SortOrder;
import ua.bank.util.BankUtils;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("=== Запуск програми (Лабораторна робота №5: Сортування та Stream API) ===");

        // 1. Наповнення доменних репозиторіїв
        BorrowerRepository borrowerRepo = new BorrowerRepository();
        LoanContractRepository contractRepo = new LoanContractRepository();

        Borrower b1 = new Borrower("CC300300", "Олена Ковальчук", LocalDate.of(1995, 5, 14), 35000.0);
        Borrower b2 = new Borrower("AA100100", "Тарас Бондар", LocalDate.of(1988, 9, 22), 55000.0);
        Borrower b3 = new Borrower("BB200200", "Наталія Ткачук", LocalDate.of(1992, 11, 30), 55000.0);
        Borrower b4 = new Borrower("DD400400", "Андрій Мельник", LocalDate.of(1985, 2, 10), 22000.0);

        borrowerRepo.add(b1);
        borrowerRepo.add(b2);
        borrowerRepo.add(b3);
        borrowerRepo.add(b4);

        BankBranch kyiv = BankBranch.of("BR-01", "Київ");
        BankBranch chernivtsi = BankBranch.of("BR-02", "Чернівці");
        BankBranch lviv = BankBranch.of("BR-03", "Львів");

        contractRepo.add(new LoanContract("LN-03", b1, kyiv, LoanType.MORTGAGE, 800000.0, LocalDate.of(2026, 5, 10), 120));
        contractRepo.add(new LoanContract("LN-01", b2, chernivtsi, LoanType.AUTO, 450000.0, LocalDate.of(2026, 7, 15), 36));
        contractRepo.add(new LoanContract("LN-04", b3, kyiv, LoanType.MORTGAGE, 800000.0, LocalDate.of(2026, 8, 20), 60));
        contractRepo.add(new LoanContract("LN-02", b4, lviv, LoanType.CONSUMER, 120000.0, LocalDate.of(2026, 9, 1), 12));

        // 2. Сортування за природним порядком, за identity (ASC / DESC) та багаторівневим компаратором
        System.out.println("\n=== Сортування позичальників (природний порядок: passportNum) ===");
        borrowerRepo.findAllSortedByNaturalOrder().forEach(System.out::println);

        System.out.println("\n=== Сортування договорів за identity (DESC) ===");
        contractRepo.findAllSortedByIdentity(SortOrder.DESC).forEach(System.out::println);

        System.out.println("\n=== Багаторівневе сортування договорів (сума DESC -> термін ASC) ===");
        contractRepo.findAllSortedByAmountDescThenTermAsc().forEach(System.out::println);

        // 3. Демонстрація TreeSet із компаратором, неузгодженим з equals
        System.out.println("\n=== TreeSet з компаратором за доходом (не узгодженим з equals) ===");
        Set<Borrower> treeSetByIncome = new TreeSet<>(Comparator.comparingDouble(Borrower::getMonthlyIncome));
        treeSetByIncome.add(b2); // дохід 55000.0
        boolean addedSameIncome = treeSetByIncome.add(b3); // інший паспорт, але той самий дохід 55000.0!
        System.out.println("Чи додано другого позичальника з таким самим доходом 55000 у TreeSet: " + addedSameIncome);
        System.out.println("Розмір TreeSet: " + treeSetByIncome.size() + " (TreeSet вважає елементи дублікатами, бо compare == 0)");

        // 4. Пошук через Stream API (діапазон, назва, кілька умов, distinct)
        System.out.println("\n=== Пошук договорів у діапазоні сум [400 000 .. 900 000] ===");
        contractRepo.findByPrincipalRange(400_000.0, 900_000.0).forEach(System.out::println);

        System.out.println("\n=== Унікальні міста відділень у договорах (map + distinct + sorted) ===");
        System.out.println(contractRepo.findDistinctBranchCities());

        // 5. Групування та агрегація
        System.out.println("\n=== Групування договорів за типом (groupingBy + counting) ===");
        System.out.println("Кількість за типами: " + contractRepo.countByLoanType());
        System.out.println("Загальна сума всіх кредитів (reduce): "
                + BankUtils.formatMoney(contractRepo.calculateTotalPrincipal()));

        DoubleSummaryStatistics incomeStats = borrowerRepo.getIncomeStatistics();
        System.out.printf("Статистика доходів: мін=%.2f, макс=%.2f, середній=%.2f%n",
                incomeStats.getMin(), incomeStats.getMax(), incomeStats.getAverage());

        // 6. Обробка порожнього результату через Optional (без isPresent() + get())
        System.out.println("\n=== Обробка порожнього результату (Optional) ===");
        Optional<LoanContract> missingContract = contractRepo.findFirstByTypeCityAndMinDate(
                LoanType.COMMERCIAL, "Одеса", LocalDate.of(2026, 1, 1)
        );
        String message = missingContract
                .map(c -> "Знайдено: " + c.contractId())
                .orElse("Договір за заданими умовами не знайдено (Optional.empty)");
        System.out.println(message);

        // 7. Повторне використання вже спожитого Stream -> IllegalStateException
        System.out.println("\n=== Повторне використання спожитого Stream ===");
        Stream<Borrower> borrowerStream = borrowerRepo.stream();
        long count = borrowerStream.count(); // термінальна операція спожила стрім
        System.out.println("Перший виклик count(): " + count);
        try {
            borrowerStream.forEach(System.out::println); // повторний виклик на тому ж Stream
        } catch (IllegalStateException e) {
            log.error("Очікувана помилка повторного використання Stream: {}", e.getMessage());
        }

        log.info("=== Роботу програми успішно завершено ===");
    }
}