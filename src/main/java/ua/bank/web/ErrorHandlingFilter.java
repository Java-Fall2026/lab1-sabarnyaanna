package ua.bank.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.bank.repository.DuplicateIdentityException;

public class ErrorHandlingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(ErrorHandlingFilter.class);
    private final JsonSupport jsonSupport;

    public ErrorHandlingFilter(JsonSupport jsonSupport) {
        this.jsonSupport = jsonSupport;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getRequestURI();

        try {
            chain.doFilter(request, response);
        } catch (HttpApiException e) {
            ApiError error = ApiError.of(e.getStatus(), e.getErrorTitle(), e.getMessage(), path, e.getDetails());
            jsonSupport.write(resp, e.getStatus(), error);
        } catch (DuplicateIdentityException e) {
            ApiError error = ApiError.of(409, "Conflict", e.getMessage(), path);
            jsonSupport.write(resp, 409, error);
        } catch (NoSuchElementException e) {
            ApiError error = ApiError.of(404, "Not Found", e.getMessage(), path);
            jsonSupport.write(resp, 404, error);
        } catch (IllegalArgumentException e) {
            ApiError error = ApiError.of(400, "Bad Request", e.getMessage(), path);
            jsonSupport.write(resp, 400, error);
        } catch (Exception e) {
            log.error("Unhandled internal server error on {} {}", req.getMethod(), path, e);
            ApiError error = ApiError.of(500, "Internal Server Error", "Внутрішня помилка сервера", path);
            jsonSupport.write(resp, 500, error);
        }
    }
}