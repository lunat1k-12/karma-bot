package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@Builder
@EqualsAndHashCode
public class TelegramMessage {
    private Long id;
    private TelegramUser user;
    private String message;
    private LocalDateTime date;
    private Long chatId;
}
