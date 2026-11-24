package ua.bank.io;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.bank.repository.GenericRepository;

/**
 * Loads entities from a file via FileStorage, validates each record using Jakarta Validation,
 * and adds valid unique items into a GenericRepository.
 */
public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    public <T> LoadResult loadInto(Path path, FileStorage<T> storage, GenericRepository<T> repository) {
        List<T> items;
        try {
            items = storage.load(path);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load data from file: " + path, e);
        }

        int readCount = items.size();
        int addedCount = 0;
        List<RejectedRecord> rejected = new ArrayList<>();

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            for (int i = 0; i < items.size(); i++) {
                T item = items.get(i);
                int recordNumber = i + 1;

                if (item == null) {
                    RejectedRecord rec = new RejectedRecord(recordNumber, List.of("Record is null"));
                    rejected.add(rec);
                    log.warn("Rejected record #{} in '{}': record is null", recordNumber, path);
                    continue;
                }

                Set<ConstraintViolation<T>> violations = validator.validate(item);
                if (!violations.isEmpty()) {
                    List<String> reasons = violations.stream()
                            .map(v -> {
                                String field = v.getPropertyPath().toString();
                                return (field.isEmpty() ? "cross-field" : field) + ": " + v.getMessage();
                            })
                            .sorted()
                            .toList();
                    RejectedRecord rec = new RejectedRecord(recordNumber, reasons);
                    rejected.add(rec);
                    log.warn("Rejected record #{} in '{}' due to {} validation error(s): {}",
                            recordNumber, path, reasons.size(), reasons);
                    continue;
                }

                boolean added = repository.addIfAbsent(item);
                if (added) {
                    addedCount++;
                } else {
                    RejectedRecord rec = new RejectedRecord(recordNumber, List.of("Duplicate entity identity"));
                    rejected.add(rec);
                    log.warn("Rejected record #{} in '{}': duplicate identity", recordNumber, path);
                }
            }
        }

        log.info("Finished loading '{}': read={}, added={}, rejectedRecords={}",
                path, readCount, addedCount, rejected.size());
        return new LoadResult(readCount, addedCount, rejected);
    }
}