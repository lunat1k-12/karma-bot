package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.PoliceDepartmentDto;
import com.coolguys.bot.entity.PoliceDepartmentEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PoliceDepartmentMapper implements EntityToDtoMapper<PoliceDepartmentEntity, PoliceDepartmentDto> {
    private final UserMapper userMapper;

    @Override
    public PoliceDepartmentDto toDto(PoliceDepartmentEntity entity) {
        return PoliceDepartmentDto.builder()
                .id(entity.getId())
                .owner(userMapper.toDto(entity.getOwner()))
                .currentPrice(entity.getCurrentPrice())
                .chatId(entity.getChatId())
                .build();
    }

    @Override
    public PoliceDepartmentEntity toEntity(PoliceDepartmentDto dto) {
        return PoliceDepartmentEntity.builder()
                .chatId(dto.getChatId())
                .currentPrice(dto.getCurrentPrice())
                .id(dto.getId())
                .owner(userMapper.toEntity(dto.getOwner()))
                .build();
    }
}
