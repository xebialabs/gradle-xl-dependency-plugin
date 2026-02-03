package com.xebialabs.gradle.dependency;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple string template resolver that replaces variable placeholders with values from a map.
 * Replacement for Groovy's SimpleTemplateEngine.
 * <p>
 * Supports two GString-style placeholder formats:
 * - ${variableName} (braced format)
 * - $variableName (non-braced format)
 */
class SimpleTemplateResolver {

    /**
     * Pattern that matches both ${variableName} and $variableName formats.
     * Group 1: captures content inside ${...}
     * Group 2: captures identifier after $ (without braces)
     */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
            "\\$\\{([^}]+)\\}|" +           // ${variableName} format
                    "\\$([a-zA-Z_][a-zA-Z0-9_]*)"   // $variableName format
    );

    /**
     * Resolves template placeholders in the given string using values from the context map.
     *
     * @param template the template string with $variable or ${variable} placeholders
     * @param context  the map containing key-value pairs for substitution
     * @return the resolved string with placeholders replaced
     */
    public static String resolve(String template, Map<String, String> context) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);

        while (matcher.find()) {
            // Group 1: ${key} format, Group 2: $key format
            String key = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String value = context.get(key);

            if (value == null) {
                // If value not found, keep the original placeholder
                value = matcher.group(0); // Keep the full match (either ${key} or $key)
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);

        return result.toString();
    }
}
