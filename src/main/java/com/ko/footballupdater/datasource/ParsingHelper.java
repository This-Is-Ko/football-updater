package com.ko.footballupdater.datasource;

public class ParsingHelper {

    public static Integer parseIntegerOrNull(String input) {
        if (input != null && !input.isEmpty()) {
            return Integer.parseInt(input);
        }
        return null;
    }
    public static Float parseFloatOrNull(String input) {
        if (input != null && !input.isEmpty()) {
            return Float.parseFloat(input);
        }
        return null;
    }

}
