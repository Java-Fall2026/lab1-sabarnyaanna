package ua.bank.web;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ua.bank.model.Borrower;
import ua.bank.repository.BorrowerRepository;
import ua.bank.repository.SortOrder;

public class BorrowerServlet extends HttpServlet {

    private static final Set<String> ALLOWED_QUERY_PARAMS = Set.of(
            "sort", "order", "name", "minIncome", "maxIncome", "limit", "offset"
    );

    private final BorrowerRepository repository;
    private final JsonSupport jsonSupport;

    public BorrowerServlet(BorrowerRepository repository, JsonSupport jsonSupport) {
        this.repository = repository;
        this.jsonSupport = jsonSupport;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String id = QueryParamHelper.extractPathIdentity(req);

        // Окремий маршрут агрегації з ЛР 5: /api/borrowers/statistics
        if ("statistics".equals(id)) {
            DoubleSummaryStatistics stats = repository.getIncomeStatistics();
            Map<String, Object> body = Map.of(
                    "count", stats.getCount(),
                    "sum", stats.getSum(),
                    "average", stats.getAverage(),
                    "min", stats.getCount() == 0 ? 0.0 : stats.getMin(),
                    "max", stats.getCount() == 0 ? 0.0 : stats.getMax()
            );
            jsonSupport.write(resp, 200, body);
            return;
        }

        if (id != null) {
            Borrower borrower = repository.findByIdentity(id)
                    .orElseThrow(() -> HttpApiException.notFound("Позичальника не знайдено: " + id));
            jsonSupport.write(resp, 200, borrower);
            return;
        }

        QueryParamHelper.validateAllowedParams(req, ALLOWED_QUERY_PARAMS);

        String sortField = req.getParameter("sort");
        SortOrder order = QueryParamHelper.parseSortOrder(req.getParameter("order"));
        String nameFilter = req.getParameter("name");
        String minIncomeRaw = req.getParameter("minIncome");
        String maxIncomeRaw = req.getParameter("maxIncome");
        int offset = QueryParamHelper.parseNonNegativeInt(req.getParameter("offset"), "offset", 0);
        String limitRaw = req.getParameter("limit");
        Integer limit = limitRaw == null ? null : QueryParamHelper.parseNonNegativeInt(limitRaw, "limit", 0);

        List<Borrower> list = resolveSortedList(sortField, order);

        // Фільтр 1: за частиною ПІБ (використовує логіку пошуку з ЛР 5)
        if (nameFilter != null && !nameFilter.isBlank()) {
            Set<String> matchedIds = repository.searchByNameContaining(nameFilter).stream()
                    .map(Borrower::getPassportNum)
                    .collect(java.util.stream.Collectors.toSet());
            list = list.stream().filter(b -> matchedIds.contains(b.getPassportNum())).toList();
        }

        // Фільтр 2: за діапазоном доходу (використовує findByIncomeRange з ЛР 5)
        if (minIncomeRaw != null || maxIncomeRaw != null) {
            double min = minIncomeRaw == null ? 0.0 : QueryParamHelper.parseDouble(minIncomeRaw, "minIncome");
            double max = maxIncomeRaw == null ? Double.MAX_VALUE : QueryParamHelper.parseDouble(maxIncomeRaw, "maxIncome");
            if (min < 0 || max < 0 || min > max) {
                throw HttpApiException.badRequest("Некоректний діапазон доходу: minIncome=" + min + ", maxIncome=" + max);
            }
            Set<String> rangeIds = repository.findByIncomeRange(min, max).stream()
                    .map(Borrower::getPassportNum)
                    .collect(java.util.stream.Collectors.toSet());
            list = list.stream().filter(b -> rangeIds.contains(b.getPassportNum())).toList();
        }

        List<Borrower> page = QueryParamHelper.paginate(list, offset, limit);
        jsonSupport.write(resp, 200, page);
    }

    private List<Borrower> resolveSortedList(String sortField, SortOrder order) {
        if (sortField == null || sortField.isBlank() || "passportNum".equals(sortField)) {
            return repository.findAllSortedByIdentity(order);
        }
        Comparator<Borrower> comparator = switch (sortField) {
            case "fullName" -> Comparator.comparing(Borrower::getFullName);
            case "monthlyIncome" -> Comparator.comparingDouble(Borrower::getMonthlyIncome);
            case "birthDate" -> Comparator.comparing(Borrower::getBirthDate);
            default -> throw HttpApiException.badRequest("Невідоме поле сортування: " + sortField);
        };
        if (order == SortOrder.DESC) {
            comparator = comparator.reversed();
        }
        return repository.findAllSorted(comparator);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (QueryParamHelper.extractPathIdentity(req) != null) {
            throw HttpApiException.methodNotAllowed("POST дозволено лише на колекцію /api/borrowers");
        }
        Borrower borrower = jsonSupport.readValid(req, Borrower.class);
        repository.add(borrower);

        String location = req.getRequestURI() + "/" + repository.identityOf(borrower);
        resp.setHeader("Location", location);
        jsonSupport.write(resp, 201, borrower);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathId = QueryParamHelper.extractPathIdentity(req);
        if (pathId == null) {
            throw HttpApiException.methodNotAllowed("PUT вимагає ідентифікатор у шляху /api/borrowers/{passportNum}");
        }
        Borrower borrower = jsonSupport.readValid(req, Borrower.class);
        String bodyId = repository.identityOf(borrower);
        if (!pathId.equals(bodyId)) {
            throw HttpApiException.badRequest(
                    "Ідентифікатор у шляху ('" + pathId + "') не збігається з ідентифікатором у тілі ('" + bodyId + "')"
            );
        }
        Borrower updated = repository.replace(borrower);
        jsonSupport.write(resp, 200, updated);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        String pathId = QueryParamHelper.extractPathIdentity(req);
        if (pathId == null) {
            throw HttpApiException.methodNotAllowed("DELETE вимагає ідентифікатор у шляху /api/borrowers/{passportNum}");
        }
        boolean removed = repository.removeByIdentity(pathId);
        if (!removed) {
            throw HttpApiException.notFound("Позичальника не знайдено: " + pathId);
        }
        resp.setStatus(204);
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws jakarta.servlet.ServletException, IOException {
        String method = req.getMethod();
        if (!Set.of("GET", "POST", "PUT", "DELETE").contains(method)) {
            throw HttpApiException.methodNotAllowed("HTTP-метод не підтримується: " + method);
        }
        super.service(req, resp);
    }
}