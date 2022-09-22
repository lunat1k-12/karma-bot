package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode
public class TelegramUser {
    private Long id;
    private String username;
    private String firstName;
    private String lastName;
}
