package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode
public class TelegramGuardDepartment {
    private Long id;
    private TelegramUser owner;
    private Integer currentPrice;
    private Long chatId;

    public void plusPrice(Integer plus) {
        this.currentPrice += plus;
    }
}
