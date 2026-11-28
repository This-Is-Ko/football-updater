package com.ko.footballupdater.photo;

import java.util.List;

/**
 * Whether a photo shows the right player.
 *
 * @param confidence 0 to 1
 */
public record MatchResult(double confidence, boolean passed, List<String> reasons) {

    public static MatchResult pass(double confidence, String reason) {
        return new MatchResult(confidence, true, List.of(reason));
    }

    public static MatchResult fail(String reason) {
        return new MatchResult(0, false, List.of(reason));
    }
}
