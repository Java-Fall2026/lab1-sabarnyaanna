package ua.bank;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ua.bank.config.AppConfig;
import ua.bank.io.DataLoader;
import ua.bank.io.FileStorage;
import ua.bank.io.LoadResult;
import ua.bank.model.BankBranch;
import ua.bank.model.Borrower;
import ua.bank.model.LoanContract;
import ua.bank.model.LoanType;
import ua.bank.repository.BorrowerRepository;
import ua.bank.repository.LoanContractRepository;

class BankLab6Test {

    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("1. Збереження й читання JSON і YAML та ISO-формат дат (2026-11-02)")
    void shouldSaveAndLoadJsonAndYamlWithIsoDates(@TempDir Path tempDir) throws IOException {
        FileStorage<Borrower> storage = new FileStorage<>(Borrower.class);
        Borrower b = new Borrower("AA123456", "Олена Ковальчук", LocalDate.of(1995, 11, 2), 40000.0);

        Path jsonFile = tempDir.resolve("borrowers.json");
        Path yamlFile = tempDir.resolve("borrowers.yaml");

        storage.save(jsonFile, List.of(b));
        storage.save(yamlFile, List.of(b));

        String rawJson = Files.readString(jsonFile);
        String rawYaml = Files.readString(yamlFile);
        assertTrue(rawJson.contains("1995-11-02"), "Дата у JSON має бути в ISO-форматі");
        assertTrue(rawYaml.contains("1995-11-02"), "Дата у YAML має бути в ISO-форматі");

        assertEquals(List.of(b), storage.load(jsonFile));
        assertEquals(List.of(b), storage.load(yamlFile));
    }

    @Test
    @DisplayName("2. Виняток при невідомому розширенні файлу")
    void shouldThrowExceptionOnUnknownFileExtension(@TempDir Path tempDir) {
        FileStorage<Borrower> storage = new FileStorage<>(Borrower.class);
        Path txtFile = tempDir.resolve("borrowers.txt");

        assertThrows(IllegalArgumentException.class, () -> storage.load(txtFile));
        assertThrows(IllegalArgumentException.class, () -> storage.save(txtFile, List.of()));
    }

    @Test
    @DisplayName("3. Стандартні анотації Jakarta Validation з повідомленнями українською")
    void shouldValidateStandardAnnotationsWithUkrainianMessages() {
        Borrower invalidBorrower = new Borrower("bad", "А", LocalDate.of(2030, 1, 1), -100.0);
        Set<ConstraintViolation<Borrower>> violations = validator.validate(invalidBorrower);

        assertEquals(4, violations.size());
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("Номер паспорта")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("ПІБ")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("минулому")));
        assertTrue(violations.stream().anyMatch(v -> v.getMessage().contains("більшим за 0")));
    }

    @Test
    @DisplayName("4. Власна перехресна анотація @ValidLoanLimit (сума кредиту <= дохід * 60)")
    void shouldValidateCustomCrossFieldAnnotation() {
        Borrower borrower = new Borrower("AA111222", "Іван Петренко", LocalDate.of(1990, 5, 10), 20000.0);
        BankBranch branch = BankBranch.of("BR-01", "Київ");

        // Ліміт = 20000 * 60 = 1_200_000, передаємо 1_500_000 -> порушення перехресного правила
        LoanContract overLimitContract = new LoanContract(
                "LN-2026-99", borrower, branch, LoanType.MORTGAGE,
                1_500_000.0, LocalDate.of(2026, 6, 1), 60
        );

        Set<ConstraintViolation<LoanContract>> violations = validator.validate(overLimitContract);
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("60 місячних доходів"));
    }

    @Test
    @DisplayName("5. DataLoader: завантаження з невалідними записами та дублікатом у відкинутих")
    void shouldLoadValidRecordsAndCollectInvalidAndDuplicates(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("mixed.json");
        String content = """
                [
                  { "passportNum": "AA111111", "fullName": "Перший Клієнт", "birthDate": "1990-01-01", "monthlyIncome": 30000.0 },
                  { "passportNum": "INVALID", "fullName": "Битий Паспорт", "birthDate": "1990-01-01", "monthlyIncome": 30000.0 },
                  { "passportNum": "AA111111", "fullName": "Дублікат Клієнта", "birthDate": "1990-01-01", "monthlyIncome": 30000.0 }
                ]
                """;
        Files.writeString(file, content);

        DataLoader loader = new DataLoader();
        FileStorage<Borrower> storage = new FileStorage<>(Borrower.class);
        BorrowerRepository repo = new BorrowerRepository();

        LoadResult result = loader.loadInto(file, storage, repo);
        assertEquals(3, result.readCount());
        assertEquals(1, result.addedCount());
        assertEquals(2, result.rejected().size());
        assertEquals(2, result.rejected().get(0).recordNumber());
        assertEquals(3, result.rejected().get(1).recordNumber());
        assertEquals("identity", result.rejected().get(1).field());
    }

    @Test
    @DisplayName("6. DataLoader: обробка синтаксично битого та неіснуючого файлу без падіння")
    void shouldHandleBrokenAndMissingFilesGracefully(@TempDir Path tempDir) throws IOException {
        Path brokenFile = tempDir.resolve("broken.json");
        Files.writeString(brokenFile, "{ broken json [");
        Path missingFile = tempDir.resolve("does_not_exist.yaml");

        DataLoader loader = new DataLoader();
        FileStorage<LoanContract> storage = new FileStorage<>(LoanContract.class);
        LoanContractRepository repo = new LoanContractRepository();

        LoadResult brokenRes = loader.loadInto(brokenFile, storage, repo);
        assertEquals(0, brokenRes.readCount());
        assertEquals(0, brokenRes.addedCount());
        assertEquals(1, brokenRes.rejected().size());

        LoadResult missingRes = loader.loadInto(missingFile, storage, repo);
        assertEquals(0, missingRes.readCount());
        assertEquals(1, missingRes.rejected().size());
    }

    @Test
    @DisplayName("7. AppConfig: виключення з назвою ключа при відсутності параметра в Properties")
    void shouldThrowExceptionWithKeyNameWhenPropertyMissing() {
        Properties props = new Properties();
        props.setProperty("existing.key", "data/test.json");
        AppConfig config = new AppConfig(props);

        assertEquals(Path.of("data/test.json"), config.getRequiredPath("existing.key"));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> config.getRequiredPath("missing.secret.key")
        );
        assertTrue(ex.getMessage().contains("missing.secret.key"));
    }
}