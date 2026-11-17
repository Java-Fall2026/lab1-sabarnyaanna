package ua.bank.io;

import java.util.List;

public record LoadResult(
        int readCount,
        int addedCount,
        List<RejectedRecord> rejected
) {
    public LoadResult {
        rejected = rejected == null ? List.of() : List.copyOf(rejected);
    }
}   