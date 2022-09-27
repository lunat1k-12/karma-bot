package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.TelegramBanRecord;
import com.coolguys.bot.entity.TelegramBanRecordEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramBanRecordMapper implements EntityToDtoMapper<TelegramBanRecordEntity, TelegramBanRecord> {

    private final TelegramUserMapper telegramUserMapper;

    @Override
    public TelegramBanRecord toDto(TelegramBanRecordEntity entity) {
        return TelegramBanRecord.builder()
                .id(entity.getId())
                .user(telegramUserMapper.toDto(entity.getUser()))
                .chatId(entity.getChatId())
                .expires(entity.getExpires())
                .build();
    }

    @Override
    public TelegramBanRecordEntity toEntity(TelegramBanRecord dto) {
        return TelegramBanRecordEntity.builder()
                .chatId(dto.getChatId())
                .expires(dto.getExpires())
                .id(dto.getId())
                .user(telegramUserMapper.toEntity(dto.getUser()))
                .build();
    }
}
