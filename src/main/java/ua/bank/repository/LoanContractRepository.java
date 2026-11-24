package ua.bank.repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.bank.model.LoanContract;
import ua.bank.model.LoanType;

/**
 * Specialized repository for LoanContract entities providing Stream-based filtering,
 * natural and custom sorting, grouping by loan type, and portfolio aggregation.
 */
public class LoanContractRepository extends GenericRepository<LoanContract> {

    private static final Logger log = LoggerFactory.getLogger(LoanContractRepository.class);

    public LoanContractRepository() {
        super("LoanContract", LoanContract::contractId);
    }

    /**
     * Finds loan contracts matching the specified loan type.
     */
    public List<LoanContract> findByLoanType(LoanType loanType) {
        Objects.requireNonNull(loanType, "loanType must not be null");
        List<LoanContract> result = stream()
                .filter(c -> c.loanType() == loanType)
                .toList();

        if (result.isEmpty()) {
            log.warn("[{}] No contracts found for loanType={}", getEntityName(), loanType);
        } else {
            log.info("[{}] Found {} contracts for loanType={}", getEntityName(), result.size(), loanType);
        }
        return result;
    }

    /**
     * Finds loan contracts whose principal amount falls within [minAmount, maxAmount] inclusive.
     */
    public List<LoanContract> findByPrincipalRange(double minAmount, double maxAmount) {
        if (minAmount < 0 || maxAmount < 0 || minAmount > maxAmount) {
            throw new IllegalArgumentException(
                    "Invalid principal range: minAmount=" + minAmount + ", maxAmount=" + maxAmount
            );
        }
        List<LoanContract> result = stream()
                .filter(c -> c.principalAmount() >= minAmount && c.principalAmount() <= maxAmount)
                .toList();

        if (result.isEmpty()) {
            log.warn("[{}] No contracts found in principal range [{}, {}]", getEntityName(), minAmount, maxAmount);
        } else {
            log.info("[{}] Found {} contracts in principal range [{}, {}]", getEntityName(), result.size(), minAmount, maxAmount);
        }
        return result;
    }

    /**
     * Performs case-insensitive substring search by borrower full name.
     */
    public List<LoanContract> searchByBorrowerName(String query) {
        if (query == null || query.trim().isEmpty()) {
            log.warn("[{}] Borrower name search called with blank query", getEntityName());
            return List.of();
        }
        String lowerQuery = query.trim().toLowerCase();
        List<LoanContract> result = stream()
                .filter(c -> c.borrower().getFullName().toLowerCase().contains(lowerQuery))
                .toList();

        if (result.isEmpty()) {
            log.warn("[{}] No contracts found for borrower name '{}'", getEntityName(), query);
        } else {
            log.info("[{}] Found {} contracts for borrower name '{}'", getEntityName(), result.size(), query);
        }
        return result;
    }

    /**
     * Returns all contracts sorted using LoanContract natural ordering (Comparable).
     */
    public List<LoanContract> findAllSortedByNaturalOrder() {
        return findAllSorted(Comparator.naturalOrder());
    }

    /**
     * Returns all contracts sorted by principal amount in the specified direction.
     */
    public List<LoanContract> findAllSortedByPrincipal(SortOrder order) {
        Objects.requireNonNull(order, "SortOrder must not be null");
        Comparator<LoanContract> comparator = Comparator.comparingDouble(LoanContract::principalAmount);
        if (order == SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        return findAllSorted(comparator);
    }

    /**
     * Groups all loan contracts by their LoanType.
     */
    public Map<LoanType, List<LoanContract>> groupByLoanType() {
        Map<LoanType, List<LoanContract>> grouped = stream()
                .collect(Collectors.groupingBy(LoanContract::loanType));
        log.info("[{}] Grouped contracts by LoanType -> groups={}", getEntityName(), grouped.size());
        return grouped;
    }

    /**
     * Counts the number of loan contracts per LoanType.
     */
    public Map<LoanType, Long> countByLoanType() {
        Map<LoanType, Long> counts = stream()
                .collect(Collectors.groupingBy(LoanContract::loanType, Collectors.counting()));
        log.info("[{}] Counted contracts by LoanType -> groups={}", getEntityName(), counts.size());
        return counts;
    }

    /**
     * Calculates the total principal amount across all contracts in the repository.
     */
    public double getTotalPrincipalAmount() {
        double total = stream()
                .mapToDouble(LoanContract::principalAmount)
                .sum();
        log.info("[{}] Calculated total principal amount: {}", getEntityName(), total);
        return total;
    }
}