package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DrugAction {
    private Long id;
    private UserInfo user;
    private LocalDateTime expires;
    private Long chatId;
}
