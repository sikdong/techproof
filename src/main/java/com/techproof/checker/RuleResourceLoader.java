package com.techproof.checker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

public final class RuleResourceLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RuleResourceLoader() {
    }

    public static <T> List<T> loadList(String resourcePath, TypeReference<List<T>> type, List<T> fallback) {
        try (InputStream in = RuleResourceLoader.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                return fallback;
            }
            return MAPPER.readValue(in, type);
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
