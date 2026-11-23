package ua.bank.web;

import java.util.List;

public class HttpApiException extends RuntimeException {

    private final int status;
    private final String errorTitle;
    private final List<String> details;

    public HttpApiException(int status, String errorTitle, String message) {
        this(status, errorTitle, message, List.of());
    }

    public HttpApiException(int status, String errorTitle, String message, List<String> details) {
        super(message);
        this.status = status;
        this.errorTitle = errorTitle;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public int getStatus() { return status; }
    public String getErrorTitle() { return errorTitle; }
    public List<String> getDetails() { return details; }

    public static HttpApiException badRequest(String message) {
        return new HttpApiException(400, "Bad Request", message);
    }

    public static HttpApiException badRequest(String message, List<String> details) {
        return new HttpApiException(400, "Bad Request", message, details);
    }

    public static HttpApiException notFound(String message) {
        return new HttpApiException(404, "Not Found", message);
    }

    public static HttpApiException methodNotAllowed(String message) {
        return new HttpApiException(405, "Method Not Allowed", message);
    }

    public static HttpApiException conflict(String message) {
        return new HttpApiException(409, "Conflict", message);
    }

    public static HttpApiException unsupportedMediaType(String message) {
        return new HttpApiException(415, "Unsupported Media Type", message);
    }
}