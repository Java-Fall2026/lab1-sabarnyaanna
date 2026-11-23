package ua.bank.benchmark;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.slf4j.LoggerFactory;
import ua.bank.model.Borrower;
import ua.bank.repository.GenericRepository;

public class SearchBenchmark {

    private static final int ELEMENT_COUNT = 100_000;
    private static final int WARMUP_RUNS = 5;
    private static final int MEASURE_RUNS = 15;
    private static final int QUERIES_PER_RUN = 1_000;

    public static void main(String[] args) {
        // Вимикаємо логи на час бенчмарку
        Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        Level previousLevel = rootLogger.getLevel();
        rootLogger.setLevel(Level.OFF);

        try {
            GenericRepository<Borrower> repository = new GenericRepository<>("Borrower", Borrower::getPassportNum);
            List<Borrower> listStorage = new ArrayList<>(ELEMENT_COUNT);

            for (int i = 0; i < ELEMENT_COUNT; i++) {
                String passport = String.format("AA%06d", i);
                Borrower b = new Borrower(passport, "Позичальник " + i, LocalDate.of(1990, 1, 1), 25000.0 + (i % 5000));
                repository.add(b);
                listStorage.add(b);
            }

            // Шукаємо ключі з початку, середини, кінця та відсутній ключ
            List<String> targetKeys = new ArrayList<>(QUERIES_PER_RUN);
            for (int i = 0; i < QUERIES_PER_RUN; i++) {
                int idx = (i * 997) % ELEMENT_COUNT;
                targetKeys.add(i % 10 == 0 ? "ZZ999999" : String.format("AA%06d", idx));
            }

            // Прогрів JVM (JIT-компіляція)
            for (int w = 0; w < WARMUP_RUNS; w++) {
                runRepoSearch(repository, targetKeys);
                runListSearch(listStorage, targetKeys);
            }

            long[] repoTimesNs = new long[MEASURE_RUNS];
            long[] listTimesNs = new long[MEASURE_RUNS];

            for (int r = 0; r < MEASURE_RUNS; r++) {
                long startRepo = System.nanoTime();
                runRepoSearch(repository, targetKeys);
                repoTimesNs[r] = System.nanoTime() - startRepo;

                long startList = System.nanoTime();
                runListSearch(listStorage, targetKeys);
                listTimesNs[r] = System.nanoTime() - startList;
            }

            double repoMedianMs = median(repoTimesNs) / 1_000_000.0;
            double listMedianMs = median(listTimesNs) / 1_000_000.0;

            System.out.printf("=== SearchBenchmark (elements=%d, queries per run=%d, runs=%d) ===%n",
                    ELEMENT_COUNT, QUERIES_PER_RUN, MEASURE_RUNS);
            System.out.printf("GenericRepository (Map O(1)) median: %.4f ms%n", repoMedianMs);
            System.out.printf("ArrayList linear scan O(N)   median: %.4f ms%n", listMedianMs);
            System.out.printf("Speedup factor: %.1fx%n", listMedianMs / repoMedianMs);
        } finally {
            rootLogger.setLevel(previousLevel);
        }
    }

    private static int runRepoSearch(GenericRepository<Borrower> repo, List<String> keys) {
        int found = 0;
        for (String key : keys) {
            if (repo.findByIdentity(key).isPresent()) {
                found++;
            }
        }
        return found;
    }

    private static int runListSearch(List<Borrower> list, List<String> keys) {
        int found = 0;
        for (String key : keys) {
            Optional<Borrower> match = Optional.empty();
            for (Borrower b : list) {
                if (b.getPassportNum().equals(key)) {
                    match = Optional.of(b);
                    break;
                }
            }
            if (match.isPresent()) {
                found++;
            }
        }
        return found;
    }

    private static long median(long[] values) {
        long[] copy = Arrays.copyOf(values, values.length);
        Arrays.sort(copy);
        int mid = copy.length / 2;
        if (copy.length % 2 == 0) {
            return (copy[mid - 1] + copy[mid]) / 2;
        }
        return copy[mid];
    }
}