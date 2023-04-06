package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TranslateRequest {
    private String q;
    private String source;
    private String target;
    private String format;
}
