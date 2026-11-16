package ua.bank;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.bank.model.BankBranch;
import ua.bank.model.Borrower;
import ua.bank.model.LoanContract;
import ua.bank.model.LoanType;
import ua.bank.repository.BorrowerRepository;
import ua.bank.repository.LoanContractRepository;
import ua.bank.repository.SortOrder;

class BankLab5Test {

    private BorrowerRepository borrowerRepo;
    private LoanContractRepository contractRepo;

    private Borrower b1;
    private Borrower b2;
    private Borrower b3;

    @BeforeEach
    void setUp() {
        borrowerRepo = new BorrowerRepository();
        contractRepo = new LoanContractRepository();

        b1 = new Borrower("CC300", "Олена Ковальчук", LocalDate.of(1995, 5, 14), 35000.50);
        b2 = new Borrower("AA100", "Тарас Бондар", LocalDate.of(1988, 9, 22), 55000.75);
        b3 = new Borrower("BB200", "Наталія Ткачук", LocalDate.of(1992, 11, 30), 55000.75);

        borrowerRepo.add(b1);
        borrowerRepo.add(b2);
        borrowerRepo.add(b3);

        BankBranch kyiv = BankBranch.of("BR-01", "Київ");
        BankBranch lviv = BankBranch.of("BR-02", "Львів");

        contractRepo.add(new LoanContract("LN-02", b1, kyiv, LoanType.MORTGAGE, 600000.50, LocalDate.of(2026, 5, 1), 120));
        contractRepo.add(new LoanContract("LN-01", b2, lviv, LoanType.AUTO, 400000.25, LocalDate.of(2026, 6, 1), 36));
        contractRepo.add(new LoanContract("LN-03", b3, kyiv, LoanType.MORTGAGE, 600000.50, LocalDate.of(2026, 7, 1), 60));
    }

    @Test
    @DisplayName("1. Природний порядок (Comparable) узгоджений з equals")
    void shouldSortByNaturalOrderConsistentWithEquals() {
        List<Borrower> sorted = borrowerRepo.findAllSortedByNaturalOrder();
        assertEquals(List.of(b2, b3, b1), sorted); // AA100, BB200, CC300

        Borrower b2Copy = new Borrower("AA100", "Інше Ім'я", LocalDate.of(1990, 1, 1), 20000.0);
        assertEquals(0, b2.compareTo(b2Copy));
        assertEquals(b2, b2Copy);
    }

    @Test
    @DisplayName("2. Багаторівневе сортування з reversed та thenComparing + порівняння дробових чисел")
    void shouldPerformMultiLevelSortingWithFractionalNumbers() {
        List<Borrower> sortedBorrowers = borrowerRepo.findAllSortedByIncomeDescThenName();
        // b2 і b3 мають однаковий дохід 55000.75 -> сортуються за ПІБ: Наталія Ткачук (b3), потім Тарас Бондар (b2)
        assertEquals(List.of(b3, b2, b1), sortedBorrowers);

        List<LoanContract> sortedContracts = contractRepo.findAllSortedByAmountDescThenTermAsc();
        // LN-03 (600000.50, 60 міс.) -> LN-02 (600000.50, 120 міс.) -> LN-01 (400000.25, 36 міс.)
        assertEquals("LN-03", sortedContracts.get(0).contractId());
        assertEquals("LN-02", sortedContracts.get(1).contractId());
        assertEquals("LN-01", sortedContracts.get(2).contractId());
    }

    @Test
    @DisplayName("3. Сортування за identity в обидва боки (SortOrder.ASC та SortOrder.DESC)")
    void shouldSortByIdentityAscAndDesc() {
        List<LoanContract> asc = contractRepo.findAllSortedByIdentity(SortOrder.ASC);
        assertEquals(List.of("LN-01", "LN-02", "LN-03"),
                asc.stream().map(LoanContract::contractId).toList());

        List<LoanContract> desc = contractRepo.findAllSortedByIdentity(SortOrder.DESC);
        assertEquals(List.of("LN-03", "LN-02", "LN-01"),
                desc.stream().map(LoanContract::contractId).toList());
    }

    @Test
    @DisplayName("4. Поведінка TreeSet з компаратором, неузгодженим з equals")
    void shouldDemonstrateTreeSetBehaviorWithInconsistentComparator() {
        Set<Borrower> treeSet = new TreeSet<>(Comparator.comparingDouble(Borrower::getMonthlyIncome));
        assertTrue(treeSet.add(b2));
        // b3 має інший паспорт (equals == false), але той самий дохід 55000.75 (compare == 0)
        assertFalse(treeSet.add(b3), "TreeSet не додає елемент, якщо comparator повертає 0");
        assertEquals(1, treeSet.size());
    }

    @Test
    @DisplayName("5. Пошук за назвою, діапазоном, кількома умовами та distinct")
    void shouldSearchByNameRangeMultipleConditionsAndDistinct() {
        assertEquals(1, borrowerRepo.searchByNameContaining("ковальчук").size());
        assertEquals(2, borrowerRepo.findByIncomeRange(50000.0, 60000.0).size());
        assertEquals(1, borrowerRepo.findTopEarners(1).size());

        Optional<LoanContract> multiMatch = contractRepo.findFirstByTypeCityAndMinDate(
                LoanType.MORTGAGE, "Київ", LocalDate.of(2026, 6, 1)
        );
        assertTrue(multiMatch.isPresent());
        assertEquals("LN-03", multiMatch.get().contractId());

        assertEquals(List.of("Київ", "Львів"), contractRepo.findDistinctBranchCities());
    }

    @Test
    @DisplayName("6. Групування за типом кредиту (groupingBy) та підрахунок кількості")
    void shouldGroupAndCountByLoanType() {
        Map<LoanType, List<LoanContract>> grouped = contractRepo.groupByLoanType();
        assertEquals(2, grouped.get(LoanType.MORTGAGE).size());
        assertEquals(1, grouped.get(LoanType.AUTO).size());

        Map<LoanType, Long> counts = contractRepo.countByLoanType();
        assertEquals(2L, counts.get(LoanType.MORTGAGE));
        assertEquals(1L, counts.get(LoanType.AUTO));
    }

    @Test
    @DisplayName("7. Агрегація на непорожній та порожній колекції")
    void shouldAggregateOnPopulatedAndEmptyRepositories() {
        assertEquals(1600001.25, contractRepo.calculateTotalPrincipal(), 0.001);

        LoanContractRepository emptyContracts = new LoanContractRepository();
        assertEquals(0.0, emptyContracts.calculateTotalPrincipal(), 0.001);

        BorrowerRepository emptyBorrowers = new BorrowerRepository();
        DoubleSummaryStatistics emptyStats = emptyBorrowers.getIncomeStatistics();
        assertEquals(0L, emptyStats.getCount());
        assertEquals(0.0, emptyStats.getSum(), 0.001);
    }

    @Test
    @DisplayName("8. Повернення Optional.empty() при відсутності збігів")
    void shouldReturnOptionalEmptyWhenNotFound() {
        Optional<Borrower> notFoundBorrower = borrowerRepo.findFirstWithIncomeAbove(100_000.0, "Неіснуючий");
        assertTrue(notFoundBorrower.isEmpty());

        Optional<LoanContract> notFoundContract = contractRepo.findFirstByTypeCityAndMinDate(
                LoanType.COMMERCIAL, "Харків", LocalDate.of(2026, 1, 1)
        );
        assertEquals(Optional.empty(), notFoundContract);
    }

    @Test
    @DisplayName("9. Викидання IllegalStateException при повторному використанні спожитого Stream")
    void shouldThrowIllegalStateExceptionWhenReusingConsumedStream() {
        Stream<Borrower> stream = borrowerRepo.stream();
        assertEquals(3L, stream.count());

        assertThrows(IllegalStateException.class, stream::toList);
    }
}