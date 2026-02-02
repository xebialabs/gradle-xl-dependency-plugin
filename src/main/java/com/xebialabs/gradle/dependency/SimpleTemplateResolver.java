package com.xebialabs.gradle.dependency;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Simple string template resolver that replaces ${key} placeholders with values from a map.
 * Replacement for Groovy's SimpleTemplateEngine.
 */
class SimpleTemplateResolver {
  
  private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");
  
  /**
   * Resolves template placeholders in the given string using values from the context map.
   * 
   * @param template the template string with ${key} placeholders
   * @param context the map containing key-value pairs for substitution
   * @return the resolved string with placeholders replaced
   */
  public static String resolve(String template, Map<String, String> context) {
    if (template == null || template.isEmpty()) {
      return template;
    }
    
    StringBuffer result = new StringBuffer();
    Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
    
    while (matcher.find()) {
      String key = matcher.group(1);
      String value = context.get(key);
      if (value == null) {
        // If value not found, keep the placeholder
        value = "${" + key + "}";
      }
      matcher.appendReplacement(result, Matcher.quoteReplacement(value));
    }
    matcher.appendTail(result);
    
    return result.toString();
  }
}
