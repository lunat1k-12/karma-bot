package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@Builder
@EqualsAndHashCode
public class TelegramKarmaUpdate {
    private Long id;
    private Long originUserId;
    private KarmaUpdateType type;
    private Long chatId;
    private LocalDateTime date;
    private TelegramUser targetUser;
}
