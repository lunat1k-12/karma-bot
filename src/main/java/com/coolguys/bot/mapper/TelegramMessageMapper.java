package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.TelegramMessage;
import com.coolguys.bot.entity.TelegramMessageEntity;
import com.coolguys.bot.service.external.Language;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramMessageMapper implements EntityToDtoMapper<TelegramMessageEntity, TelegramMessage> {
    private final TelegramUserMapper telegramUserMapper;

    @Override
    public TelegramMessage toDto(TelegramMessageEntity entity) {
        return TelegramMessage.builder()
                .message(entity.getMessage())
                .date(entity.getDate())
                .user(telegramUserMapper.toDto(entity.getUser()))
                .id(entity.getId())
                .chatId(entity.getChatId())
                .language(Language.getByCode(entity.getLanguage()))
                .build();
    }

    @Override
    public TelegramMessageEntity toEntity(TelegramMessage dto) {
        return TelegramMessageEntity.builder()
                .message(dto.getMessage())
                .chatId(dto.getChatId())
                .date(dto.getDate())
                .id(dto.getId())
                .user(telegramUserMapper.toEntity(dto.getUser()))
                .language(dto.getLanguage().getCode())
                .build();
    }
}
