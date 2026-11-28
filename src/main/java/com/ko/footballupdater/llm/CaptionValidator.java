package com.ko.footballupdater.llm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic checks for an LLM caption body before it's used.
 */
public final class CaptionValidator {

    public static final int INSTAGRAM_CAPTION_MAX_LENGTH = 2200;
    public static final int MAX_BODY_LENGTH = 1000;

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]*)}");

    private CaptionValidator() {
    }

    /**
     * @param body           caption body as returned by the model, with {placeholders}
     * @param placeholders   available placeholders and the database names they render to
     * @param allowedNumbers numbers in the post's stats and match data
     * @param knownNames     all player and team names in the database
     * @param hashtagSuffix  hashtags appended after the body
     * @return validation errors, empty when the caption is valid
     */
    public static List<String> validate(String body, Map<String, String> placeholders, Set<String> allowedNumbers,
                                        Collection<String> knownNames, String hashtagSuffix) {
        List<String> errors = new ArrayList<>();
        if (body == null || body.isBlank()) {
            errors.add("Caption is empty");
            return errors;
        }
        if (body.length() > MAX_BODY_LENGTH) {
            errors.add("Caption is longer than " + MAX_BODY_LENGTH + " characters");
        }
        if (body.contains("#")) {
            errors.add("Caption must not contain hashtags, they are added automatically");
        }
        if (body.contains("@")) {
            errors.add("Caption must not contain @mentions");
        }
        if (body.toLowerCase().contains("http") || body.toLowerCase().contains("www.")) {
            errors.add("Caption must not contain links");
        }

        Matcher placeholderMatcher = PLACEHOLDER_PATTERN.matcher(body);
        while (placeholderMatcher.find()) {
            if (!placeholders.containsKey(placeholderMatcher.group(1))) {
                errors.add("Unknown placeholder {" + placeholderMatcher.group(1) + "}");
            }
        }

        List<String> ungroundedNumbers = GroundingValidator.findUngroundedNumbers(body, allowedNumbers);
        if (!ungroundedNumbers.isEmpty()) {
            errors.add("Numbers not found in the stats or match data: " + String.join(", ", ungroundedNumbers));
        }

        List<String> unknownNames = GroundingValidator.findUnknownNames(body, placeholders.values(), knownNames);
        if (!unknownNames.isEmpty()) {
            errors.add("Names that don't match the player or teams for this post (use the placeholders): " + String.join(", ", unknownNames));
        }

        String rendered = render(body, placeholders);
        int totalLength = rendered.length() + (hashtagSuffix != null ? hashtagSuffix.length() : 0);
        if (totalLength > INSTAGRAM_CAPTION_MAX_LENGTH) {
            errors.add("Caption with hashtags is longer than Instagram's " + INSTAGRAM_CAPTION_MAX_LENGTH + " character limit");
        }
        return errors;
    }

    public static String render(String body, Map<String, String> placeholders) {
        String rendered = body;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            rendered = rendered.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return rendered.trim();
    }
}
