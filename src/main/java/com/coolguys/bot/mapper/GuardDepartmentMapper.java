package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.GuardDepartmentDto;
import com.coolguys.bot.entity.GuardDepartmentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GuardDepartmentMapper implements EntityToDtoMapper<GuardDepartmentEntity, GuardDepartmentDto> {
    private final UserMapper userMapper;

    @Override
    public GuardDepartmentDto toDto(GuardDepartmentEntity entity) {
        return GuardDepartmentDto.builder()
                .owner(userMapper.toDto(entity.getOwner()))
                .id(entity.getId())
                .currentPrice(entity.getCurrentPrice())
                .chatId(entity.getChatId())
                .build();
    }

    @Override
    public GuardDepartmentEntity toEntity(GuardDepartmentDto dto) {
        return GuardDepartmentEntity.builder()
                .owner(userMapper.toEntity(dto.getOwner()))
                .currentPrice(dto.getCurrentPrice())
                .id(dto.getId())
                .chatId(dto.getChatId())
                .build();
    }
}
