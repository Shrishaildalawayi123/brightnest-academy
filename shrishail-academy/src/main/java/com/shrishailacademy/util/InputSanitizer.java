package com.shrishailacademy.util;

import org.springframework.web.util.HtmlUtils;

import java.util.Locale;

/**
 * Utility for canonicalizing and sanitizing user-provided text before persistence.
 * Uses HTML escaping to prevent stored XSS from being rendered as active HTML/JS.
 */
public final class InputSanitizer {

    private InputSanitizer() {
    }

    private static String sanitizeInternal(String value) {
        return HtmlUtils.htmlEscape(value);
    }

    public static String sanitize(String value) {
        String normalized = normalize(value);
        return normalized == null ? null : sanitizeInternal(normalized);
    }

    public static String sanitizeNullable(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : sanitizeInternal(normalized);
    }

    public static String sanitizeEmail(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        return HtmlUtils.htmlEscape(normalized).toLowerCase(Locale.ROOT);
    }

    public static String sanitizeEmailAndTruncate(String value, int maxLength) {
        return truncate(sanitizeEmail(value), maxLength);
    }

    public static String sanitizeAndTruncate(String value, int maxLength) {
        String sanitized = sanitize(value);
        return truncate(sanitized, maxLength);
    }

    public static String sanitizeAndTruncateNullable(String value, int maxLength) {
        String sanitized = sanitizeNullable(value);
        return truncate(sanitized, maxLength);
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String truncate(String value, int maxLength) {
        if (value == null || maxLength <= 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
