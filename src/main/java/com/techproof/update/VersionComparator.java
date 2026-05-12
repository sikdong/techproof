package com.techproof.update;

import java.util.ArrayList;
import java.util.List;

public final class VersionComparator {
    private VersionComparator() {
    }

    public static int compare(String left, String right) {
        List<Integer> leftParts = numericParts(left);
        List<Integer> rightParts = numericParts(right);
        int length = Math.max(leftParts.size(), rightParts.size());
        for (int i = 0; i < length; i++) {
            int leftValue = i < leftParts.size() ? leftParts.get(i) : 0;
            int rightValue = i < rightParts.size() ? rightParts.get(i) : 0;
            if (leftValue != rightValue) {
                return Integer.compare(leftValue, rightValue);
            }
        }
        return 0;
    }

    private static List<Integer> numericParts(String version) {
        String normalized = version == null ? "" : version.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        int suffixStart = firstSuffixIndex(normalized);
        if (suffixStart >= 0) {
            normalized = normalized.substring(0, suffixStart);
        }

        List<Integer> parts = new ArrayList<>();
        for (String part : normalized.split("\\.")) {
            if (part.isBlank()) {
                parts.add(0);
                continue;
            }
            parts.add(parseLeadingNumber(part));
        }
        return parts;
    }

    private static int firstSuffixIndex(String version) {
        int dash = version.indexOf('-');
        int plus = version.indexOf('+');
        if (dash < 0) {
            return plus;
        }
        if (plus < 0) {
            return dash;
        }
        return Math.min(dash, plus);
    }

    private static int parseLeadingNumber(String text) {
        int end = 0;
        while (end < text.length() && Character.isDigit(text.charAt(end))) {
            end++;
        }
        if (end == 0) {
            return 0;
        }
        return Integer.parseInt(text.substring(0, end));
    }
}
