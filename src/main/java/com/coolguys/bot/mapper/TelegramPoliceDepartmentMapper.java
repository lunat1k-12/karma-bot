package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.TelegramPoliceDepartment;
import com.coolguys.bot.entity.TelegramPoliceDepartmentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramPoliceDepartmentMapper implements EntityToDtoMapper<TelegramPoliceDepartmentEntity, TelegramPoliceDepartment> {

    private final TelegramUserMapper telegramUserMapper;

    @Override
    public TelegramPoliceDepartment toDto(TelegramPoliceDepartmentEntity entity) {
        return TelegramPoliceDepartment.builder()
                .id(entity.getId())
                .currentPrice(entity.getCurrentPrice())
                .owner(telegramUserMapper.toDto(entity.getOwner()))
                .chatId(entity.getChatId())
                .build();
    }

    @Override
    public TelegramPoliceDepartmentEntity toEntity(TelegramPoliceDepartment dto) {
        return TelegramPoliceDepartmentEntity.builder()
                .chatId(dto.getChatId())
                .currentPrice(dto.getCurrentPrice())
                .id(dto.getId())
                .owner(telegramUserMapper.toEntity(dto.getOwner()))
                .build();
    }
}
