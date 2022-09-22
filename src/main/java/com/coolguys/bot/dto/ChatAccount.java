package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode
public class ChatAccount {
    private Long id;
    private TelegramUser user;
    private TelegramChat chat;
    private Integer socialCredit;
    private UserStatus status;

    public void plusCredit(Integer plus) {
        this.socialCredit += plus;
    }

    public void minusCredit(Integer minus) {
        this.socialCredit -= minus;
    }

    public boolean isActive() {
        return UserStatus.ACTIVE.equals(status);
    }
}
