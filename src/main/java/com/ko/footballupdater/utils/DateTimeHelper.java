package com.ko.footballupdater.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeHelper {

    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");

    private static final SimpleDateFormat formatterFileName = new SimpleDateFormat("yyyy_MM_dd");

    public static String getDateAsFormattedString(Date date) {
        if (date != null) {
            return formatter.format(date);
        }
        return "";
    }

    public static String getDateAsFormattedStringForFileName(Date date) {
        if (date != null) {
            return formatterFileName.format(date);
        }
        return "";
    }
}
