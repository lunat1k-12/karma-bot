package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.TelegramGuardDepartment;
import com.coolguys.bot.entity.TelegramGuardDepartmentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramGuardDepartmentMapper implements EntityToDtoMapper<TelegramGuardDepartmentEntity, TelegramGuardDepartment> {

    private final TelegramUserMapper telegramUserMapper;

    @Override
    public TelegramGuardDepartment toDto(TelegramGuardDepartmentEntity entity) {
        return TelegramGuardDepartment.builder()
                .id(entity.getId())
                .owner(telegramUserMapper.toDto(entity.getOwner()))
                .currentPrice(entity.getCurrentPrice())
                .chatId(entity.getChatId())
                .build();
    }

    @Override
    public TelegramGuardDepartmentEntity toEntity(TelegramGuardDepartment dto) {
        return TelegramGuardDepartmentEntity.builder()
                .chatId(dto.getChatId())
                .currentPrice(dto.getCurrentPrice())
                .id(dto.getId())
                .owner(telegramUserMapper.toEntity(dto.getOwner()))
                .build();
    }
}
