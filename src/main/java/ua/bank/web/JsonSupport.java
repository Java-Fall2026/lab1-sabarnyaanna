package ua.bank.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import ua.bank.io.JacksonFactory;

public class JsonSupport {

    public static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";

    private final ObjectMapper mapper;
    private final Validator validator;

    public JsonSupport() {
        this.mapper = JacksonFactory.createJsonMapper();
        this.validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    public <T> T readValid(HttpServletRequest req, Class<T> clazz) throws IOException {
        String contentType = req.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("application/json")) {
            throw HttpApiException.unsupportedMediaType(
                    "Очікується Content-Type: application/json, отримано: " + contentType
            );
        }

        byte[] bodyBytes = req.getInputStream().readAllBytes();
        if (bodyBytes.length == 0) {
            throw HttpApiException.badRequest("Тіло JSON-запиту не може бути порожнім");
        }

        T obj;
        try {
            obj = mapper.readValue(bodyBytes, clazz);
        } catch (JsonProcessingException e) {
            throw HttpApiException.badRequest("Некоректний формат JSON: " + e.getOriginalMessage());
        }

        if (obj == null) {
            throw HttpApiException.badRequest("Десеріалізований об'єкт є порожнім (null)");
        }

        Set<ConstraintViolation<T>> violations = validator.validate(obj);
        if (!violations.isEmpty()) {
            List<String> details = violations.stream()
                    .map(v -> {
                        String path = v.getPropertyPath().toString();
                        return (path.isEmpty() ? "cross-field" : path) + ": " + v.getMessage();
                    })
                    .sorted()
                    .toList();
            throw HttpApiException.badRequest("Помилка валідації вхідних даних", details);
        }

        return obj;
    }

    public void write(HttpServletResponse resp, int status, Object body) throws IOException {
        resp.setStatus(status);
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType(JSON_CONTENT_TYPE);
        mapper.writeValue(resp.getOutputStream(), body);
    }
}