package ua.bank.repository;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.slf4j.LoggerFactory;
import ua.bank.model.Borrower;

public class SearchBenchmark {

    private static final int ELEMENT_COUNT = 100_000;
    private static final int WARMUP_ITERATIONS = 5_000;
    private static final int MEASURE_ITERATIONS = 20_000;
    private static final int MEASURE_RUNS = 15;

    public static void main(String[] args) {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        Level previousLevel = rootLogger.getLevel();
        rootLogger.setLevel(Level.OFF);

        try {
            IdentityExtractor<Borrower> extractor = Borrower::getPassportNum;
            GenericRepository<Borrower> repository = new GenericRepository<>("Borrower", extractor);

            for (int i = 0; i < ELEMENT_COUNT; i++) {
                String passport = String.format("AA%06d", i);
                repository.add(new Borrower(
                        passport,
                        "Позичальник " + i,
                        LocalDate.of(1990, 1, 1),
                        25000.0 + (i % 5000)
                ));
            }

            // Беремо список один раз із repository.findAll()
            List<Borrower> list = repository.findAll();

            String firstKey = "AA000000";
            String lastKey = String.format("AA%06d", ELEMENT_COUNT - 1);
            String missingKey = "ZZ999999";

            // Прогрів JVM для всіх трьох сценаріїв
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                repository.findByIdentity(firstKey);
                linearSearch(list, firstKey, extractor);
                repository.findByIdentity(lastKey);
                linearSearch(list, lastKey, extractor);
                repository.findByIdentity(missingKey);
                linearSearch(list, missingKey, extractor);
            }

            double firstMapNs = measureMedianPerOpNs(() -> repository.findByIdentity(firstKey));
            double firstListNs = measureMedianPerOpNs(() -> linearSearch(list, firstKey, extractor));

            double lastMapNs = measureMedianPerOpNs(() -> repository.findByIdentity(lastKey));
            double lastListNs = measureMedianPerOpNs(() -> linearSearch(list, lastKey, extractor));

            double missingMapNs = measureMedianPerOpNs(() -> repository.findByIdentity(missingKey));
            double missingListNs = measureMedianPerOpNs(() -> linearSearch(list, missingKey, extractor));

            System.out.printf("=== SearchBenchmark (elements=%d, ops per run=%d, runs=%d) ===%n",
                    ELEMENT_COUNT, MEASURE_ITERATIONS, MEASURE_RUNS);
            System.out.printf("Перший елемент (%s)    -> Map: %8.1f ns | List: %10.1f ns%n",
                    firstKey, firstMapNs, firstListNs);
            System.out.printf("Останній елемент (%s)  -> Map: %8.1f ns | List: %10.1f ns%n",
                    lastKey, lastMapNs, lastListNs);
            System.out.printf("Відсутній елемент (%s) -> Map: %8.1f ns | List: %10.1f ns%n",
                    missingKey, missingMapNs, missingListNs);
        } finally {
            rootLogger.setLevel(previousLevel);
        }
    }

    private static <E> Optional<E> linearSearch(List<E> items, String identity, IdentityExtractor<E> extractor) {
        for (E item : items) {
            if (extractor.identityOf(item).equals(identity)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    private static double measureMedianPerOpNs(Runnable action) {
        long[] runTotalsNs = new long[MEASURE_RUNS];
        for (int r = 0; r < MEASURE_RUNS; r++) {
            long start = System.nanoTime();
            for (int i = 0; i < MEASURE_ITERATIONS; i++) {
                action.run();
            }
            runTotalsNs[r] = System.nanoTime() - start;
        }
        Arrays.sort(runTotalsNs);
        long medianTotalNs = runTotalsNs[MEASURE_RUNS / 2];
        return (double) medianTotalNs / MEASURE_ITERATIONS;
    }
}