package ua.bank.io;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import ua.bank.exception.InvalidDataException;
import ua.bank.model.BankBranch;
import ua.bank.model.Borrower;

public final class BankCsvReader {

    private static final Logger LOGGER = Logger.getLogger(BankCsvReader.class.getName());

    private BankCsvReader() {
    }

    public static List<BankBranch> readBranches(String filePath) {
        List<BankBranch> branches = new ArrayList<>();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8));
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty() || (lineNumber == 1 && line.startsWith("branchCode"))) {
                    continue; // skip empty line or CSV header
                }
                try {
                    BankBranch branch = parseBranchLine(line, lineNumber);
                    branches.add(branch);
                } catch (InvalidDataException e) {
                    LOGGER.log(Level.WARNING, "Skipping invalid branch row #" + lineNumber + ": " + e.getMessage());
                }
            }
            LOGGER.info("Successfully loaded " + branches.size() + " branches from " + filePath);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read branches file: " + filePath, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                    LOGGER.info("Closed reader for file: " + filePath);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error closing file reader: " + filePath, e);
                }
            }
        }
        return branches;
    }

    public static BankBranch parseBranchLine(String line, int lineNumber) throws InvalidDataException {
        if (line == null || line.trim().isEmpty()) {
            throw new InvalidDataException("Line #" + lineNumber + " is empty");
        }
        String[] parts = line.split(",", -1);
        if (parts.length != 2) {
            throw new InvalidDataException("Line #" + lineNumber + " expected 2 columns (branchCode,city), got: " + parts.length);
        }
        try {
            return BankBranch.of(parts[0], parts[1]);
        } catch (IllegalArgumentException e) {
            throw new InvalidDataException("Validation failed at line #" + lineNumber + ": " + e.getMessage(), e);
        }
    }

    public static List<Borrower> readBorrowers(String filePath) {
        List<Borrower> borrowers = new ArrayList<>();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8));
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty() || (lineNumber == 1 && line.startsWith("passportNum"))) {
                    continue;
                }
                try {
                    Borrower borrower = parseBorrowerLine(line, lineNumber);
                    borrowers.add(borrower);
                } catch (InvalidDataException e) {
                    LOGGER.log(Level.WARNING, "Skipping invalid borrower row #" + lineNumber + ": " + e.getMessage());
                }
            }
            LOGGER.info("Successfully loaded " + borrowers.size() + " borrowers from " + filePath);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read borrowers file: " + filePath, e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                    LOGGER.info("Closed reader for file: " + filePath);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Error closing file reader: " + filePath, e);
                }
            }
        }
        return borrowers;
    }

    public static Borrower parseBorrowerLine(String line, int lineNumber) throws InvalidDataException {
        if (line == null || line.trim().isEmpty()) {
            throw new InvalidDataException("Line #" + lineNumber + " is empty");
        }
        String[] parts = line.split(",", -1);
        if (parts.length != 4) {
            throw new InvalidDataException("Line #" + lineNumber + " expected 4 columns, got: " + parts.length);
        }
        try {
            LocalDate birthDate = LocalDate.parse(parts[2].trim());
            double income = Double.parseDouble(parts[3].trim());
            return new Borrower(parts[0], parts[1], birthDate, income);
        } catch (DateTimeParseException | IllegalArgumentException e) {
            // multi-catch for parsing and domain validation errors
            throw new InvalidDataException("Invalid borrower data at line #" + lineNumber + ": " + e.getMessage(), e);
        }
    }
}