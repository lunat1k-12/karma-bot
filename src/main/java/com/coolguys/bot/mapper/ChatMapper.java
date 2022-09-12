package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.ChatDto;
import com.coolguys.bot.entity.ChatEntity;
import org.springframework.stereotype.Component;

@Component
public class ChatMapper implements EntityToDtoMapper<ChatEntity, ChatDto>{
    @Override
    public ChatDto toDto(ChatEntity entity) {
        return ChatDto.builder()
                .telegramId(entity.getTelegramId())
                .name(entity.getName())
                .id(entity.getId())
                .build();
    }

    @Override
    public ChatEntity toEntity(ChatDto dto) {
        return ChatEntity.builder()
                .id(dto.getId())
                .name(dto.getName())
                .telegramId(dto.getTelegramId())
                .build();
    }
}
