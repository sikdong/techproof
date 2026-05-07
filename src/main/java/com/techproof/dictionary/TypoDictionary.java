package com.techproof.dictionary;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.techproof.model.SourceInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

public class TypoDictionary {
    private final Map<String, String> entries;
    private final Map<String, SourceInfo> sources;

    public TypoDictionary(Map<String, String> entries) {
        this(entries, Map.of());
    }

    public TypoDictionary(Map<String, String> entries, Map<String, SourceInfo> sources) {
        this.entries = new LinkedHashMap<>(entries);
        this.sources = new LinkedHashMap<>(sources);
    }

    public static TypoDictionary loadDefault() {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = TypoDictionary.class.getResourceAsStream("/dictionary/typo-dictionary.json")) {
            if (in == null) return new TypoDictionary(Map.of());
            JsonNode root = mapper.readTree(in);
            return parse(root);
        } catch (IOException e) {
            return new TypoDictionary(Map.of());
        }
    }

    public Map<String, String> entries() {
        return entries;
    }

    public SourceInfo sourceFor(String word) {
        return sources.getOrDefault(word, SourceInfo.NONE);
    }

    private static TypoDictionary parse(JsonNode root) {
        Map<String, String> entries = new LinkedHashMap<>();
        Map<String, SourceInfo> sources = new LinkedHashMap<>();

        if (root == null) {
            return new TypoDictionary(entries, sources);
        }

        if (root.isObject()) {
            root.fields().forEachRemaining(field -> {
                JsonNode value = field.getValue();
                if (value.isTextual()) {
                    entries.put(field.getKey(), value.asText());
                } else if (value.isObject()) {
                    String correct = text(value, "correct");
                    if (!correct.isBlank()) {
                        entries.put(field.getKey(), correct);
                        sources.put(field.getKey(), sourceInfo(value.get("source")));
                    }
                }
            });
            return new TypoDictionary(entries, sources);
        }

        if (root.isArray()) {
            for (JsonNode node : root) {
                String wrong = text(node, "wrong");
                String correct = text(node, "correct");
                if (wrong.isBlank() || correct.isBlank()) {
                    continue;
                }
                entries.put(wrong, correct);
                sources.put(wrong, sourceInfo(node.get("source")));
            }
        }

        return new TypoDictionary(entries, sources);
    }

    private static SourceInfo sourceInfo(JsonNode node) {
        if (node == null || !node.isObject()) {
            return SourceInfo.NONE;
        }
        return new SourceInfo(
            text(node, "provider"),
            firstText(node, "title", "dictionary", "document"),
            firstText(node, "detail", "section", "query"),
            text(node, "url"),
            text(node, "retrievedAt")
        );
    }

    private static String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return "";
        }
        return node.get(field).asText("");
    }
}
