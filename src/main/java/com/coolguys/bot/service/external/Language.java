package com.coolguys.bot.service.external;

import java.util.Arrays;

public enum Language {
    UKRAINIAN("uk"), RUSNYA("ru"), ENGLISH("en"), POLISH("pl"), FRANCE("fr"), JAPANESE("ja"),
    SERBIAN("sr"), NA("n/a");

    private final String code;

    Language(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static Language getByCode(String code) {
        if (code == null) {
            return Language.NA;
        }
        return Arrays.stream(values())
                .filter(stage -> stage.code.equals(code))
                .findFirst()
                .orElse(NA);
    }
}
