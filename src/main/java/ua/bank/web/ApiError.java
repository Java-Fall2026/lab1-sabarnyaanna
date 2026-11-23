package ua.bank.web;

import java.time.Instant;
import java.util.List;

public record ApiError(
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        List<String> details
) {
    public ApiError {
        details = details == null ? List.of() : List.copyOf(details);
    }

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(status, error, message, path, Instant.now(), List.of());
    }

    public static ApiError of(int status, String error, String message, String path, List<String> details) {
        return new ApiError(status, error, message, path, Instant.now(), details);
    }
}