package com.ko.footballupdater.utils;

import jakarta.persistence.Transient;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeHelper {

    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");

    private static final SimpleDateFormat formatterFileName = new SimpleDateFormat("yyyy_MM_dd");

    public static String getDateAsFormattedString(Date date) {
        return formatter.format(date);
    }

    public static String getDateAsFormattedStringForFileName(Date date) {
        return formatterFileName.format(date);
    }
}
