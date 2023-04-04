package com.coolguys.bot.dto;

import com.coolguys.bot.service.external.Language;
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
    private Language language;
}
