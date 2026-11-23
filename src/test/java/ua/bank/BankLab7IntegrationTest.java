package ua.bank;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ua.bank.model.BankBranch;
import ua.bank.model.Borrower;
import ua.bank.model.LoanContract;
import ua.bank.model.LoanType;
import ua.bank.repository.BorrowerRepository;
import ua.bank.repository.LoanContractRepository;
import ua.bank.web.ApiServer;

class BankLab7IntegrationTest {

    private BorrowerRepository borrowerRepo;
    private LoanContractRepository contractRepo;
    private ApiServer server;
    private HttpClient client;
    private String baseUrl;

    @BeforeEach
    void setUp() throws Exception {
        borrowerRepo = new BorrowerRepository();
        contractRepo = new LoanContractRepository();

        Borrower b1 = new Borrower("AA100001", "Олена Ковальчук", LocalDate.of(1995, 5, 14), 35000.0);
        Borrower b2 = new Borrower("BB200002", "Тарас Бондар", LocalDate.of(1988, 9, 22), 60000.0);
        borrowerRepo.add(b1);
        borrowerRepo.add(b2);

        BankBranch kyiv = BankBranch.of("BR-01", "Київ");
        contractRepo.add(new LoanContract("LN-2026-01", b1, kyiv, LoanType.MORTGAGE, 800000.0, LocalDate.of(2026, 5, 1), 120));
        contractRepo.add(new LoanContract("LN-2026-02", b2, kyiv, LoanType.AUTO, 400000.0, LocalDate.of(2026, 6, 1), 36));

        server = new ApiServer(0, borrowerRepo, contractRepo);
        server.start();
        baseUrl = "http://localhost:" + server.port();
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() throws Exception {
        server.stop();
    }

    @Test
    @DisplayName("1. Should start server on dynamic port 0 and return 200 OK with UTF-8 JSON for GET collection")
    void shouldGetBorrowersCollectionWithUtf8() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/borrowers")).GET().build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertTrue(resp.headers().firstValue("Content-Type").orElse("").contains("application/json"));
        assertTrue(resp.body().contains("Олена Ковальчук"));
    }

    @Test
    @DisplayName("2. Should get entity by identity (200) and return 404 when missing")
    void shouldGetByIdentityAndReturn404WhenNotFound() throws Exception {
        HttpResponse<String> okResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/borrowers/AA100001")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(200, okResp.statusCode());
        assertTrue(okResp.body().contains("AA100001"));

        HttpResponse<String> notFoundResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/borrowers/ZZ999999")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(404, notFoundResp.statusCode());
        assertTrue(notFoundResp.body().contains("\"status\" : 404"));
    }

    @Test
    @DisplayName("3. Should create entity via POST with 201 Created and Location header")
    void shouldCreateBorrowerWith201AndLocation() throws Exception {
        String json = """
                {
                  "passportNum": "CC300003",
                  "fullName": "Ірина Савченко",
                  "birthDate": "1997-03-12",
                  "monthlyIncome": 42000.0
                }
                """;
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/borrowers"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode());
        assertEquals("/api/borrowers/CC300003", resp.headers().firstValue("Location").orElse(""));
        assertTrue(resp.body().contains("Ірина Савченко"));
    }

    @Test
    @DisplayName("4. Should return 409 Conflict when POSTing duplicate identity")
    void shouldReturn409OnDuplicatePost() throws Exception {
        String duplicateJson = """
                {
                  "passportNum": "AA100001",
                  "fullName": "Дублікат Клієнта",
                  "birthDate": "1995-05-14",
                  "monthlyIncome": 35000.0
                }
                """;
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/borrowers"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(duplicateJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(409, resp.statusCode());
    }

    @Test
    @DisplayName("5. Should update existing entity via PUT (200) and reject path/body identity mismatch (400)")
    void shouldUpdateViaPutAndRejectIdMismatch() throws Exception {
        String validPut = """
                {
                  "passportNum": "AA100001",
                  "fullName": "Олена Ковальчук-Оновлена",
                  "birthDate": "1995-05-14",
                  "monthlyIncome": 49000.0
                }
                """;
        HttpResponse<String> okResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/borrowers/AA100001"))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(validPut, StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(200, okResp.statusCode());
        assertTrue(okResp.body().contains("Олена Ковальчук-Оновлена"));

        HttpResponse<String> mismatchResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/borrowers/BB200002"))
                        .header("Content-Type", "application/json")
                        .PUT(HttpRequest.BodyPublishers.ofString(validPut, StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(400, mismatchResp.statusCode());
    }

    @Test
    @DisplayName("6. Should delete entity with 204 No Content and return 404 on second delete")
    void shouldDeleteWith204AndReturn404WhenAlreadyDeleted() throws Exception {
        HttpResponse<String> del1 = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/borrowers/BB200002")).DELETE().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(204, del1.statusCode());

        HttpResponse<String> del2 = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/borrowers/BB200002")).DELETE().build(),
                HttpResponse.BodyHandlers.ofString()
        );
        assertEquals(404, del2.statusCode());
    }

    @Test
    @DisplayName("7. Should return 400 with all Jakarta Validation violations including custom @ValidLoanLimit")
    void shouldReturn400WithAllValidationViolations() throws Exception {
        String invalidContractJson = """
                {
                  "contractId": "BAD-ID",
                  "borrower": {
                    "passportNum": "AA100001",
                    "fullName": "Олена Ковальчук",
                    "birthDate": "1995-05-14",
                    "monthlyIncome": 20000.0
                  },
                  "branch": {
                    "branchCode": "BR-01",
                    "city": "Київ"
                  },
                  "loanType": "MORTGAGE",
                  "principalAmount": 5000000.0,
                  "issueDate": "2026-10-01",
                  "termMonths": 500
                }
                """;
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/contracts"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(invalidContractJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(400, resp.statusCode());
        assertTrue(resp.body().contains("contractId"));
        assertTrue(resp.body().contains("termMonths"));
        assertTrue(resp.body().contains("60 місячних доходів"));
    }

    @Test
    @DisplayName("8. Should return 400 on malformed JSON or unknown JSON property without stack trace")
    void shouldReturn400OnBrokenJsonAndUnknownFields() throws Exception {
        HttpResponse<String> brokenResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/borrowers"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString("{ broken json [", StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(400, brokenResp.statusCode());
        assertFalse(brokenResp.body().contains("Exception"));

        String unknownFieldJson = """
                {
                  "passportNum": "DD400004",
                  "fullName": "Тест Тестович",
                  "birthDate": "1990-01-01",
                  "monthlyIncome": 30000.0,
                  "secretHackerField": 123
                }
                """;
        HttpResponse<String> unknownResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/borrowers"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(unknownFieldJson, StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(400, unknownResp.statusCode());
    }

    @Test
    @DisplayName("9. Should return 415 Unsupported Media Type when Content-Type is not application/json")
    void shouldReturn415WhenContentTypeInvalid() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/borrowers"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString("hello"))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(415, resp.statusCode());
    }

    @Test
    @DisplayName("10. Should return 405 Method Not Allowed on unsupported HTTP method")
    void shouldReturn405OnUnsupportedMethod() throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/borrowers"))
                .method("PATCH", HttpRequest.BodyPublishers.ofString("{}"))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertEquals(405, resp.statusCode());
    }

    @Test
    @DisplayName("11. Should filter, sort, paginate and reject invalid query params with 400")
    void shouldFilterSortPaginateAndRejectInvalidQueryParams() throws Exception {
        HttpResponse<String> filtered = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/borrowers?sort=monthlyIncome&order=DESC&minIncome=50000&limit=1&offset=0"))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(200, filtered.statusCode());
        assertTrue(filtered.body().contains("Тарас Бондар"));
        assertFalse(filtered.body().contains("Олена Ковальчук"));

        HttpResponse<String> badQuery = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/borrowers?sort=invalidColumn")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(400, badQuery.statusCode());
    }

    @Test
    @DisplayName("12. Should return aggregation and grouping results on dedicated endpoints")
    void shouldReturnAggregationAndGroupingEndpoints() throws Exception {
        HttpResponse<String> statsResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/borrowers/statistics")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(200, statsResp.statusCode());
        assertTrue(statsResp.body().contains("\"count\" : 2"));

        HttpResponse<String> groupedResp = client.send(
                HttpRequest.newBuilder(URI.create(baseUrl + "/api/contracts/grouped")).GET().build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        assertEquals(200, groupedResp.statusCode());
        assertTrue(groupedResp.body().contains("MORTGAGE"));
    }

    @Test
    @DisplayName("13. Should handle concurrent POST requests with same identity: exactly one 201 and one 409")
    void shouldHandleConcurrentPostWithSameIdentity() throws Exception {
        String payload = """
                {
                  "passportNum": "EE500005",
                  "fullName": "Паралельний Клієнт",
                  "birthDate": "1993-08-15",
                  "monthlyIncome": 51000.0
                }
                """;

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Integer> task = () -> {
                HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/borrowers"))
                        .header("Content-Type", "application/json; charset=UTF-8")
                        .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                        .build();
                return client.send(req, HttpResponse.BodyHandlers.ofString()).statusCode();
            };

            List<Future<Integer>> futures = pool.invokeAll(List.of(task, task));
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> f : futures) {
                statuses.add(f.get());
            }

            assertTrue(statuses.contains(201), "Один із паралельних запитів має повернути 201");
            assertTrue(statuses.contains(409), "Другий паралельний запит має повернути 409");
        } finally {
            pool.shutdownNow();
        }
    }
}