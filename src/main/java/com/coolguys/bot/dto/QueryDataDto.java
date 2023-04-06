package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QueryDataDto {

    public static final String REPLY_ORDER_TYPE = "ro";
    public static final String STEAL_TYPE = "st";
    public static final String ROLE_SELECT_TYPE = "rst";
    public static final String ROLE_ACTION_TYPE = "rat";

    public static final String DROP_DRUGS_TYPE = "ddt";

    public static final String THIEF_INVESTIGATE_TYPE = "th.in";

    public static final String DOCTOR_DISEASE_TYPE = "doc";

    private String type;
    private String option;
    private Long originalAccId;
}
