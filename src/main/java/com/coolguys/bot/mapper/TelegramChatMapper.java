package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.TelegramChat;
import com.coolguys.bot.entity.TelegramChatEntity;
import org.springframework.stereotype.Component;

@Component
public class TelegramChatMapper implements EntityToDtoMapper<TelegramChatEntity, TelegramChat> {
    @Override
    public TelegramChat toDto(TelegramChatEntity entity) {
        if (entity == null) {
            return null;
        }
        return TelegramChat.builder()
                .premium(entity.getPremium())
                .id(entity.getId())
                .name(entity.getName())
                .active(entity.getActive())
                .build();
    }

    @Override
    public TelegramChatEntity toEntity(TelegramChat dto) {
        if (dto == null) {
            return null;
        }
        return TelegramChatEntity.builder()
                .id(dto.getId())
                .name(dto.getName())
                .premium(dto.getPremium())
                .active(dto.getActive())
                .build();
    }
}
