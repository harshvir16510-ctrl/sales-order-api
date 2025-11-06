package com.example.salesorder.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateFormatter {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static String formatInstant(Instant instant) {
        if (instant == null) {
            return null;
        }
        LocalDate date = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        return date.format(DATE_FORMATTER);
    }
}

