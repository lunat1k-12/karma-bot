package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Order {

    private Long id;
    private Long originUserId;
    private OrderType type;
    private Long chatId;
    private ReplyOrderStage stage;
    private Long iterationCount;
    private Long currentIteration;
    private String respondMessage;
    private UserInfo targetUser;
}
