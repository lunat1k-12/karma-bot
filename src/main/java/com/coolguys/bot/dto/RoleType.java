package com.coolguys.bot.dto;

import lombok.Data;

import java.util.Arrays;
import java.util.List;

public enum RoleType {

    THIEF("thief", "Крадій", "\uD83E\uDD77", List.of(RoleAction.of("thief.steal", "Вкрасти"))),
    DRUG_DEALER("drug_dealer", "Наркоділер", "⚗️", List.of(
            RoleAction.of("d.dod", "Торгувати наркотою"),
            RoleAction.of("d.dropd", "Підкинути наркотики"))),
    PRISON_WATCH("prison_watch", "Вертухай", "\uD83D\uDC6E\u200D♀️",
            List.of(RoleAction.of("prison_watch.take_money", "Вимагати грощі у в`язнів")));
    private final String id;
    private final String label;
    private final String emoji;
    private final List<RoleAction> actions;

    RoleType(String id, String label, String emoji, List<RoleAction> actions) {
        this.id = id;
        this.label = label;
        this.emoji = emoji;
        this.actions = actions;
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

    public List<RoleAction> getRoleActions() {
        return actions;
    }

    public static RoleType getById(String id) {
        return Arrays.stream(values())
                .filter(stage -> stage.id.equals(id))
                .findFirst()
                .orElseThrow();
    }

    @Data(staticConstructor = "of")
    public static class RoleAction {
        private final String id;
        private final String label;
    }
}
