package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatDto {

    private Long id;
    private Long telegramId;
    private String name;
}
