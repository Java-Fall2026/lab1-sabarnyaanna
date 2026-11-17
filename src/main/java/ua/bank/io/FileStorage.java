package ua.bank.io;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileStorage<T> {

    private static final Logger log = LoggerFactory.getLogger(FileStorage.class);

    private final Class<T> entityClass;
    private final ObjectMapper jsonMapper;
    private final ObjectMapper yamlMapper;

    public FileStorage(Class<T> entityClass) {
        this.entityClass = Objects.requireNonNull(entityClass, "entityClass must not be null");
        this.jsonMapper = configureMapper(new ObjectMapper());
        this.yamlMapper = configureMapper(new ObjectMapper(new YAMLFactory()));
    }

    private ObjectMapper configureMapper(ObjectMapper mapper) {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    private ObjectMapper getMapperFor(Path file) {
        DataFormat format = DataFormat.fromPath(file);
        return switch (format) {
            case JSON -> jsonMapper;
            case YAML -> yamlMapper;
        };
    }

    public List<T> load(Path file) throws IOException {
        ObjectMapper mapper = getMapperFor(file);
        JavaType listType = mapper.getTypeFactory().constructCollectionType(List.class, entityClass);
        List<T> items = mapper.readValue(file.toFile(), listType);
        log.info("Read {} raw records of {} from {}", items.size(), entityClass.getSimpleName(), file);
        return items;
    }

    public void save(Path file, List<T> items) throws IOException {
        ObjectMapper mapper = getMapperFor(file);
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        mapper.writeValue(file.toFile(), items);
        log.info("Saved {} records of {} to {}", items.size(), entityClass.getSimpleName(), file);
    }
}