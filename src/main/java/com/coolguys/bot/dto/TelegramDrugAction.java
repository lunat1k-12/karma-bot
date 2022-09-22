package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@Builder
@EqualsAndHashCode
public class TelegramDrugAction {
    private Long id;
    private TelegramUser user;
    private LocalDateTime expires;
    private Long chatId;
}
