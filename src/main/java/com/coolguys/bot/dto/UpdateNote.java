package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateNote {
    private Long id;
    private UpdateNoteStatus status;
    private String message;
}
