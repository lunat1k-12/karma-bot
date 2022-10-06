package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode
public class TelegramChat {
    private Long id;
    private String name;
    private Boolean premium;
    private Boolean active;
}
