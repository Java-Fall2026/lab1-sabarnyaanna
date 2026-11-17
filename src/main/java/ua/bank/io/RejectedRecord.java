package ua.bank.io;

public record RejectedRecord(
        int recordNumber,
        String field,
        String message
) {
    @Override
    public String toString() {
        return "Запис #" + recordNumber + " [" + field + "]: " + message;
    }
}