package ua.bank.io;

import java.util.List;

/**
 * Summary of loading records from a file into a repository.
 */
public record LoadResult(
        int readCount,
        int addedCount,
        List<RejectedRecord> rejected
) {
    public LoadResult {
        rejected = rejected == null ? List.of() : List.copyOf(rejected);
    }

    public int rejectedCount() {
        return rejected.size();
    }
}