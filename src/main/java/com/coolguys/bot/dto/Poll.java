package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode
public class Poll {
    private Long id;
    private TelegramMessage message;
    private TelegramChat chat;
    private PollStatus status;
    private String telegramPollId;
}
