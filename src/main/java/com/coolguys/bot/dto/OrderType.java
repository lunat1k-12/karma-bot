package com.coolguys.bot.dto;

import java.util.Arrays;

public enum OrderType {
    MESSAGE_REPLY("message_reply"),
    STEAL("steal"),
    DROP_DRUGS("drop_drugs");

    private final String id;

    OrderType(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static OrderType getById(String id) {
        return Arrays.stream(values())
                .filter(stage -> stage.id.equals(id))
                .findFirst()
                .orElseThrow();
    }
}
