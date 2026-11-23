package ua.bank.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import ua.bank.repository.SortOrder;

public final class QueryParamHelper {

    private QueryParamHelper() {
    }

    public static void validateAllowedParams(HttpServletRequest req, Set<String> allowed) {
        for (String param : req.getParameterMap().keySet()) {
            if (!allowed.contains(param)) {
                throw HttpApiException.badRequest("Невідомий параметр запиту: " + param);
            }
        }
    }

    public static SortOrder parseSortOrder(String rawOrder) {
        if (rawOrder == null || rawOrder.isBlank()) {
            return SortOrder.ASC;
        }
        try {
            return SortOrder.valueOf(rawOrder.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw HttpApiException.badRequest("Некоректне значення order (очікується ASC або DESC): " + rawOrder);
        }
    }

    public static int parseNonNegativeInt(String raw, String paramName, int defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            int val = Integer.parseInt(raw.trim());
            if (val < 0) {
                throw HttpApiException.badRequest("Параметр '" + paramName + "' не може бути від'ємним: " + val);
            }
            return val;
        } catch (NumberFormatException e) {
            throw HttpApiException.badRequest("Параметр '" + paramName + "' має бути цілим числом: " + raw);
        }
    }

    public static double parseDouble(String raw, String paramName) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            throw HttpApiException.badRequest("Параметр '" + paramName + "' має бути числом: " + raw);
        }
    }

    public static <T> List<T> paginate(List<T> items, int offset, Integer limit) {
        if (offset >= items.size()) {
            return List.of();
        }
        if (limit == null) {
            return items.subList(offset, items.size());
        }
        int end = Math.min(items.size(), offset + limit);
        return items.subList(offset, end);
    }

    public static String extractPathIdentity(HttpServletRequest req) {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.equals("/")) {
            return null;
        }
        String trimmed = pathInfo.startsWith("/") ? pathInfo.substring(1) : pathInfo;
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.contains("/")) {
            throw HttpApiException.notFound("Невідомий вкладений шлях: " + pathInfo);
        }
        return trimmed.isBlank() ? null : trimmed;
    }
}