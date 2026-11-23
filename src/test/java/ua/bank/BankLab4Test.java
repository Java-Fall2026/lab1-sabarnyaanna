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
import ua.bank.repository.DuplicateIdentityException;
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
    @DisplayName("1. Should add items of different entity types and verify size()")
    void shouldAddItemsOfDifferentTypes() {
        BankBranch branch = BankBranch.of("BR-01", "Київ");
        Borrower borrower = new Borrower("AA123456", "Олена Ковальчук", LocalDate.of(1995, 5, 14), 35000.0);
        LoanContract contract = new LoanContract(
                "LN-100", borrower, branch, LoanType.MORTGAGE,
                240000.0, LocalDate.of(2026, 1, 15), 12
        );

        branchRepo.add(branch);
        borrowerRepo.add(borrower);
        contractRepo.add(contract);

        assertEquals(1, branchRepo.size());
        assertEquals(1, borrowerRepo.size());
        assertEquals(1, contractRepo.size());
    }

    @Test
    @DisplayName("2. Should throw DuplicateIdentityException with repository name and identity on duplicate add()")
    void shouldThrowDuplicateIdentityExceptionOnDuplicateAdd() {
        branchRepo.add(BankBranch.of("BR-10", "Київ"));

        DuplicateIdentityException ex = assertThrows(
                DuplicateIdentityException.class,
                () -> branchRepo.add(BankBranch.of("BR-10", "Одеса"))
        );

        assertTrue(ex.getMessage().contains("BR-10"));
        assertTrue(ex.getMessage().contains("BankBranch"));
        assertEquals("BR-10", ex.getIdentity());
        assertEquals("BankBranch", ex.getRepositoryName());
    }

    @Test
    @DisplayName("3. Should add unique item and return false on duplicate in addIfAbsent() without changing size()")
    void shouldHandleAddIfAbsentAndMaintainSize() {
        Borrower original = new Borrower("AA111222", "Іван Петренко", LocalDate.of(1990, 3, 10), 40000.0);
        Borrower duplicate = new Borrower("AA111222", "Дублікат Іван", LocalDate.of(1990, 3, 10), 50000.0);

        assertTrue(borrowerRepo.addIfAbsent(original));
        assertEquals(1, borrowerRepo.size());

        assertFalse(borrowerRepo.addIfAbsent(duplicate));
        assertEquals(1, borrowerRepo.size());
        assertEquals("Іван Петренко", borrowerRepo.findByIdentity("AA111222").orElseThrow().getFullName());
    }

    @Test
    @DisplayName("4. Should add batch via addAll() skipping duplicates and returning added count")
    void shouldAddAllSkippingDuplicates() {
        List<BankBranch> batch = List.of(
                BankBranch.of("BR-01", "Київ"),
                BankBranch.of("BR-02", "Львів"),
                BankBranch.of("BR-01", "Київ Дублікат")
        );

        int added = branchRepo.addAll(batch);

        assertEquals(2, added);
        assertEquals(2, branchRepo.size());
    }

    @Test
    @DisplayName("5. Should find existing item by identity and return empty Optional when not found")
    void shouldFindByIdentityAndReturnEmptyWhenMissing() {
        Borrower borrower = new Borrower("CC333444", "Марія Сидоренко", LocalDate.of(1992, 7, 20), 45000.0);
        borrowerRepo.add(borrower);

        Optional<Borrower> found = borrowerRepo.findByIdentity("  CC333444  ");
        assertTrue(found.isPresent());
        assertEquals("Марія Сидоренко", found.get().getFullName());

        assertTrue(borrowerRepo.findByIdentity("MISSING-ID").isEmpty());
        assertTrue(borrowerRepo.findByIdentity("   ").isEmpty());
    }

    @Test
    @DisplayName("6. Should remove existing item and return false when removing non-existent identity")
    void shouldRemoveExistingAndReturnFalseForMissingItem() {
        BankBranch branch = BankBranch.of("BR-05", "Чернівці");
        branchRepo.add(branch);

        assertTrue(branchRepo.remove(branch));
        assertEquals(0, branchRepo.size());

        assertFalse(branchRepo.removeByIdentity("BR-05"));
        assertFalse(branchRepo.removeByIdentity("NON-EXISTENT"));
    }

    @Test
    @DisplayName("7. Should return unmodifiable list from findAll()")
    void shouldReturnUnmodifiableListFromFindAll() {
        BankBranch b1 = BankBranch.of("BR-01", "Київ");
        BankBranch b2 = BankBranch.of("BR-02", "Одеса");
        branchRepo.add(b1);
        branchRepo.add(b2);

        List<BankBranch> all = branchRepo.findAll();
        assertEquals(List.of(b1, b2), all);
        assertThrows(UnsupportedOperationException.class, () -> all.add(BankBranch.of("BR-03", "Дніпро")));
        assertThrows(UnsupportedOperationException.class, () -> all.remove(0));
    }

    @Test
    @DisplayName("8. Should throw IllegalArgumentException when null is passed to add(), addIfAbsent(), or addAll()")
    void shouldThrowIllegalArgumentExceptionOnNullInputs() {
        assertThrows(IllegalArgumentException.class, () -> branchRepo.add(null));
        assertThrows(IllegalArgumentException.class, () -> branchRepo.addIfAbsent(null));
        assertThrows(IllegalArgumentException.class, () -> branchRepo.addAll(null));
    }
}