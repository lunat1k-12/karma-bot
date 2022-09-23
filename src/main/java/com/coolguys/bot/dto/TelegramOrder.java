package com.coolguys.bot.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode
public class TelegramOrder {
    private Long id;
    private Long originAccId;
    private OrderType type;
    private Long chatId;
    private ReplyOrderStage stage;
    private Long iterationCount;
    private Long currentIteration;
    private String respondMessage;
    private ChatAccount targetAcc;
    private TelegramDrugAction drugAction;
}
