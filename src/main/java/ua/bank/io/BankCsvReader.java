package ua.bank.io;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.bank.exception.InvalidDataException;
import ua.bank.model.BankBranch;
import ua.bank.model.Borrower;

public final class BankCsvReader {

    private static final Logger log = LoggerFactory.getLogger(BankCsvReader.class);

    private BankCsvReader() {
    }

    public static List<BankBranch> readBranches(String filePath) {
        List<BankBranch> branches = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.trim().isEmpty() || (lineNumber == 1 && line.startsWith("branchCode"))) {
                    continue;
                }
                try {
                    BankBranch branch = parseBranchLine(line, lineNumber);
                    branches.add(branch);
                    log.info("Parsed BankBranch at line {}: {} ({})", lineNumber, branch.branchCode(), branch.city());
                } catch (InvalidDataException e) {
                    log.warn("Skipping invalid branch row #{}: {}", lineNumber, e.getMessage());
                }
            }
            log.info("Successfully loaded {} branches from {}", branches.size(), filePath);
        } catch (FileNotFoundException e) {
            log.warn("Branches file not found: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to read branches file '{}': {}", filePath, e.getMessage(), e);
        } finally {
            log.info("Finished reading {}", filePath);
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

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8))) {
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
                    log.info("Parsed Borrower at line {}: {} ({})", lineNumber, borrower.getPassportNum(), borrower.getFullName());
                } catch (InvalidDataException e) {
                    log.warn("Skipping invalid borrower row #{}: {}", lineNumber, e.getMessage());
                }
            }
            log.info("Successfully loaded {} borrowers from {}", borrowers.size(), filePath);
        } catch (FileNotFoundException e) {
            log.warn("Borrowers file not found: {}", filePath);
        } catch (IOException e) {
            log.error("Failed to read borrowers file '{}': {}", filePath, e.getMessage(), e);
        } finally {
            log.info("Finished reading {}", filePath);
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
            throw new InvalidDataException("Invalid borrower data at line #" + lineNumber + ": " + e.getMessage(), e);
        }
    }
}