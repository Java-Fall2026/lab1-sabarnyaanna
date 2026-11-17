package ua.bank.io;

import java.nio.file.Path;

public enum DataFormat {
    JSON,
    YAML;

    public static DataFormat fromPath(Path file) {
        if (file == null || file.getFileName() == null) {
            throw new IllegalArgumentException("Шлях до файлу не може бути null");
        }
        String name = file.getFileName().toString().toLowerCase();
        if (name.endsWith(".json")) {
            return JSON;
        }
        if (name.endsWith(".yaml") || name.endsWith(".yml")) {
            return YAML;
        }
        throw new IllegalArgumentException("Непідтримуване розширення файлу: " + file.getFileName());
    }
}