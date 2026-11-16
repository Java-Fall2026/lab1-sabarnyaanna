package ua.bank.repository;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.bank.model.LoanContract;
import ua.bank.model.LoanType;

public class LoanContractRepository extends GenericRepository<LoanContract> {

    private static final Logger log = LoggerFactory.getLogger(LoanContractRepository.class);

    public LoanContractRepository() {
        super("LoanContract", LoanContract::contractId);
    }

    // --- Сортування ---

    public List<LoanContract> findAllSortedByNaturalOrder() {
        List<LoanContract> result = stream().sorted().toList();
        log.info("[LoanContract] Sorted by natural order -> count={}", result.size());
        return result;
    }

    /**
     * Багаторівневе сортування: за сумою кредиту за спаданням (reversed), потім за терміном за зростанням.
     */
    public List<LoanContract> findAllSortedByAmountDescThenTermAsc() {
        Comparator<LoanContract> comparator = Comparator
                .comparingDouble(LoanContract::principalAmount)
                .reversed()
                .thenComparingInt(LoanContract::termMonths);
        List<LoanContract> result = findAllSorted(comparator);
        log.info("[LoanContract] Sorted by principalAmount DESC, termMonths ASC -> count={}", result.size());
        return result;
    }

    /**
     * Сортування за типом кредиту та датою видачі.
     */
    public List<LoanContract> findAllSortedByTypeThenIssueDate() {
        Comparator<LoanContract> comparator = Comparator
                .comparing(LoanContract::loanType)
                .thenComparing(LoanContract::issueDate);
        List<LoanContract> result = findAllSorted(comparator);
        log.info("[LoanContract] Sorted by loanType ASC, issueDate ASC -> count={}", result.size());
        return result;
    }

    // --- Пошук через Stream API ---

    public List<LoanContract> searchByBorrowerName(String borrowerNamePart) {
        if (borrowerNamePart == null || borrowerNamePart.trim().isEmpty()) {
            log.info("[LoanContract] Search by borrowerName='' -> count=0");
            return List.of();
        }
        String query = borrowerNamePart.trim().toLowerCase();
        List<LoanContract> result = stream()
                .filter(c -> c.borrower().getFullName().toLowerCase().contains(query))
                .toList();
        log.info("[LoanContract] Search by borrower name containing '{}' -> count={}", query, result.size());
        return result;
    }

    public List<LoanContract> findByPrincipalRange(double minAmount, double maxAmount) {
        List<LoanContract> result = stream()
                .filter(c -> Double.compare(c.principalAmount(), minAmount) >= 0
                        && Double.compare(c.principalAmount(), maxAmount) <= 0)
                .sorted(Comparator.comparingDouble(LoanContract::principalAmount))
                .toList();
        log.info("[LoanContract] Search by principalAmount in [{}, {}] -> count={}",
                minAmount, maxAmount, result.size());
        return result;
    }

    /**
     * Пошук за кількома умовами одночасно: тип кредиту, місто відділення та дата видачі не раніше minDate.
     */
    public Optional<LoanContract> findFirstByTypeCityAndMinDate(LoanType type, String city, LocalDate minDate) {
        Optional<LoanContract> found = stream()
                .filter(c -> c.loanType() == type)
                .filter(c -> c.branch().city().equalsIgnoreCase(city))
                .filter(c -> !c.issueDate().isBefore(minDate))
                .findFirst();
        log.info("[LoanContract] Search first by type={}, city='{}', minDate={} -> found={}",
                type, city, minDate, found.isPresent());
        return found;
    }

    /**
     * Використання map + distinct + sorted: унікальні міста відділень, у яких видано кредити.
     */
    public List<String> findDistinctBranchCities() {
        List<String> cities = stream()
                .map(c -> c.branch().city())
                .distinct()
                .sorted()
                .toList();
        log.info("[LoanContract] Distinct branch cities -> count={}", cities.size());
        return cities;
    }

    // --- Групування та агрегація ---

    public Map<LoanType, List<LoanContract>> groupByLoanType() {
        Map<LoanType, List<LoanContract>> grouped = stream()
                .collect(Collectors.groupingBy(LoanContract::loanType));
        log.info("[LoanContract] Grouped contracts by LoanType -> groups={}", grouped.size());
        return grouped;
    }

    public Map<LoanType, Long> countByLoanType() {
        Map<LoanType, Long> counts = stream()
                .collect(Collectors.groupingBy(LoanContract::loanType, Collectors.counting()));
        log.info("[LoanContract] Counted contracts by LoanType -> groups={}", counts.size());
        return counts;
    }

    /**
     * Зведення загальної суми всіх кредитів через reduce (на порожній колекції безпечно повертає 0.0).
     */
    public double calculateTotalPrincipal() {
        double total = stream()
                .map(LoanContract::principalAmount)
                .reduce(0.0, Double::sum);
        log.info("[LoanContract] Calculated total principal via reduce -> sum={}", total);
        return total;
    }
}