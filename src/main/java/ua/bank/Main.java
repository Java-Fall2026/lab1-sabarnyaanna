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
import ua.bank.repository.DuplicateIdentityException;
import ua.bank.repository.GenericRepository;
import ua.bank.util.BankUtils;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Starting application (Lab 4: GenericRepository and Collections)");

        GenericRepository<BankBranch> branchRepo =
                new GenericRepository<>("BankBranch", BankBranch::branchCode);
        GenericRepository<Borrower> borrowerRepo =
                new GenericRepository<>("Borrower", Borrower::getPassportNum);
        GenericRepository<LoanContract> contractRepo =
                new GenericRepository<>("LoanContract", LoanContract::contractId);
        GenericRepository<RepaymentScheduleItem> scheduleRepo =
                new GenericRepository<>("RepaymentScheduleItem",
                        item -> item.getContract().contractId() + "_" + item.getDueDate());

        // 1. Завантаження об'єктів із CSV (включно з дублікатом у файлі) через addAll
        List<BankBranch> csvBranches = BankCsvReader.readBranches("data/branches.csv");
        int addedBranches = branchRepo.addAll(csvBranches);
        System.out.printf("Відділення з CSV: прочитано=%d, додано унікальних=%d%n",
                csvBranches.size(), addedBranches);

        List<Borrower> csvBorrowers = BankCsvReader.readBorrowers("data/borrowers.csv");
        int addedBorrowers = borrowerRepo.addAll(csvBorrowers);
        System.out.printf("Позичальники з CSV: прочитано=%d, додано унікальних=%d%n",
                csvBorrowers.size(), addedBorrowers);

        Borrower b1 = borrowerRepo.findAll().get(0);
        BankBranch br1 = branchRepo.findAll().get(0);

        LoanContract contract1 = new LoanContract(
                "LN-2026-01", b1, br1, LoanType.MORTGAGE,
                500000.0, LocalDate.of(2026, 10, 1), 24
        );
        contractRepo.add(contract1);

        RepaymentScheduleItem item1 = RepaymentScheduleItem.of(
                contract1, LocalDate.of(2026, 11, 1),
                BankUtils.monthlyPayment(contract1), RepaymentStatus.PENDING
        );
        scheduleRepo.add(item1);

        // 2. Пошук знайдено / не знайдено для всіх трьох репозиторіїв
        System.out.println("\n=== Пошук за identity (знайдено / не знайдено) ===");
        System.out.println("BankBranch 'BR-001' знайдено: " + branchRepo.findByIdentity("BR-001").isPresent());
        System.out.println("BankBranch 'BR-999' знайдено: " + branchRepo.findByIdentity("BR-999").isPresent());

        System.out.println("Borrower '" + b1.getPassportNum() + "' знайдено: "
                + borrowerRepo.findByIdentity(b1.getPassportNum()).isPresent());
        System.out.println("Borrower 'ZZ000000' знайдено: "
                + borrowerRepo.findByIdentity("ZZ000000").isPresent());

        System.out.println("LoanContract 'LN-2026-01' знайдено: "
                + contractRepo.findByIdentity("LN-2026-01").isPresent());
        System.out.println("LoanContract 'LN-MISSING' знайдено: "
                + contractRepo.findByIdentity("LN-MISSING").isPresent());

        // 3. Демонстрація add (кидає DuplicateIdentityException) проти addIfAbsent (повертає false)
        System.out.println("\n=== Дублікати: add() з виключенням проти addIfAbsent() ===");
        BankBranch duplicateBranch = BankBranch.of("BR-001", "Інше Місто");
        try {
            branchRepo.add(duplicateBranch);
        } catch (DuplicateIdentityException e) {
            System.out.println("Перехоплено виключення при add(): " + e.getMessage());
        }

        boolean addedViaIfAbsent = branchRepo.addIfAbsent(duplicateBranch);
        System.out.println("Результат addIfAbsent() на тому ж дублікаті: " + addedViaIfAbsent);

        // 4. Видалення існуючого та відсутнього елемента
        System.out.println("\n=== Видалення елементів ===");
        System.out.println("Видалення існуючого платежу: " + scheduleRepo.remove(item1));
        System.out.println("Видалення відсутнього договору 'LN-NOT-EXISTS': "
                + contractRepo.removeByIdentity("LN-NOT-EXISTS"));

        log.info("Application finished successfully");
    }
}