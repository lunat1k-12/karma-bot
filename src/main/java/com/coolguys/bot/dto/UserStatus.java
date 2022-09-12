package com.coolguys.bot.dto;

import java.util.Arrays;

public enum UserStatus {
    ACTIVE("active"),
    INACTIVE("inactive");

    private final String id;

    UserStatus(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static UserStatus getById(String id) {
        return Arrays.stream(values())
                .filter(stage -> stage.id.equals(id))
                .findFirst()
                .orElseThrow();
    }
}
