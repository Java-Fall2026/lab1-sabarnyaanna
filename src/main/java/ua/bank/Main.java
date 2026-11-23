package ua.bank;

import java.nio.file.Path;
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
import ua.bank.web.ApiServer;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        log.info("Starting application (Lab 7: Servlets and REST API)");

        AppConfig config = AppConfig.loadFrom(Path.of("app.properties"));
        int serverPort = config.getRequiredInt("server.port");
        Path borrowersInput = config.getRequiredPath("data.borrowers.file");
        Path contractsInput = config.getRequiredPath("data.contracts.file");
        Path borrowersOutput = config.getRequiredPath("data.output.borrowers.file");
        Path contractsOutput = config.getRequiredPath("data.output.contracts.file");

        FileStorage<Borrower> borrowerStorage = new FileStorage<>(Borrower.class);
        FileStorage<LoanContract> contractStorage = new FileStorage<>(LoanContract.class);
        DataLoader loader = new DataLoader();

        BorrowerRepository borrowerRepo = new BorrowerRepository();
        LoanContractRepository contractRepo = new LoanContractRepository();

        LoadResult borrowersRes = loader.loadInto(borrowersInput, borrowerStorage, borrowerRepo);
        LoadResult contractsRes = loader.loadInto(contractsInput, contractStorage, contractRepo);

        System.out.printf("Завантажено позичальників: прочитано=%d, додано=%d, відкинуто=%d%n",
                borrowersRes.readCount(), borrowersRes.addedCount(), borrowersRes.rejected().size());
        System.out.printf("Завантажено договорів: прочитано=%d, додано=%d, відкинуто=%d%n",
                contractsRes.readCount(), contractsRes.addedCount(), contractsRes.rejected().size());

        ApiServer server = new ApiServer(serverPort, borrowerRepo, contractRepo);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop();
                borrowerStorage.save(borrowersOutput, borrowerRepo.findAll());
                contractStorage.save(contractsOutput, contractRepo.findAll());
                log.info("Repositories saved on shutdown to '{}' and '{}'", borrowersOutput, contractsOutput);
            } catch (Exception e) {
                log.error("Error during server shutdown and save: {}", e.getMessage(), e);
            }
        }));

        server.start();
        System.out.println("REST API сервер запущено за адресою: http://localhost:" + server.port());
        System.out.println("Натисніть Enter у консолі або Ctrl+C для зупинки сервера та збереження даних...");

        System.in.read();
        System.exit(0);
    }
}