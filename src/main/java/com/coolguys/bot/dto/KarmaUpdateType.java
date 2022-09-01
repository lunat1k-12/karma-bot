package com.coolguys.bot.dto;

import java.util.Arrays;

public enum KarmaUpdateType {
    INCREASE("increase", "ну ти йому ще яйця вилижи\nНезарахованно"),
    DECREASE("decrease", "Тупо Хейтер\nНезарахованно");

    private final String id;
    private final String messageForDuplicate;

    KarmaUpdateType(String id, String messageForDuplicate) {
        this.id = id;
        this.messageForDuplicate = messageForDuplicate;
    }

    public String getId() {
        return id;
    }

    public static KarmaUpdateType getById(String id) {
        return Arrays.stream(values())
                .filter(stage -> stage.id.equals(id))
                .findFirst()
                .orElseThrow();
    }

    public String getMessageForDuplicate() {
        return messageForDuplicate;
    }
}
