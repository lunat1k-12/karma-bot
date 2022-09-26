package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueryDataDto {

    public static final String REPLY_ORDER_TYPE = "reply.order";
    public static final String STEAL_TYPE = "steal";
    public static final String ROLE_SELECT_TYPE = "role.select";

    public static final String DROP_DRUGS_TYPE = "drop.drugs";

    private String type;
    private String option;
    private Long originalAccId;
}
