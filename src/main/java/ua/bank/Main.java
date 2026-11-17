package ua.bank;

import java.nio.file.Path;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.bank.config.AppConfig;
import ua.bank.io.DataLoader;
import ua.bank.io.FileStorage;
import ua.bank.io.LoadResult;
import ua.bank.model.Borrower;
import ua.bank.model.LoanContract;
import ua.bank.repository.BorrowerRepository;
import ua.bank.repository.LoanContractRepository;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("=== Запуск програми (Лабораторна робота №6: JSON/YAML, Jackson, Jakarta Validation) ===");

        // 1. Читання конфігурації з app.properties
        AppConfig config = AppConfig.loadFrom(Path.of("app.properties"));
        Path borrowersJsonPath = config.getRequiredPath("data.borrowers.file");
        Path contractsYamlPath = config.getRequiredPath("data.contracts.file");
        Path brokenJsonPath = config.getRequiredPath("data.broken.file");
        Path exportedYamlPath = config.getRequiredPath("data.output.borrowers.file");

        FileStorage<Borrower> borrowerStorage = new FileStorage<>(Borrower.class);
        FileStorage<LoanContract> contractStorage = new FileStorage<>(LoanContract.class);
        DataLoader loader = new DataLoader();

        BorrowerRepository borrowerRepo = new BorrowerRepository();
        LoanContractRepository contractRepo = new LoanContractRepository();

        // 2. Завантаження обох файлів (JSON та YAML)
        LoadResult borrowersResult = loader.loadInto(borrowersJsonPath, borrowerStorage, borrowerRepo);
        LoadResult contractsResult = loader.loadInto(contractsYamlPath, contractStorage, contractRepo);

        System.out.printf("%nПідсумок завантаження позичальників (JSON): прочитано=%d, додано=%d, відкинуто=%d%n",
                borrowersResult.readCount(), borrowersResult.addedCount(), borrowersResult.rejected().size());
        borrowersResult.rejected().forEach(r -> System.out.println("  -> " + r));

        System.out.printf("%nПідсумок завантаження договорів (YAML): прочитано=%d, додано=%d, відкинуто=%d%n",
                contractsResult.readCount(), contractsResult.addedCount(), contractsResult.rejected().size());
        contractsResult.rejected().forEach(r -> System.out.println("  -> " + r));

        // 3. Спроба завантажити broken.json (програма не падає, помилка фіксується у LoadResult)
        System.out.println("\n=== Спроба завантажити синтаксично пошкоджений broken.json ===");
        LoadResult brokenResult = loader.loadInto(brokenJsonPath, borrowerStorage, borrowerRepo);
        brokenResult.rejected().forEach(r -> System.out.println("  -> " + r));

        // 4. Зміна вмісту репозиторію та збереження у новий файл (YAML)
        System.out.println("\n=== Оновлення репозиторію та експорт у новий файл ===");
        borrowerRepo.removeByIdentity("DD100004");
        borrowerRepo.add(new Borrower("ZZ999888", "Новий Клієнт Експорту", LocalDate.of(1994, 4, 20), 75000.0));

        try {
            borrowerStorage.save(exportedYamlPath, borrowerRepo.getAll());
        } catch (Exception e) {
            log.error("Помилка збереження файлу {}: {}", exportedYamlPath, e.getMessage(), e);
        }

        // 5. Повторне завантаження збереженого файлу у чистий репозиторій
        BorrowerRepository reloadedRepo = new BorrowerRepository();
        LoadResult reloadResult = loader.loadInto(exportedYamlPath, borrowerStorage, reloadedRepo);
        System.out.printf("Повторне завантаження '%s': прочитано=%d, додано=%d, відкинуто=%d%n",
                exportedYamlPath, reloadResult.readCount(), reloadResult.addedCount(), reloadResult.rejected().size());

        log.info("=== Роботу програми успішно завершено ===");
    }
}