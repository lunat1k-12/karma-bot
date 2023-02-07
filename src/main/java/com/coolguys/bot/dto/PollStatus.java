package com.coolguys.bot.dto;

import java.util.Arrays;

public enum PollStatus {
    IN_PROGRESS("in_progress"), DONE("done");

    private final String id;

    PollStatus(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public static PollStatus getById(String id) {
        return Arrays.stream(values())
                .filter(stage -> stage.id.equals(id))
                .findFirst()
                .orElseThrow();
    }
}
