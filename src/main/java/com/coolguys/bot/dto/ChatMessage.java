package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Deprecated
public class ChatMessage {

    private Long id;
    private UserInfo user;
    private String message;
    private LocalDateTime date;
    private Long chatId;
}
