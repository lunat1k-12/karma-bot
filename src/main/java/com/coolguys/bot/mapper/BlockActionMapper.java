package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.BlockActionDto;
import com.coolguys.bot.entity.BlockActionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BlockActionMapper implements EntityToDtoMapper<BlockActionEntity, BlockActionDto> {

    private final ChatAccountMapper chatAccountMapper;

    @Override
    public BlockActionDto toDto(BlockActionEntity entity) {
        return BlockActionDto.builder()
                .id(entity.getId())
                .acc(chatAccountMapper.toDto(entity.getAcc()))
                .type(BlockActionDto.BlockType.getByValue(entity.getType()))
                .expires(entity.getExpires())
                .build();
    }

    @Override
    public BlockActionEntity toEntity(BlockActionDto dto) {
        return BlockActionEntity.builder()
                .acc(chatAccountMapper.toEntity(dto.getAcc()))
                .expires(dto.getExpires())
                .id(dto.getId())
                .type(dto.getType().getValue())
                .build();
    }
}
