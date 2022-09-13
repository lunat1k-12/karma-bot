package com.coolguys.bot.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class DateConverter {

    public static String localDateTimeToStringLabel(LocalDateTime ldt) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .format(ldt.atZone(ZoneId.of("UTC+03:00")));
    }
}
