package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@Builder
@EqualsAndHashCode
public class UserInfo {
    private Long id;
    private String username;
    private Integer socialCredit;
    private Long chatId;
    private Long telegramId;

    public void plusCredit(Integer plus) {
        this.socialCredit += plus;
    }

    public void minusCredit(Integer minus) {
        this.socialCredit -= minus;
    }
}
