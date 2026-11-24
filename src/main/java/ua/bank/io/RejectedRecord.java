package ua.bank.io;

import java.util.List;

/**
 * Represents a single rejected record along with all its validation or duplicate reasons.
 */
public record RejectedRecord(
        int recordIndex,
        List<String> reasons
) {
    public RejectedRecord {
        reasons = reasons == null ? List.of() : List.copyOf(reasons);
    }
}