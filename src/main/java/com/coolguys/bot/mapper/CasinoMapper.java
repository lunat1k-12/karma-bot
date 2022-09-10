package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.CasinoDto;
import com.coolguys.bot.entity.CasinoEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CasinoMapper implements EntityToDtoMapper<CasinoEntity, CasinoDto> {

    private final UserMapper userMapper;

    @Override
    public CasinoDto toDto(CasinoEntity entity) {
        return CasinoDto.builder()
                .owner(userMapper.toDto(entity.getOwner()))
                .chatId(entity.getChatId())
                .currentPrice(entity.getCurrentPrice())
                .id(entity.getId())
                .build();
    }

    @Override
    public CasinoEntity toEntity(CasinoDto dto) {
        return CasinoEntity.builder()
                .chatId(dto.getChatId())
                .currentPrice(dto.getCurrentPrice())
                .id(dto.getId())
                .owner(userMapper.toEntity(dto.getOwner()))
                .build();
    }
}
