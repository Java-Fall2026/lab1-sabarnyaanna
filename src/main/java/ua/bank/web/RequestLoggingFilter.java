package ua.bank.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RequestLoggingFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        long startNs = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            double durationMs = (System.nanoTime() - startNs) / 1_000_000.0;
            int status = resp.getStatus();
            String method = req.getMethod();
            String path = req.getRequestURI();

            if (status >= 500) {
                log.error("{} {} -> {} ({} ms)", method, path, status, String.format("%.2f", durationMs));
            } else if (status >= 400) {
                log.warn("{} {} -> {} ({} ms)", method, path, status, String.format("%.2f", durationMs));
            } else {
                log.info("{} {} -> {} ({} ms)", method, path, status, String.format("%.2f", durationMs));
            }
        }
    }
}