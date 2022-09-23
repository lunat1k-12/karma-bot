package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.OrderType;
import com.coolguys.bot.dto.ReplyOrderStage;
import com.coolguys.bot.dto.TelegramOrder;
import com.coolguys.bot.entity.TelegramOrderEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramOrderMapper implements EntityToDtoMapper<TelegramOrderEntity, TelegramOrder> {

    private final ChatAccountMapper chatAccountMapper;
    private final TelegramDrugActionMapper telegramDrugActionMapper;

    @Override
    public TelegramOrder toDto(TelegramOrderEntity entity) {
        return TelegramOrder.builder()
                .targetAcc(chatAccountMapper.toDto(entity.getTargetAcc()))
                .id(entity.getId())
                .chatId(entity.getChatId())
                .currentIteration(entity.getCurrentIteration())
                .iterationCount(entity.getIterationCount())
                .originAccId(entity.getOriginAccId())
                .respondMessage(entity.getRespondMessage())
                .stage(ReplyOrderStage.getById(entity.getStage()))
                .type(OrderType.getById(entity.getType()))
                .drugAction(telegramDrugActionMapper.toDto(entity.getDrugAction()))
                .build();
    }

    @Override
    public TelegramOrderEntity toEntity(TelegramOrder dto) {
        return TelegramOrderEntity.builder()
                .targetAcc(chatAccountMapper.toEntity(dto.getTargetAcc()))
                .respondMessage(dto.getRespondMessage())
                .type(dto.getType().getId())
                .stage(dto.getStage().getId())
                .originAccId(dto.getOriginAccId())
                .iterationCount(dto.getIterationCount())
                .currentIteration(dto.getCurrentIteration())
                .chatId(dto.getChatId())
                .id(dto.getId())
                .drugAction(telegramDrugActionMapper.toEntity(dto.getDrugAction()))
                .build();
    }
}
