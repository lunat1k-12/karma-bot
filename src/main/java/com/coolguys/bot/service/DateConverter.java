package com.coolguys.bot.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class DateConverter {

    public static String localDateTimeToStringLabel(LocalDateTime ldt) {
        ZonedDateTime zdt = ldt.toInstant(ZoneOffset.UTC).atZone(ZoneId.of("UTC+03:00"));
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .format(zdt);
    }
}
