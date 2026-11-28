package com.ko.footballupdater.llm;

import com.ko.footballupdater.utils.StatFieldHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic checks that LLM text only uses numbers and names that exist in our data.
 * The checks fail closed: anything that can't be verified is reported.
 */
public final class GroundingValidator {

    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#[\\p{L}\\p{N}_]+");
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{[a-z_]+}");

    // Number words, including football terms that imply a count
    private static final Map<String, String> NUMBER_WORDS = Map.ofEntries(
            Map.entry("one", "1"), Map.entry("two", "2"), Map.entry("three", "3"), Map.entry("four", "4"),
            Map.entry("five", "5"), Map.entry("six", "6"), Map.entry("seven", "7"), Map.entry("eight", "8"),
            Map.entry("nine", "9"), Map.entry("ten", "10"), Map.entry("eleven", "11"), Map.entry("twelve", "12"),
            Map.entry("thirteen", "13"), Map.entry("fourteen", "14"), Map.entry("fifteen", "15"),
            Map.entry("sixteen", "16"), Map.entry("seventeen", "17"), Map.entry("eighteen", "18"),
            Map.entry("nineteen", "19"), Map.entry("twenty", "20"), Map.entry("dozen", "12"),
            Map.entry("brace", "2"), Map.entry("hat-trick", "3"), Map.entry("hattrick", "3"), Map.entry("hat trick", "3"),
            Map.entry("double", "2"), Map.entry("treble", "3"), Map.entry("hundred", "100")
    );
    private static final Pattern NUMBER_WORD_PATTERN = Pattern.compile(
            "\\b(" + String.join("|", NUMBER_WORDS.keySet().stream()
                    .sorted((a, b) -> b.length() - a.length())
                    .map(Pattern::quote)
                    .toList()) + ")\\b",
            Pattern.CASE_INSENSITIVE);

    // Capitalised words allowed mid-sentence that aren't names
    private static final Set<String> CAPITALISED_ALLOWLIST = Set.of(
            "i", "motm", "potm", "vs", "ft", "ht", "xg", "xa", "goat",
            "monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday",
            "january", "february", "march", "april", "may", "june", "july", "august",
            "september", "october", "november", "december"
    );

    private static final Pattern WORD_PATTERN = Pattern.compile("[\\p{L}][\\p{L}'’\\-]*");

    // Punctuation, line breaks and emoji start a new sentence
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("[.!?:\\n\\p{So}\\p{Cs}]");

    private GroundingValidator() {
    }

    /**
     * @return numbers (digits or number words) in the text that aren't in the allowed set. Hashtags are ignored.
     */
    public static List<String> findUngroundedNumbers(String text, Set<String> allowedNumbers) {
        List<String> ungrounded = new ArrayList<>();
        if (text == null) {
            return ungrounded;
        }
        String stripped = HASHTAG_PATTERN.matcher(text).replaceAll(" ");
        Matcher numberMatcher = StatFieldHelper.NUMBER_PATTERN.matcher(stripped);
        while (numberMatcher.find()) {
            String normalized = StatFieldHelper.normalizeNumber(numberMatcher.group());
            if (!allowedNumbers.contains(normalized)) {
                ungrounded.add(numberMatcher.group());
            }
        }
        Matcher wordMatcher = NUMBER_WORD_PATTERN.matcher(stripped);
        while (wordMatcher.find()) {
            String value = NUMBER_WORDS.get(wordMatcher.group(1).toLowerCase(Locale.ROOT));
            if (value != null && !allowedNumbers.contains(value)) {
                ungrounded.add(wordMatcher.group(1));
            }
        }
        return ungrounded;
    }

    /**
     * Find capitalised words in the middle of a sentence that aren't part of an allowed name,
     * and any known (forbidden) names that appear anywhere in the text.
     *
     * @param allowedNames   names the text may use (the post's player and teams)
     * @param forbiddenNames other names from the database that must not appear
     */
    public static List<String> findUnknownNames(String text, Collection<String> allowedNames, Collection<String> forbiddenNames) {
        List<String> unknown = new ArrayList<>();
        if (text == null) {
            return unknown;
        }
        String stripped = PLACEHOLDER_PATTERN.matcher(HASHTAG_PATTERN.matcher(text).replaceAll(" ")).replaceAll(" ");

        Set<String> allowedWords = new HashSet<>();
        for (String name : allowedNames) {
            if (name == null) {
                continue;
            }
            for (String part : name.split("[\\s\\-]+")) {
                if (!part.isBlank()) {
                    allowedWords.add(part.toLowerCase(Locale.ROOT));
                }
            }
        }

        // Mid-sentence capitalised words must belong to an allowed name
        boolean sentenceStart = true;
        int lastEnd = 0;
        Matcher matcher = WORD_PATTERN.matcher(stripped);
        while (matcher.find()) {
            String between = stripped.substring(lastEnd, matcher.start());
            if (SENTENCE_BOUNDARY.matcher(between).find()) {
                sentenceStart = true;
            }
            String word = matcher.group().replaceAll("(['’]s|['’])$", "");
            if (!sentenceStart && !word.isEmpty() && Character.isUpperCase(word.charAt(0))) {
                String lower = word.toLowerCase(Locale.ROOT);
                if (!allowedWords.contains(lower) && !CAPITALISED_ALLOWLIST.contains(lower)) {
                    unknown.add(word);
                }
            }
            sentenceStart = false;
            lastEnd = matcher.end();
        }

        // Known names of other players/teams aren't allowed anywhere
        String lowerText = stripped.toLowerCase(Locale.ROOT);
        Set<String> allowedLower = new HashSet<>();
        allowedNames.stream().filter(n -> n != null).forEach(n -> allowedLower.add(n.toLowerCase(Locale.ROOT)));
        for (String forbidden : forbiddenNames) {
            if (forbidden == null || forbidden.length() < 3) {
                continue;
            }
            String lower = forbidden.toLowerCase(Locale.ROOT);
            if (allowedLower.contains(lower)) {
                continue;
            }
            if (Pattern.compile("\\b" + Pattern.quote(lower) + "\\b").matcher(lowerText).find()
                    && allowedLower.stream().noneMatch(allowed -> allowed.contains(lower))) {
                unknown.add(forbidden);
            }
        }
        return unknown;
    }
}
