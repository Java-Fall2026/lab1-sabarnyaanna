package ua.bank.io;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.bank.repository.GenericRepository;

public class DataLoader {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);
    private final Validator validator;

    public DataLoader() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    public <T> LoadResult loadInto(Path file, FileStorage<T> storage, GenericRepository<T> repository) {
        List<RejectedRecord> rejected = new ArrayList<>();
        List<T> rawItems;

        try {
            rawItems = storage.load(file);
        } catch (Exception e) {
            RejectedRecord fileError = new RejectedRecord(0, "file", "Помилка читання файлу '" + file + "': " + e.getMessage());
            rejected.add(fileError);
            log.error("File '{}' failed to load: {}", file, e.getMessage());
            log.warn("Rejected record -> {}", fileError);
            log.info("Load summary for '{}': read=0, added=0, rejected={}", file, rejected.size());
            return new LoadResult(0, 0, rejected);
        }

        int addedCount = 0;
        for (int i = 0; i < rawItems.size(); i++) {
            int recordNumber = i + 1;
            T item = rawItems.get(i);

            if (item == null) {
                RejectedRecord nullRec = new RejectedRecord(recordNumber, "object", "Запис є порожнім (null)");
                rejected.add(nullRec);
                log.warn("Rejected record -> {}", nullRec);
                continue;
            }

            Set<ConstraintViolation<T>> violations = validator.validate(item);
            if (!violations.isEmpty()) {
                for (ConstraintViolation<T> v : violations) {
                    String fieldPath = v.getPropertyPath().toString();
                    if (fieldPath.isEmpty()) {
                        fieldPath = "cross-field";
                    }
                    RejectedRecord rej = new RejectedRecord(recordNumber, fieldPath, v.getMessage());
                    rejected.add(rej);
                    log.warn("Rejected record -> {}", rej);
                }
                continue;
            }

            boolean added = repository.add(item);
            if (added) {
                addedCount++;
            } else {
                RejectedRecord dup = new RejectedRecord(recordNumber, "identity", "Дублікат унікального ідентифікатора");
                rejected.add(dup);
                log.warn("Rejected record -> {}", dup);
            }
        }

        log.info("Load summary for '{}': read={}, added={}, rejected={}",
                file, rawItems.size(), addedCount, rejected.size());
        return new LoadResult(rawItems.size(), addedCount, rejected);
    }
}