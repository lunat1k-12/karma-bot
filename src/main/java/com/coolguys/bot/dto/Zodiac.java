package com.coolguys.bot.dto;

import java.util.Arrays;

public enum Zodiac {
    VIRGO("virgem","Діва"), LIBRA("libra", "Ваги"),
    SCORPIO("escorpiao", "Скорпіон"), SAGITTARIUS("sargitario", "Стрілець"),
    CAPRICORN("capricornio", "Козеріг"), AQUARIUS("aquario", "Водолій"),
    PISCES("peixes", "Риби"), ARIES("aries", "Овен"),
    TAURUS("touro", "Телець"), GEMINI("gemeos", "Близнюки"),
    CANCER("cancer", "Рак"), LEO("leao", "Лев");

    private final String name;
    private final String buttonName;

    Zodiac(String name, String buttonName) {
        this.name = name;
        this.buttonName = buttonName;
    }

    public String getName() {
        return name;
    }

    public String getButtonName() {
        return buttonName;
    }

    public static Zodiac getByName(String name) {
        return Arrays.stream(values())
                .filter(z -> z.name.equals(name))
                .findFirst()
                .orElse(null);
    }
}
