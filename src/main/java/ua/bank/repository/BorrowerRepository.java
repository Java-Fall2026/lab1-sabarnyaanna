package ua.bank.repository;

import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.bank.model.Borrower;

public class BorrowerRepository extends GenericRepository<Borrower> {

    private static final Logger log = LoggerFactory.getLogger(BorrowerRepository.class);

    public BorrowerRepository() {
        super("Borrower", Borrower::getPassportNum);
    }

    // --- Сортування ---

    public List<Borrower> findAllSortedByNaturalOrder() {
        List<Borrower> result = stream().sorted().toList();
        log.info("[Borrower] Sorted by natural order (passportNum) -> count={}", result.size());
        return result;
    }

    /**
     * Багаторівневе сортування: за доходом за спаданням (reversed), потім за ПІБ за зростанням.
     */
    public List<Borrower> findAllSortedByIncomeDescThenName() {
        Comparator<Borrower> comparator = Comparator
                .comparingDouble(Borrower::getMonthlyIncome)
                .reversed()
                .thenComparing(Borrower::getFullName);
        List<Borrower> result = findAllSorted(comparator);
        log.info("[Borrower] Sorted by monthlyIncome DESC, fullName ASC -> count={}", result.size());
        return result;
    }

    /**
     * Сортування за віком (дата народження за зростанням — від найстарших до наймолодших).
     */
    public List<Borrower> findAllSortedByBirthDateAsc() {
        List<Borrower> result = findAllSorted(Comparator.comparing(Borrower::getBirthDate));
        log.info("[Borrower] Sorted by birthDate ASC -> count={}", result.size());
        return result;
    }

    // --- Пошук через Stream API ---

    public List<Borrower> searchByNameContaining(String namePart) {
        if (namePart == null || namePart.trim().isEmpty()) {
            log.info("[Borrower] Search by name='' -> count=0");
            return List.of();
        }
        String query = namePart.trim().toLowerCase();
        List<Borrower> result = stream()
                .filter(b -> b.getFullName().toLowerCase().contains(query))
                .sorted(Comparator.comparing(Borrower::getFullName))
                .toList();
        log.info("[Borrower] Search by name containing '{}' -> count={}", query, result.size());
        return result;
    }

    public List<Borrower> findByIncomeRange(double minIncome, double maxIncome) {
        List<Borrower> result = stream()
                .filter(b -> Double.compare(b.getMonthlyIncome(), minIncome) >= 0
                        && Double.compare(b.getMonthlyIncome(), maxIncome) <= 0)
                .sorted(Comparator.comparingDouble(Borrower::getMonthlyIncome))
                .toList();
        log.info("[Borrower] Search by income range [{}, {}] -> count={}", minIncome, maxIncome, result.size());
        return result;
    }

    public Optional<Borrower> findFirstWithIncomeAbove(double minIncome, String namePrefix) {
        String prefix = namePrefix == null ? "" : namePrefix.trim().toLowerCase();
        Optional<Borrower> found = stream()
                .filter(b -> Double.compare(b.getMonthlyIncome(), minIncome) > 0)
                .filter(b -> b.getFullName().toLowerCase().startsWith(prefix))
                .findFirst();
        log.info("[Borrower] Search first with income > {} and name prefix '{}' -> found={}",
                minIncome, prefix, found.isPresent());
        return found;
    }

    public List<Borrower> findTopEarners(int limit) {
        List<Borrower> result = stream()
                .sorted(Comparator.comparingDouble(Borrower::getMonthlyIncome).reversed())
                .limit(Math.max(0, limit))
                .toList();
        log.info("[Borrower] Top {} earners -> count={}", limit, result.size());
        return result;
    }

    // --- Агрегація ---

    public DoubleSummaryStatistics getIncomeStatistics() {
        DoubleSummaryStatistics stats = stream()
                .collect(Collectors.summarizingDouble(Borrower::getMonthlyIncome));
        log.info("[Borrower] Income statistics: count={}, sum={}, avg={}, max={}",
                stats.getCount(), stats.getSum(), stats.getAverage(), stats.getMax());
        return stats;
    }
}