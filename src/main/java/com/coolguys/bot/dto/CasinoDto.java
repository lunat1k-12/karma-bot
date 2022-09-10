package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CasinoDto {
    private Long id;
    private UserInfo owner;
    private Integer currentPrice;
    private Long chatId;

    public void plusPrice(Integer plus) {
        this.currentPrice += plus;
    }
}