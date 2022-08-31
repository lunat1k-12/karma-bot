package com.coolguys.bot.dto;

import java.util.Arrays;

public enum ReplyOrderStage {
    TARGET_REQUIRED("target_required"), MESSAGE_REQUIRED("message_required"),
    IN_PROGRESS("in_progress"), DONE("done");

    private final String id;

    ReplyOrderStage(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
    public static ReplyOrderStage getById(String id) {
        return Arrays.stream(values())
                .filter(stage -> stage.id.equals(id))
                .findFirst()
                .orElseThrow();
    }
}
