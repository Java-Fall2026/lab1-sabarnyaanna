package ua.bank.io;

import java.nio.file.Path;

/**
 * Supported serialization formats determined by file extension.
 */
public enum DataFormat {
    JSON,
    YAML;

    public static DataFormat fromPath(Path path) {
        if (path == null) {
            throw new IllegalArgumentException("File path must not be null");
        }
        String fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".json")) {
            return JSON;
        }
        if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
            return YAML;
        }
        throw new IllegalArgumentException("Unsupported file format (expected .json, .yaml, or .yml): " + path);
    }
}