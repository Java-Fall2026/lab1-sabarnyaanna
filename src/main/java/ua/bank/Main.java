package ua.bank;

import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.bank.io.BankCsvReader;
import ua.bank.model.BankBranch;
import ua.bank.model.Borrower;
import ua.bank.model.LoanContract;
import ua.bank.model.LoanType;
import ua.bank.model.RepaymentScheduleItem;
import ua.bank.model.RepaymentStatus;
import ua.bank.repository.GenericRepository;
import ua.bank.util.BankUtils;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("=== Запуск програми (Лабораторна робота №4: Generics та Колекції) ===");

        // 1. Створення репозиторіїв для різних типів сутностей за допомогою лямбд / method references
        GenericRepository<BankBranch> branchRepo =
                new GenericRepository<>("BankBranch", BankBranch::branchCode);

        GenericRepository<Borrower> borrowerRepo =
                new GenericRepository<>("Borrower", Borrower::getPassportNum);

        GenericRepository<LoanContract> contractRepo =
                new GenericRepository<>("LoanContract", LoanContract::contractId);

        // Для RepaymentScheduleItem унікальний ключ — композитний (contractId + dueDate)
        GenericRepository<RepaymentScheduleItem> scheduleRepo =
                new GenericRepository<>("RepaymentScheduleItem",
                        item -> item.getContract().contractId() + "_" + item.getDueDate());

        // 2. Завантаження об'єктів із CSV (ЛР 3) та додавання в репозиторії
        List<BankBranch> csvBranches = BankCsvReader.readBranches("data/branches.csv");
        csvBranches.forEach(branchRepo::add);

        List<Borrower> csvBorrowers = BankCsvReader.readBorrowers("data/borrowers.csv");
        csvBorrowers.forEach(borrowerRepo::add);

        // Створення об'єктів LoanContract та RepaymentScheduleItem з використанням усіх enum (ЛР 2)
        Borrower borrower1 = borrowerRepo.findByIdentity("AA111222").orElseThrow();
        Borrower borrower2 = borrowerRepo.findByIdentity("EE999000").orElseThrow();
        BankBranch branch1 = branchRepo.findByIdentity("BR-001").orElseThrow();
        BankBranch branch2 = branchRepo.findByIdentity("BR-002").orElseThrow();

        LoanContract contract1 = new LoanContract(
                "LN-2026-01", borrower1, branch1, LoanType.MORTGAGE,
                500000.0, LocalDate.of(2026, 10, 1), 24
        );
        LoanContract contract2 = new LoanContract(
                "LN-2026-02", borrower2, branch2, LoanType.AUTO,
                300000.0, LocalDate.of(2026, 10, 5), 12
        );
        contractRepo.add(contract1);
        contractRepo.add(contract2);

        RepaymentScheduleItem item1 = RepaymentScheduleItem.of(
                contract1, LocalDate.of(2026, 11, 1),
                BankUtils.monthlyPayment(contract1), RepaymentStatus.PAID
        );
        RepaymentScheduleItem item2 = RepaymentScheduleItem.of(
                contract1, LocalDate.of(2026, 12, 1),
                BankUtils.monthlyPayment(contract1), RepaymentStatus.PENDING
        );
        RepaymentScheduleItem item3 = RepaymentScheduleItem.of(
                contract2, LocalDate.of(2026, 11, 5),
                BankUtils.monthlyPayment(contract2), RepaymentStatus.OVERDUE
        );
        scheduleRepo.add(item1);
        scheduleRepo.add(item2);
        scheduleRepo.add(item3);

        // 3. Пошук за унікальним полем (findByIdentity)
        System.out.println("\n=== Пошук за identity ===");
        contractRepo.findByIdentity("LN-2026-01")
                .ifPresent(c -> System.out.println("Знайдено договір: " + c
                        + " | Ризик-коефіцієнт: " + BankUtils.getRiskCoefficient(c.loanType())));

        System.out.println("Пошук неіснуючого договору 'LN-999': "
                + contractRepo.findByIdentity("LN-999").isPresent());

        // 4. Демонстрація роботи з дубльованими даними
        System.out.println("\n=== Демонстрація дубльованих даних ===");
        BankBranch duplicateBranch = BankBranch.of("BR-001", "Інше Місто (дублікат коду)");
        boolean addedDuplicateBranch = branchRepo.add(duplicateBranch);
        System.out.println("Чи додано дублікат відділення BR-001: " + addedDuplicateBranch);
        System.out.println("Актуальне відділення BR-001 у репозиторії: "
                + branchRepo.findByIdentity("BR-001").orElseThrow());

        Borrower duplicateBorrower = new Borrower(
                "AA111222", "Інший ПІБ Дублікат", LocalDate.of(1990, 1, 1), 50000.0
        );
        boolean addedDuplicateBorrower = borrowerRepo.add(duplicateBorrower);
        System.out.println("Чи додано дублікат позичальника AA111222: " + addedDuplicateBorrower);

        // 5. Видалення та отримання всіх елементів
        System.out.println("\n=== Видалення та список усіх елементів ===");
        scheduleRepo.remove(item3);
        System.out.println("Залишилось платежів у графіку: " + scheduleRepo.getAll().size());
        scheduleRepo.getAll().forEach(p ->
                System.out.println(" - " + p + " -> " + BankUtils.getAvailableAction(p.getStatus())));

        log.info("=== Роботу програми успішно завершено ===");
    }
}