package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Deprecated
public class KarmaUpdate {

    private Long id;
    private Long originUserId;
    private KarmaUpdateType type;
    private Long chatId;
    private LocalDateTime date;
    private UserInfo targetUser;
}
