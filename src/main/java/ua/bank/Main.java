package ua.bank;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
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

/**
 * Entry point for Lab 6 demonstrating classpath configuration loading,
 * JSON/YAML serialization, Jakarta Validation, and round-trip verification.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws IOException {
        log.info("Starting Lab 6 application");

        // 1. Load and display configuration from classpath
        AppConfig config = AppConfig.loadFromClasspath("app.properties");
        Path borrowersInput = config.getRequiredPath("data.borrowers.file");
        Path contractsInput = config.getRequiredPath("data.contracts.file");
        Path borrowersOutput = config.getRequiredPath("data.output.borrowers.file");
        Path contractsOutput = config.getRequiredPath("data.output.contracts.file");

        System.out.println("=== Loaded Configuration (classpath: app.properties) ===");
        System.out.println("data.borrowers.file        = " + borrowersInput);
        System.out.println("data.contracts.file        = " + contractsInput);
        System.out.println("data.output.borrowers.file = " + borrowersOutput);
        System.out.println("data.output.contracts.file = " + contractsOutput);

        // 2. Load and validate JSON and YAML files into repositories
        FileStorage<Borrower> borrowerStorage = new FileStorage<>(Borrower.class);
        FileStorage<LoanContract> contractStorage = new FileStorage<>(LoanContract.class);
        DataLoader loader = new DataLoader();

        BorrowerRepository borrowerRepo = new BorrowerRepository();
        LoanContractRepository contractRepo = new LoanContractRepository();

        LoadResult borrowersRes = loader.loadInto(borrowersInput, borrowerStorage, borrowerRepo);
        LoadResult contractsRes = loader.loadInto(contractsInput, contractStorage, contractRepo);

        System.out.printf("%nBorrowers loaded: read=%d, added=%d, rejectedRecords=%d%n",
                borrowersRes.readCount(), borrowersRes.addedCount(), borrowersRes.rejectedCount());
        System.out.printf("Contracts loaded: read=%d, added=%d, rejectedRecords=%d%n",
                contractsRes.readCount(), contractsRes.addedCount(), contractsRes.rejectedCount());

        // 3. Save repositories to new files and verify round-trip equality
        borrowerStorage.save(borrowersOutput, borrowerRepo.findAll());
        contractStorage.save(contractsOutput, contractRepo.findAll());

        List<Borrower> reloadedBorrowers = borrowerStorage.load(borrowersOutput);
        List<LoanContract> reloadedContracts = contractStorage.load(contractsOutput);

        boolean borrowersMatch = borrowerRepo.findAll().equals(reloadedBorrowers);
        boolean contractsMatch = contractRepo.findAll().equals(reloadedContracts);

        System.out.println("\n=== Round-Trip File Verification ===");
        System.out.println("Reloaded borrowers match repository : " + borrowersMatch + " (count=" + reloadedBorrowers.size() + ")");
        System.out.println("Reloaded contracts match repository : " + contractsMatch + " (count=" + reloadedContracts.size() + ")");

        if (!borrowersMatch || !contractsMatch) {
            throw new IllegalStateException("Reloaded file content does not match repository state!");
        }

        log.info("Lab 6 application completed successfully");
    }
}