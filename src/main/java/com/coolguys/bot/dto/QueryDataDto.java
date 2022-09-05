package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueryDataDto {

    public static final String REPLY_ORDER_TYPE = "reply.order";
    public static final String STEAL_TYPE = "steal";

    private String type;
    private String option;
}
