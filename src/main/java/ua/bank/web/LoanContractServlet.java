package ua.bank.web;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import ua.bank.model.LoanContract;
import ua.bank.model.LoanType;
import ua.bank.repository.LoanContractRepository;
import ua.bank.repository.SortOrder;

public class LoanContractServlet extends HttpServlet {

    private static final Set<String> ALLOWED_QUERY_PARAMS = Set.of(
            "sort", "order", "borrowerName", "minAmount", "maxAmount", "loanType", "limit", "offset"
    );

    private final LoanContractRepository repository;
    private final JsonSupport jsonSupport;

    public LoanContractServlet(LoanContractRepository repository, JsonSupport jsonSupport) {
        this.repository = repository;
        this.jsonSupport = jsonSupport;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String id = QueryParamHelper.extractPathIdentity(req);

        // Окремий маршрут групування з ЛР 5: /api/contracts/grouped
        if ("grouped".equals(id)) {
            jsonSupport.write(resp, 200, repository.countByLoanType());
            return;
        }

        if (id != null) {
            LoanContract contract = repository.findByIdentity(id)
                    .orElseThrow(() -> HttpApiException.notFound("Кредитний договір не знайдено: " + id));
            jsonSupport.write(resp, 200, contract);
            return;
        }

        QueryParamHelper.validateAllowedParams(req, ALLOWED_QUERY_PARAMS);

        String sortField = req.getParameter("sort");
        SortOrder order = QueryParamHelper.parseSortOrder(req.getParameter("order"));
        String borrowerName = req.getParameter("borrowerName");
        String minAmountRaw = req.getParameter("minAmount");
        String maxAmountRaw = req.getParameter("maxAmount");
        String loanTypeRaw = req.getParameter("loanType");
        int offset = QueryParamHelper.parseNonNegativeInt(req.getParameter("offset"), "offset", 0);
        String limitRaw = req.getParameter("limit");
        Integer limit = limitRaw == null ? null : QueryParamHelper.parseNonNegativeInt(limitRaw, "limit", 0);

        List<LoanContract> list = resolveSortedList(sortField, order);

        // Фільтр 1: за ПІБ позичальника (метод пошуку з ЛР 5)
        if (borrowerName != null && !borrowerName.isBlank()) {
            Set<String> matchedIds = repository.searchByBorrowerName(borrowerName).stream()
                    .map(LoanContract::contractId)
                    .collect(Collectors.toSet());
            list = list.stream().filter(c -> matchedIds.contains(c.contractId())).toList();
        }

        // Фільтр 2: за діапазоном суми кредиту (метод findByPrincipalRange з ЛР 5)
        if (minAmountRaw != null || maxAmountRaw != null) {
            double min = minAmountRaw == null ? 0.0 : QueryParamHelper.parseDouble(minAmountRaw, "minAmount");
            double max = maxAmountRaw == null ? Double.MAX_VALUE : QueryParamHelper.parseDouble(maxAmountRaw, "maxAmount");
            if (min < 0 || max < 0 || min > max) {
                throw HttpApiException.badRequest("Некоректний діапазон суми кредиту");
            }
            Set<String> rangeIds = repository.findByPrincipalRange(min, max).stream()
                    .map(LoanContract::contractId)
                    .collect(Collectors.toSet());
            list = list.stream().filter(c -> rangeIds.contains(c.contractId())).toList();
        }

        // Фільтр 3: за типом кредиту (LoanType enum)
        if (loanTypeRaw != null && !loanTypeRaw.isBlank()) {
            LoanType type;
            try {
                type = LoanType.valueOf(loanTypeRaw.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw HttpApiException.badRequest("Невідомий тип кредиту loanType: " + loanTypeRaw);
            }
            list = list.stream().filter(c -> c.loanType() == type).toList();
        }

        List<LoanContract> page = QueryParamHelper.paginate(list, offset, limit);
        jsonSupport.write(resp, 200, page);
    }

    private List<LoanContract> resolveSortedList(String sortField, SortOrder order) {
        if (sortField == null || sortField.isBlank() || "contractId".equals(sortField)) {
            return repository.findAllSortedByIdentity(order);
        }
        Comparator<LoanContract> comparator = switch (sortField) {
            case "principalAmount" -> Comparator.comparingDouble(LoanContract::principalAmount);
            case "termMonths" -> Comparator.comparingInt(LoanContract::termMonths);
            case "issueDate" -> Comparator.comparing(LoanContract::issueDate);
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
            throw HttpApiException.methodNotAllowed("POST дозволено лише на колекцію /api/contracts");
        }
        LoanContract contract = jsonSupport.readValid(req, LoanContract.class);
        repository.add(contract);

        String location = req.getRequestURI() + "/" + repository.identityOf(contract);
        resp.setHeader("Location", location);
        jsonSupport.write(resp, 201, contract);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathId = QueryParamHelper.extractPathIdentity(req);
        if (pathId == null) {
            throw HttpApiException.methodNotAllowed("PUT вимагає ідентифікатор у шляху /api/contracts/{contractId}");
        }
        LoanContract contract = jsonSupport.readValid(req, LoanContract.class);
        String bodyId = repository.identityOf(contract);
        if (!pathId.equals(bodyId)) {
            throw HttpApiException.badRequest(
                    "Ідентифікатор у шляху ('" + pathId + "') не збігається з ідентифікатором у тілі ('" + bodyId + "')"
            );
        }
        LoanContract updated = repository.replace(contract);
        jsonSupport.write(resp, 200, updated);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        String pathId = QueryParamHelper.extractPathIdentity(req);
        if (pathId == null) {
            throw HttpApiException.methodNotAllowed("DELETE вимагає ідентифікатор у шляху /api/contracts/{contractId}");
        }
        boolean removed = repository.removeByIdentity(pathId);
        if (!removed) {
            throw HttpApiException.notFound("Кредитний договір не знайдено: " + pathId);
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