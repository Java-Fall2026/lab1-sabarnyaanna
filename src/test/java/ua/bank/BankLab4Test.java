package ua.bank;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.bank.model.BankBranch;
import ua.bank.model.Borrower;
import ua.bank.model.LoanContract;
import ua.bank.model.LoanType;
import ua.bank.model.RepaymentScheduleItem;
import ua.bank.model.RepaymentStatus;
import ua.bank.repository.GenericRepository;

class BankLab4Test {

    private GenericRepository<BankBranch> branchRepo;
    private GenericRepository<Borrower> borrowerRepo;
    private GenericRepository<LoanContract> contractRepo;

    @BeforeEach
    void setUp() {
        branchRepo = new GenericRepository<>("BankBranch", BankBranch::branchCode);
        borrowerRepo = new GenericRepository<>("Borrower", Borrower::getPassportNum);
        contractRepo = new GenericRepository<>("LoanContract", LoanContract::contractId);
    }

    @Test
    @DisplayName("1. Успішне додавання та отримання всіх елементів різних типів")
    void shouldAddAndRetrieveAllItems() {
        BankBranch b1 = BankBranch.of("BR-01", "Київ");
        BankBranch b2 = BankBranch.of("BR-02", "Чернівці");

        assertTrue(branchRepo.add(b1));
        assertTrue(branchRepo.add(b2));
        assertEquals(2, branchRepo.size());

        List<BankBranch> all = branchRepo.getAll();
        assertEquals(List.of(b1, b2), all);
        assertThrows(UnsupportedOperationException.class, () -> all.add(BankBranch.of("BR-03", "Львів")));
    }

    @Test
    @DisplayName("2. Пошук об'єкта за унікальним полем findByIdentity (знайдено / не знайдено)")
    void shouldFindItemByIdentity() {
        Borrower borrower = new Borrower("AA123456", "Олена Ковальчук", LocalDate.of(1995, 5, 14), 35000.0);
        borrowerRepo.add(borrower);

        Optional<Borrower> found = borrowerRepo.findByIdentity("  AA123456  ");
        assertTrue(found.isPresent());
        assertEquals("Олена Ковальчук", found.get().getFullName());

        assertTrue(borrowerRepo.findByIdentity("UNKNOWN").isEmpty());
        assertTrue(borrowerRepo.findByIdentity("   ").isEmpty());
    }

    @Test
    @DisplayName("3. Відхилення дубльованих даних за унікальним identity")
    void shouldRejectDuplicateIdentity() {
        BankBranch original = BankBranch.of("BR-10", "Київ");
        BankBranch duplicate = BankBranch.of("BR-10", "Одеса");

        assertTrue(branchRepo.add(original));
        assertFalse(branchRepo.add(duplicate), "Дублікат за branchCode не має додаватися");
        assertEquals(1, branchRepo.size());
        assertEquals("Київ", branchRepo.findByIdentity("BR-10").orElseThrow().city());
    }

    @Test
    @DisplayName("4. Видалення елементів за identity та за об'єктом")
    void shouldRemoveItemsCorrectly() {
        Borrower borrower = new Borrower("BB111222", "Іван Петренко", LocalDate.of(1990, 3, 10), 40000.0);
        BankBranch branch = BankBranch.of("BR-05", "Львів");
        LoanContract contract = new LoanContract(
                "LN-500", borrower, branch, LoanType.CONSUMER,
                100000.0, LocalDate.of(2026, 9, 1), 12
        );

        contractRepo.add(contract);
        assertEquals(1, contractRepo.size());

        assertTrue(contractRepo.remove(contract));
        assertEquals(0, contractRepo.size());
        assertFalse(contractRepo.removeByIdentity("LN-500"));
    }

    @Test
    @DisplayName("5. Робота репозиторію з композитним identity для RepaymentScheduleItem")
    void shouldWorkWithCompositeIdentityExtractor() {
        Borrower borrower = new Borrower("CC333444", "Марія Сидоренко", LocalDate.of(1992, 7, 20), 45000.0);
        BankBranch branch = BankBranch.of("BR-07", "Дніпро");
        LoanContract contract = new LoanContract(
                "LN-700", borrower, branch, LoanType.COMMERCIAL,
                200000.0, LocalDate.of(2026, 8, 1), 10
        );

        GenericRepository<RepaymentScheduleItem> scheduleRepo = new GenericRepository<>(
                "RepaymentScheduleItem",
                item -> item.getContract().contractId() + "_" + item.getDueDate()
        );

        RepaymentScheduleItem item = RepaymentScheduleItem.of(
                contract, LocalDate.of(2026, 9, 1), 20000.0, RepaymentStatus.PENDING
        );
        RepaymentScheduleItem duplicateItem = RepaymentScheduleItem.of(
                contract, LocalDate.of(2026, 9, 1), 25000.0, RepaymentStatus.PAID
        );

        assertTrue(scheduleRepo.add(item));
        assertFalse(scheduleRepo.add(duplicateItem));
        assertTrue(scheduleRepo.findByIdentity("LN-700_2026-09-01").isPresent());
    }

    @Test
    @DisplayName("6. Викидання IllegalArgumentException при передачі null у репозиторій")
    void shouldThrowExceptionWhenAddingNullItem() {
        assertThrows(IllegalArgumentException.class, () -> branchRepo.add(null));
    }
}