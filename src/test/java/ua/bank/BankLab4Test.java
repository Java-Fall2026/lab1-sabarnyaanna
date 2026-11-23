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
import ua.bank.repository.DuplicateIdentityException;
import ua.bank.repository.GenericRepository;

class BankLab4Test {

    private GenericRepository<BankBranch> branchRepo;
    private GenericRepository<Borrower> borrowerRepo;
    private GenericRepository<LoanContract> contractRepo;
    private GenericRepository<RepaymentScheduleItem> scheduleRepo;

    @BeforeEach
    void setUp() {
        branchRepo = new GenericRepository<>("BankBranch", BankBranch::branchCode);
        borrowerRepo = new GenericRepository<>("Borrower", Borrower::getPassportNum);
        contractRepo = new GenericRepository<>("LoanContract", LoanContract::contractId);
        scheduleRepo = new GenericRepository<>("RepaymentScheduleItem",
                item -> item.getContract().contractId() + "_" + item.getDueDate());
    }

    @Test
    @DisplayName("1. Should add items and return unmodifiable list from findAll()")
    void shouldAddItemsAndReturnUnmodifiableListFromFindAll() {
        BankBranch b1 = BankBranch.of("BR-01", "Київ");
        BankBranch b2 = BankBranch.of("BR-02", "Львів");

        branchRepo.add(b1);
        branchRepo.add(b2);

        List<BankBranch> all = branchRepo.findAll();
        assertEquals(List.of(b1, b2), all);
        assertThrows(UnsupportedOperationException.class, () -> all.add(BankBranch.of("BR-03", "Одеса")));
    }

    @Test
    @DisplayName("2. Should throw DuplicateIdentityException on duplicate add() and return false on addIfAbsent()")
    void shouldHandleDuplicateAddAndAddIfAbsent() {
        BankBranch original = BankBranch.of("BR-01", "Київ");
        BankBranch duplicate = BankBranch.of("BR-01", "Київ Інше Відділення");

        assertTrue(branchRepo.addIfAbsent(original));
        assertFalse(branchRepo.addIfAbsent(duplicate));
        assertThrows(DuplicateIdentityException.class, () -> branchRepo.add(duplicate));
    }

    @Test
    @DisplayName("3. Should find by identity and remove existing or missing items")
    void shouldFindAndRemoveItems() {
        Borrower borrower = new Borrower("AA123456", "Олена Ковальчук", LocalDate.of(1995, 5, 14), 35000.0);
        BankBranch branch = BankBranch.of("BR-01", "Київ");
        LoanContract contract = new LoanContract("LN-2026-01", borrower, branch, LoanType.MORTGAGE,
                240000.0, LocalDate.of(2026, 1, 15), 12);
        RepaymentScheduleItem item = RepaymentScheduleItem.of(contract, LocalDate.of(2026, 2, 15), 20000.0, RepaymentStatus.PENDING);
        RepaymentScheduleItem duplicateItem = RepaymentScheduleItem.of(contract, LocalDate.of(2026, 2, 15), 21000.0, RepaymentStatus.PAID);

        assertTrue(scheduleRepo.addIfAbsent(item));
        assertFalse(scheduleRepo.addIfAbsent(duplicateItem));

        Optional<RepaymentScheduleItem> found = scheduleRepo.findByIdentity("LN-2026-01_2026-02-15");
        assertTrue(found.isPresent());
        assertTrue(scheduleRepo.remove(item));
        assertFalse(scheduleRepo.removeByIdentity("LN-2026-01_2026-02-15"));
    }
}