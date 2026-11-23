package ua.bank.io;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public final class JacksonFactory {

    private JacksonFactory() {
    }

    public static ObjectMapper createJsonMapper() {
        return configure(new ObjectMapper());
    }

    public static ObjectMapper createYamlMapper() {
        return configure(new ObjectMapper(new YAMLFactory()));
    }

    private static ObjectMapper configure(ObjectMapper mapper) {
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        // Невідомі JSON-поля відхиляємо як 400 Bad Request, щоб клієнт не передавав неіснуючі атрибути
        mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        return mapper;
    }
}