package com.coolguys.bot.dto;

import lombok.Data;

import java.util.Arrays;
import java.util.List;

import static com.coolguys.bot.service.role.RoleActionType.DOCTOR_MAKE_SICK;
import static com.coolguys.bot.service.role.RoleActionType.DO_DRUGS_ACTION;
import static com.coolguys.bot.service.role.RoleActionType.DROP_DRUGS_ACTION;
import static com.coolguys.bot.service.role.RoleActionType.PRISON_WATCH_TAKE_MONEY_ACTION;
import static com.coolguys.bot.service.role.RoleActionType.THIEF_INVESTIGATE_ACTION;
import static com.coolguys.bot.service.role.RoleActionType.THIEF_STEAL_ACTION;

public enum RoleType {

    THIEF("thief", "Крадій", "\uD83E\uDD77", List.of(
            RoleAction.of(THIEF_STEAL_ACTION, "Вкрасти"),
            RoleAction.of(THIEF_INVESTIGATE_ACTION, "Розвідати"))),
    DRUG_DEALER("drug_dealer", "Наркоділер", "⚗️", List.of(
            RoleAction.of(DO_DRUGS_ACTION, "Торгувати наркотою"),
            RoleAction.of(DROP_DRUGS_ACTION, "Підкинути наркотики"))),
    PRISON_WATCH("prison_watch", "Вертухай", "\uD83D\uDC6E\u200D♀️",
            List.of(RoleAction.of(PRISON_WATCH_TAKE_MONEY_ACTION, "Вимагати грощі у в`язнів"))),
    DOCTOR("doc", "Лікар", "\uD83D\uDC68\u200D⚕️",
            List.of(RoleAction.of(DOCTOR_MAKE_SICK, "Заразити хворобою")));
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
