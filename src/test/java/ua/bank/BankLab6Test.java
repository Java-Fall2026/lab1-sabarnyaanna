package ua.bank;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import ua.bank.config.AppConfig;
import ua.bank.io.DataFormat;
import ua.bank.io.DataLoader;
import ua.bank.io.FileStorage;
import ua.bank.io.LoadResult;
import ua.bank.model.BankBranch;
import ua.bank.model.Borrower;
import ua.bank.model.LoanContract;
import ua.bank.model.LoanType;
import ua.bank.repository.BorrowerRepository;

/**
 * Unit and I/O tests for Lab 6: classpath config, JSON/YAML serialization,
 * parameterized Jakarta Validation field checks, custom annotations, and DataLoader.
 */
class BankLab6Test {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void initValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    static Stream<Arguments> invalidEntityCases() {
        Borrower validBorrower = new Borrower("AA111222", "Олена Ковальчук", LocalDate.of(1995, 5, 14), 40000.0);
        BankBranch validBranch = BankBranch.of("BR-01", "Київ");

        return Stream.of(
                Arguments.of(
                        "Borrower blank passportNum (@NotBlank)",
                        new Borrower("", "Олена Ковальчук", LocalDate.of(1995, 5, 14), 40000.0),
                        "passportNum"
                ),
                Arguments.of(
                        "Borrower invalid passportNum format (@Pattern)",
                        new Borrower("bad", "Олена Ковальчук", LocalDate.of(1995, 5, 14), 40000.0),
                        "passportNum"
                ),
                Arguments.of(
                        "Borrower too short fullName (@Size)",
                        new Borrower("AA111222", "О", LocalDate.of(1995, 5, 14), 40000.0),
                        "fullName"
                ),
                Arguments.of(
                        "Borrower future birthDate (@Past)",
                        new Borrower("AA111222", "Олена Ковальчук", LocalDate.now().plusDays(10), 40000.0),
                        "birthDate"
                ),
                Arguments.of(
                        "Borrower non-positive monthlyIncome (@Positive)",
                        new Borrower("AA111222", "Олена Ковальчук", LocalDate.of(1995, 5, 14), 0.0),
                        "monthlyIncome"
                ),
                Arguments.of(
                        "LoanContract invalid contractId (@Pattern)",
                        new LoanContract("INVALID", validBorrower, validBranch, LoanType.AUTO, 200000.0, LocalDate.now(), 24),
                        "contractId"
                ),
                Arguments.of(
                        "LoanContract null loanType (@NotNull)",
                        new LoanContract("LN-2026-01", validBorrower, validBranch, null, 200000.0, LocalDate.now(), 24),
                        "loanType"
                ),
                Arguments.of(
                        "LoanContract negative principalAmount (@Positive)",
                        new LoanContract("LN-2026-01", validBorrower, validBranch, LoanType.AUTO, -100.0, LocalDate.now(), 24),
                        "principalAmount"
                ),
                Arguments.of(
                        "LoanContract null issueDate (@NotNull)",
                        new LoanContract("LN-2026-01", validBorrower, validBranch, LoanType.AUTO, 200000.0, null, 24),
                        "issueDate"
                ),
                Arguments.of(
                        "LoanContract zero termMonths (@Min)",
                        new LoanContract("LN-2026-01", validBorrower, validBranch, LoanType.AUTO, 200000.0, LocalDate.now(), 0),
                        "termMonths"
                ),
                Arguments.of(
                        "LoanContract termMonths exceeding max (@Max)",
                        new LoanContract("LN-2026-01", validBorrower, validBranch, LoanType.AUTO, 200000.0, LocalDate.now(), 400),
                        "termMonths"
                )
        );
    }

    @ParameterizedTest(name = "{0} -> expected field: {2}")
    @MethodSource("invalidEntityCases")
    @DisplayName("1. Should report exact property field for every standard validation annotation")
    void shouldReportExactFieldForStandardAnnotations(String caseName, Object entity, String expectedField) {
        Set<ConstraintViolation<Object>> violations = validator.validate(entity);

        assertFalse(violations.isEmpty(), "Expected validation violation for: " + caseName);
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals(expectedField)),
                "Expected violation on field '" + expectedField + "', actual fields: " +
                        violations.stream().map(v -> v.getPropertyPath().toString()).toList()
        );
    }

    @Test
    @DisplayName("2. Should cascade validation to nested Borrower (@Valid) inside LoanContract")
    void shouldCascadeValidationToNestedBorrower() {
        Borrower invalidBorrower = new Borrower("bad", "Олена Ковальчук", LocalDate.of(1995, 5, 14), 40000.0);
        BankBranch validBranch = BankBranch.of("BR-01", "Київ");
        LoanContract contract = new LoanContract("LN-2026-01", invalidBorrower, validBranch, LoanType.AUTO, 200000.0, LocalDate.now(), 24);

        Set<ConstraintViolation<LoanContract>> violations = validator.validate(contract);
        assertTrue(
                violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("borrower.passportNum")),
                "Expected cascaded violation on 'borrower.passportNum'"
        );
    }

    @Test
    @DisplayName("3. Should fail @ValidLoanLimit cross-field validation when principal exceeds 60x income")
    void shouldFailValidLoanLimitCustomAnnotation() {
        Borrower borrower = new Borrower("AA111222", "Олена Ковальчук", LocalDate.of(1995, 5, 14), 20000.0);
        BankBranch branch = BankBranch.of("BR-01", "Київ");
        LoanContract excessive = new LoanContract(
                "LN-2026-05", borrower, branch, LoanType.MORTGAGE,
                1_500_000.0, LocalDate.now(), 60
        );

        Set<ConstraintViolation<LoanContract>> violations = validator.validate(excessive);
        assertEquals(1, violations.size());
        assertTrue(violations.iterator().next().getMessage().contains("60"));
    }

    @Test
    @DisplayName("4. Should serialize and deserialize Borrower list to/from JSON preserving equality")
    void shouldRoundTripBorrowersJson() throws IOException {
        FileStorage<Borrower> storage = new FileStorage<>(Borrower.class);
        Path jsonFile = tempDir.resolve("borrowers.json");
        List<Borrower> original = List.of(
                new Borrower("AA111222", "Олена Ковальчук", LocalDate.of(1995, 5, 14), 35000.0),
                new Borrower("BB333444", "Тарас Бондар", LocalDate.of(1988, 9, 22), 52000.0)
        );

        storage.save(jsonFile, original);
        List<Borrower> reloaded = storage.load(jsonFile);

        assertEquals(original, reloaded);
    }

    @Test
    @DisplayName("5. Should serialize and deserialize LoanContract list to/from YAML preserving equality")
    void shouldRoundTripContractsYaml() throws IOException {
        FileStorage<LoanContract> storage = new FileStorage<>(LoanContract.class);
        Path yamlFile = tempDir.resolve("contracts.yaml");
        Borrower borrower = new Borrower("AA111222", "Олена Ковальчук", LocalDate.of(1995, 5, 14), 35000.0);
        BankBranch branch = BankBranch.of("BR-01", "Київ");
        List<LoanContract> original = List.of(
                new LoanContract("LN-2026-01", borrower, branch, LoanType.MORTGAGE, 850000.0, LocalDate.of(2026, 6, 1), 120)
        );

        storage.save(yamlFile, original);
        List<LoanContract> reloaded = storage.load(yamlFile);

        assertEquals(original, reloaded);
    }

    @Test
    @DisplayName("6. Should count a record with multiple validation errors as ONE rejected record in LoadResult")
    void shouldCountRecordWithMultipleErrorsOnce() throws IOException {
        FileStorage<Borrower> storage = new FileStorage<>(Borrower.class);
        Path file = tempDir.resolve("multi_error_borrowers.json");

        Borrower valid = new Borrower("AA111222", "Олена Ковальчук", LocalDate.of(1995, 5, 14), 35000.0);
        Borrower multiInvalid = new Borrower("bad", "О", LocalDate.now().plusYears(1), -100.0);

        storage.save(file, List.of(valid, multiInvalid));

        BorrowerRepository repo = new BorrowerRepository();
        DataLoader loader = new DataLoader();
        LoadResult result = loader.loadInto(file, storage, repo);

        assertEquals(2, result.readCount());
        assertEquals(1, result.addedCount());
        assertEquals(1, result.rejectedCount(), "Record with multiple errors must be counted once");
        assertTrue(result.rejected().get(0).reasons().size() >= 4);
    }

    @Test
    @DisplayName("7. Should reject duplicate identity records in DataLoader")
    void shouldRejectDuplicateRecordsInDataLoader() throws IOException {
        FileStorage<Borrower> storage = new FileStorage<>(Borrower.class);
        Path file = tempDir.resolve("duplicates.json");

        Borrower first = new Borrower("AA111222", "Олена Ковальчук", LocalDate.of(1995, 5, 14), 35000.0);
        Borrower duplicate = new Borrower("AA111222", "Олена Дублікат", LocalDate.of(1995, 5, 14), 45000.0);

        storage.save(file, List.of(first, duplicate));

        BorrowerRepository repo = new BorrowerRepository();
        LoadResult result = new DataLoader().loadInto(file, storage, repo);

        assertEquals(2, result.readCount());
        assertEquals(1, result.addedCount());
        assertEquals(1, result.rejectedCount());
    }

    @Test
    @DisplayName("8. Should load app.properties from classpath")
    void shouldLoadAppConfigFromClasspath() {
        AppConfig config = AppConfig.loadFromClasspath("app.properties");
        assertEquals(Path.of("data/borrowers.json"), config.getRequiredPath("data.borrowers.file"));
        assertEquals(Path.of("data/contracts.yaml"), config.getRequiredPath("data.contracts.file"));
    }

    @Test
    @DisplayName("9. Should throw IllegalStateException when classpath config resource does not exist")
    void shouldThrowWhenClasspathResourceMissing() {
        assertThrows(IllegalStateException.class, () -> AppConfig.loadFromClasspath("non_existent.properties"));
    }

    @Test
    @DisplayName("10. Should throw IllegalArgumentException when required config key is missing")
    void shouldThrowWhenRequiredConfigKeyMissing() {
        AppConfig empty = new AppConfig(new Properties());
        assertThrows(IllegalArgumentException.class, () -> empty.getRequiredPath("missing.key"));
    }

    @Test
    @DisplayName("11. Should detect JSON and YAML formats and reject unsupported extensions")
    void shouldDetectFormatsAndRejectUnsupported() {
        assertEquals(DataFormat.JSON, DataFormat.fromPath(Path.of("test.json")));
        assertEquals(DataFormat.YAML, DataFormat.fromPath(Path.of("test.yaml")));
        assertEquals(DataFormat.YAML, DataFormat.fromPath(Path.of("test.yml")));
        assertThrows(IllegalArgumentException.class, () -> DataFormat.fromPath(Path.of("test.xml")));
    }
}