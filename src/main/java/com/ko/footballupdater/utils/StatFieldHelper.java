package com.ko.footballupdater.utils;

import com.ko.footballupdater.models.Match;
import com.ko.footballupdater.models.PlayerMatchPerformanceStats;
import com.ko.footballupdater.models.Post;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helpers to read a post's stat fields and the numbers they contain, used to ground LLM output in real data.
 */
@Slf4j
public final class StatFieldHelper {

    private static final Set<String> NON_STAT_FIELDS = Set.of("id", "match", "dataSourceSiteName");

    public static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+(?:[.,]\\d+)?");

    private StatFieldHelper() {
    }

    /**
     * @return stat field name to value, for all non-null stats, in declaration order
     */
    public static Map<String, Object> nonNullStats(PlayerMatchPerformanceStats stats) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (stats == null) {
            return result;
        }
        for (Field field : PlayerMatchPerformanceStats.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || NON_STAT_FIELDS.contains(field.getName())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(stats);
                if (value != null) {
                    result.put(field.getName(), value);
                }
            } catch (IllegalAccessException ex) {
                log.atWarn().setMessage("Unable to read stat field " + field.getName()).setCause(ex).log();
            }
        }
        return result;
    }

    /**
     * Every number that appears in a post's stats or match data, normalised with {@link #normalizeNumber}.
     * Float stats also allow their 1 and 2 decimal place roundings.
     */
    public static Set<String> allowedNumbers(Post post) {
        Set<String> allowed = new HashSet<>();
        if (post == null || post.getPlayerMatchPerformanceStats() == null) {
            return allowed;
        }
        for (Object value : nonNullStats(post.getPlayerMatchPerformanceStats()).values()) {
            if (value instanceof Float || value instanceof Double) {
                BigDecimal decimal = new BigDecimal(value.toString());
                allowed.add(normalizeNumber(decimal.toPlainString()));
                allowed.add(normalizeNumber(decimal.setScale(1, RoundingMode.HALF_UP).toPlainString()));
                allowed.add(normalizeNumber(decimal.setScale(2, RoundingMode.HALF_UP).toPlainString()));
            } else {
                addNumbersInText(value.toString(), allowed);
            }
        }
        Match match = post.getPlayerMatchPerformanceStats().getMatch();
        if (match != null) {
            if (match.getHomeTeamScore() != null) {
                allowed.add(match.getHomeTeamScore().toString());
            }
            if (match.getAwayTeamScore() != null) {
                allowed.add(match.getAwayTeamScore().toString());
            }
            if (match.getDate() != null) {
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(match.getDate());
                allowed.add(String.valueOf(calendar.get(Calendar.DAY_OF_MONTH)));
                allowed.add(String.valueOf(calendar.get(Calendar.MONTH) + 1));
                allowed.add(String.valueOf(calendar.get(Calendar.YEAR)));
                addNumbersInText(DateTimeHelper.getDateAsFormattedString(match.getDate()), allowed);
            }
        }
        return allowed;
    }

    public static Set<String> allowedNumbers(Collection<Post> posts) {
        Set<String> allowed = new HashSet<>();
        posts.forEach(post -> allowed.addAll(allowedNumbers(post)));
        return allowed;
    }

    public static void addNumbersInText(String text, Set<String> into) {
        if (text == null) {
            return;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        while (matcher.find()) {
            into.add(normalizeNumber(matcher.group()));
        }
    }

    /**
     * Normalise a number string so "07", "7.0" and "7" compare equal, and "0,5" equals "0.5".
     */
    public static String normalizeNumber(String number) {
        try {
            return new BigDecimal(number.replace(',', '.')).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException ex) {
            return number;
        }
    }
}
