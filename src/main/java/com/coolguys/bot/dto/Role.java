package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@Builder
@EqualsAndHashCode
public class Role {
    private Long id;
    private ChatAccount account;
    private RoleType role;
    private LocalDateTime expires;
}
