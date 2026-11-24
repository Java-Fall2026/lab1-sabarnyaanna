package ua.bank;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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

/**
 * Unit tests for Lab 5: Comparable/Comparator sorting, Stream filtering,
 * boundary conditions, grouping, and aggregation.
 */
class BankLab5Test {

    private BorrowerRepository borrowerRepo;
    private LoanContractRepository contractRepo;
    private Borrower b1;
    private Borrower b2;
    private Borrower b3;
    private BankBranch kyiv;
    private BankBranch lviv;

    @BeforeEach
    void setUp() {
        borrowerRepo = new BorrowerRepository();
        contractRepo = new LoanContractRepository();

        b1 = new Borrower("AA100001", "Олена Ковальчук", LocalDate.of(1995, 5, 14), 30000.0);
        b2 = new Borrower("BB200002", "Тарас Бондар", LocalDate.of(1988, 9, 22), 50000.0);
        b3 = new Borrower("CC300003", "Іван Петренко", LocalDate.of(1992, 1, 10), 40000.0);

        borrowerRepo.add(b1);
        borrowerRepo.add(b2);
        borrowerRepo.add(b3);

        kyiv = BankBranch.of("BR-01", "Київ");
        lviv = BankBranch.of("BR-02", "Львів");

        contractRepo.add(new LoanContract("LN-2026-02", b1, kyiv, LoanType.MORTGAGE, 750000.0, LocalDate.of(2026, 2, 1), 120));
        contractRepo.add(new LoanContract("LN-2026-01", b2, lviv, LoanType.AUTO, 450000.0, LocalDate.of(2026, 1, 15), 36));
        contractRepo.add(new LoanContract("LN-2026-03", b3, kyiv, LoanType.MORTGAGE, 900000.0, LocalDate.of(2026, 3, 10), 180));
    }

    @Test
    @DisplayName("1. Should compare LoanContracts across all fields including sub-unit principal difference (< 1.0)")
    void shouldCompareLoanContractsAcrossAllFieldsAndSubUnitAmount() {
        LoanContract base = new LoanContract("LN-2026-10", b1, kyiv, LoanType.MORTGAGE, 500000.25, LocalDate.of(2026, 5, 1), 60);
        LoanContract higherByFraction = new LoanContract("LN-2026-10", b1, kyiv, LoanType.MORTGAGE, 500000.75, LocalDate.of(2026, 5, 1), 60);
        LoanContract shorterTerm = new LoanContract("LN-2026-10", b1, kyiv, LoanType.MORTGAGE, 500000.75, LocalDate.of(2026, 5, 1), 24);
        LoanContract differentBranch = new LoanContract("LN-2026-10", b1, lviv, LoanType.MORTGAGE, 500000.25, LocalDate.of(2026, 5, 1), 60);

        assertTrue(base.compareTo(higherByFraction) < 0,
                "Sub-unit difference (0.25 vs 0.75) must be preserved in compareTo");
        assertTrue(higherByFraction.compareTo(base) > 0);
        assertTrue(shorterTerm.compareTo(higherByFraction) < 0,
                "termMonths must be compared when earlier fields match");
        assertTrue(base.compareTo(differentBranch) < 0,
                "branch must be compared when contractId and borrower match");
        assertEquals(0, base.compareTo(new LoanContract("LN-2026-10", b1, kyiv, LoanType.MORTGAGE, 500000.25, LocalDate.of(2026, 5, 1), 60)));
    }

    @Test
    @DisplayName("2. Should sort LoanContracts via findAllSortedByNaturalOrder without ClassCastException")
    void shouldSortContractsByNaturalOrder() {
        List<LoanContract> sorted = contractRepo.findAllSortedByNaturalOrder();
        assertEquals(List.of("LN-2026-01", "LN-2026-02", "LN-2026-03"),
                sorted.stream().map(LoanContract::contractId).toList());
    }

    @Test
    @DisplayName("3. Should sort entities by identity in ASC and DESC order")
    void shouldSortByIdentityAscAndDesc() {
        List<String> ascIds = borrowerRepo.findAllSortedByIdentity(SortOrder.ASC).stream()
                .map(Borrower::getPassportNum)
                .toList();
        List<String> descIds = borrowerRepo.findAllSortedByIdentity(SortOrder.DESC).stream()
                .map(Borrower::getPassportNum)
                .toList();

        assertEquals(List.of("AA100001", "BB200002", "CC300003"), ascIds);
        assertEquals(List.of("CC300003", "BB200002", "AA100001"), descIds);
    }

    @Test
    @DisplayName("4. Should sort borrowers by monthly income using custom Comparator")
    void shouldSortBorrowersByIncome() {
        List<Borrower> desc = borrowerRepo.findAllSortedByIncome(SortOrder.DESC);
        assertEquals(List.of(b2, b3, b1), desc);
    }

    @Test
    @DisplayName("5. Should include exact lower and upper boundaries in income range search")
    void shouldIncludeExactBoundariesInIncomeRange() {
        List<Borrower> inRange = borrowerRepo.findByIncomeRange(30000.0, 40000.0);
        assertEquals(2, inRange.size());
        assertTrue(inRange.containsAll(List.of(b1, b3)));
        assertFalse(inRange.contains(b2));
    }

    @Test
    @DisplayName("6. Should handle object matching only one of two combined search conditions")
    void shouldHandleObjectMatchingOnlyOneCondition() {
        Borrower b4 = new Borrower("DD400004", "Тарас Шевченко", LocalDate.of(1990, 3, 9), 32000.0);
        borrowerRepo.add(b4);

        Set<String> byNameIds = borrowerRepo.searchByNameContaining("Тарас").stream()
                .map(Borrower::getPassportNum)
                .collect(Collectors.toSet());
        List<Borrower> combined = borrowerRepo.findByIncomeRange(25000.0, 35000.0).stream()
                .filter(b -> byNameIds.contains(b.getPassportNum()))
                .toList();

        assertEquals(List.of(b4), combined);
        assertFalse(combined.contains(b1));
        assertFalse(combined.contains(b2));
    }

    @Test
    @DisplayName("7. Should perform case-insensitive substring text search on borrower name")
    void shouldSearchBorrowersByNameSubstring() {
        List<Borrower> found = borrowerRepo.searchByNameContaining("КОВАЛЬ");
        assertEquals(List.of(b1), found);
    }

    @Test
    @DisplayName("8. Should group contracts by LoanType and count per type")
    void shouldGroupAndCountContractsByLoanType() {
        Map<LoanType, List<LoanContract>> grouped = contractRepo.groupByLoanType();
        Map<LoanType, Long> counts = contractRepo.countByLoanType();

        assertEquals(2, grouped.get(LoanType.MORTGAGE).size());
        assertEquals(1, grouped.get(LoanType.AUTO).size());
        assertEquals(2L, counts.get(LoanType.MORTGAGE));
        assertEquals(1L, counts.get(LoanType.AUTO));
    }

    @Test
    @DisplayName("9. Should calculate total principal amount and borrower income statistics")
    void shouldCalculateAggregations() {
        assertEquals(2100000.0, contractRepo.getTotalPrincipalAmount(), 0.001);

        DoubleSummaryStatistics stats = borrowerRepo.getIncomeStatistics();
        assertEquals(3, stats.getCount());
        assertEquals(30000.0, stats.getMin(), 0.001);
        assertEquals(50000.0, stats.getMax(), 0.001);
        assertEquals(40000.0, stats.getAverage(), 0.001);
    }

    @Test
    @DisplayName("10. Should return empty Optional and empty List when search yields no matches")
    void shouldReturnEmptyResultsWhenNoMatchesFound() {
        assertTrue(borrowerRepo.findByIdentity("ZZ999999").isEmpty());
        assertTrue(borrowerRepo.searchByNameContaining("НеіснуючеІм'я").isEmpty());
        assertTrue(contractRepo.findByPrincipalRange(10_000_000.0, 20_000_000.0).isEmpty());
    }
}