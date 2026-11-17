package ua.bank.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class AppConfig {

    private final Properties properties = new Properties();

    public AppConfig(Properties properties) {
        this.properties.putAll(properties);
    }

    public static AppConfig loadFrom(Path path) {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(path)) {
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Не вдалося завантажити файл конфігурації: " + path, e);
        }
        return new AppConfig(props);
    }

    public Path getRequiredPath(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Відсутній обов'язковий ключ у конфігурації: " + key);
        }
        return Path.of(value.trim());
    }
}