package com.coolguys.bot.dto;

import java.util.Arrays;

public enum RoleType {

    THIEF("thief", "Крадій", "\uD83E\uDD77"), DRUG_DEALER("drug_dealer", "Наркоділер", "⚗️"),
    PRISON_WATCH("prison_watch", "Вертухай", "\uD83D\uDC6E\u200D♀️");
    private final String id;
    private final String label;
    private final String emoji;

    RoleType(String id, String label, String emoji) {
        this.id = id;
        this.label = label;
        this.emoji = emoji;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getEmoji() {
        return emoji;
    }

    public static RoleType getById(String id) {
        return Arrays.stream(values())
                .filter(stage -> stage.id.equals(id))
                .findFirst()
                .orElseThrow();
    }
}
