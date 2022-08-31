package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.Order;
import com.coolguys.bot.dto.OrderType;
import com.coolguys.bot.dto.ReplyOrderStage;
import com.coolguys.bot.entity.OrderEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderMapper implements EntityToDtoMapper<OrderEntity, Order> {

    private final UserMapper userMapper;
    @Override
    public Order toDto(OrderEntity entity) {
        return Order.builder()
                .targetUser(userMapper.toDto(entity.getTargetUser()))
                .id(entity.getId())
                .chatId(entity.getChatId())
                .currentIteration(entity.getCurrentIteration())
                .iterationCount(entity.getIterationCount())
                .originUserId(entity.getOriginUserId())
                .respondMessage(entity.getRespondMessage())
                .stage(ReplyOrderStage.getById(entity.getStage()))
                .type(OrderType.getById(entity.getType()))
                .build();
    }

    @Override
    public OrderEntity toEntity(Order dto) {
        return OrderEntity.builder()
                .targetUser(userMapper.toEntity(dto.getTargetUser()))
                .respondMessage(dto.getRespondMessage())
                .type(dto.getType().getId())
                .stage(dto.getStage().getId())
                .originUserId(dto.getOriginUserId())
                .iterationCount(dto.getIterationCount())
                .currentIteration(dto.getCurrentIteration())
                .chatId(dto.getChatId())
                .id(dto.getId())
                .build();
    }
}
