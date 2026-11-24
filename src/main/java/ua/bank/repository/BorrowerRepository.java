package ua.bank.repository;

import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.bank.model.Borrower;

/**
 * Specialized repository for Borrower entities providing Stream-based search,
 * custom sorting, and income statistics aggregation.
 */
public class BorrowerRepository extends GenericRepository<Borrower> {

    private static final Logger log = LoggerFactory.getLogger(BorrowerRepository.class);

    public BorrowerRepository() {
        super("Borrower", Borrower::getPassportNum);
    }

    /**
     * Finds borrowers whose monthly income falls within [minIncome, maxIncome] inclusive.
     */
    public List<Borrower> findByIncomeRange(double minIncome, double maxIncome) {
        if (minIncome < 0 || maxIncome < 0 || minIncome > maxIncome) {
            throw new IllegalArgumentException(
                    "Invalid income range: minIncome=" + minIncome + ", maxIncome=" + maxIncome
            );
        }
        List<Borrower> result = stream()
                .filter(b -> b.getMonthlyIncome() >= minIncome && b.getMonthlyIncome() <= maxIncome)
                .toList();

        if (result.isEmpty()) {
            log.warn("[{}] No borrowers found in income range [{}, {}]", getEntityName(), minIncome, maxIncome);
        } else {
            log.info("[{}] Found {} borrowers in income range [{}, {}]", getEntityName(), result.size(), minIncome, maxIncome);
        }
        return result;
    }

    /**
     * Performs case-insensitive substring search by borrower full name.
     */
    public List<Borrower> searchByNameContaining(String query) {
        if (query == null || query.trim().isEmpty()) {
            log.warn("[{}] Name search called with blank query", getEntityName());
            return List.of();
        }
        String lowerQuery = query.trim().toLowerCase();
        List<Borrower> result = stream()
                .filter(b -> b.getFullName().toLowerCase().contains(lowerQuery))
                .toList();

        if (result.isEmpty()) {
            log.warn("[{}] No borrowers found matching name '{}'", getEntityName(), query);
        } else {
            log.info("[{}] Found {} borrowers matching name '{}'", getEntityName(), result.size(), query);
        }
        return result;
    }

    /**
     * Returns all borrowers sorted by monthly income in the specified direction.
     */
    public List<Borrower> findAllSortedByIncome(SortOrder order) {
        Objects.requireNonNull(order, "SortOrder must not be null");
        Comparator<Borrower> comparator = Comparator.comparingDouble(Borrower::getMonthlyIncome);
        if (order == SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        return findAllSorted(comparator);
    }

    /**
     * Returns all borrowers sorted naturally (by passport number).
     */
    public List<Borrower> findAllSortedByNaturalOrder() {
        return findAllSorted(Comparator.naturalOrder());
    }

    /**
     * Calculates summary statistics (count, sum, min, average, max) of borrower monthly incomes.
     */
    public DoubleSummaryStatistics getIncomeStatistics() {
        DoubleSummaryStatistics stats = stream()
                .mapToDouble(Borrower::getMonthlyIncome)
                .summaryStatistics();
        log.info("[{}] Calculated income statistics: count={}, avg={}", getEntityName(), stats.getCount(), stats.getAverage());
        return stats;
    }
}