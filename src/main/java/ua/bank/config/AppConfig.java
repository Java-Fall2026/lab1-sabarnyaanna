package ua.bank.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Loads application configuration properties from the classpath.
 */
public class AppConfig {

    private final Properties properties = new Properties();

    public AppConfig(Properties properties) {
        this.properties.putAll(properties);
    }

    /**
     * Loads properties from a classpath resource (e.g. "app.properties").
     */
    public static AppConfig loadFromClasspath(String resourceName) {
        if (resourceName == null || resourceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Resource name must not be blank");
        }
        String normalized = resourceName.startsWith("/") ? resourceName.substring(1) : resourceName;
        Properties props = new Properties();
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(normalized)) {
            if (in == null) {
                throw new IllegalStateException("Classpath configuration resource not found: " + normalized);
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read configuration resource: " + normalized, e);
        }
        return new AppConfig(props);
    }

    public Path getRequiredPath(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required configuration key: " + key);
        }
        return Path.of(value.trim());
    }
}