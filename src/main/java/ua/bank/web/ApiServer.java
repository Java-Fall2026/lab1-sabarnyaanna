package ua.bank.web;

import jakarta.servlet.DispatcherType;
import java.util.EnumSet;
import org.eclipse.jetty.ee10.servlet.FilterHolder;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ua.bank.repository.BorrowerRepository;
import ua.bank.repository.LoanContractRepository;

public class ApiServer {

    private static final Logger log = LoggerFactory.getLogger(ApiServer.class);

    private final int configuredPort;
    private final Server server;

    public ApiServer(int port, BorrowerRepository borrowerRepo, LoanContractRepository contractRepo) {
        this.configuredPort = port;
        this.server = new Server(port);

        JsonSupport jsonSupport = new JsonSupport();
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");

        EnumSet<DispatcherType> dispatchers = EnumSet.of(DispatcherType.REQUEST);
        context.addFilter(new FilterHolder(new RequestLoggingFilter()), "/*", dispatchers);
        context.addFilter(new FilterHolder(new ErrorHandlingFilter(jsonSupport)), "/*", dispatchers);

        context.addServlet(new ServletHolder(new BorrowerServlet(borrowerRepo, jsonSupport)), "/api/borrowers/*");
        context.addServlet(new ServletHolder(new LoanContractServlet(contractRepo, jsonSupport)), "/api/contracts/*");

        server.setHandler(context);
    }

    public void start() throws Exception {
        server.start();
        log.info("Jetty ApiServer started on http://localhost:{}", port());
    }

    public void stop() throws Exception {
        if (server.isRunning()) {
            server.stop();
            log.info("Jetty ApiServer stopped");
        }
    }

    public int port() {
        if (server.getConnectors().length > 0 && server.getConnectors()[0] instanceof ServerConnector connector) {
            int localPort = connector.getLocalPort();
            if (localPort > 0) {
                return localPort;
            }
        }
        return configuredPort;
    }
}