package com.coolguys.bot.dto;

import java.util.Arrays;

public enum UpdateNoteStatus {
    WAITING("waiting"),
    SENT("sent");

    private final String id;

    UpdateNoteStatus(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static UpdateNoteStatus getById(String id) {
        return Arrays.stream(values())
                .filter(stage -> stage.id.equals(id))
                .findFirst()
                .orElseThrow();
    }
}
