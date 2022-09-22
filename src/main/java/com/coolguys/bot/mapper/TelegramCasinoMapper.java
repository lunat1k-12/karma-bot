package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.TelegramCasino;
import com.coolguys.bot.entity.TelegramCasinoEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramCasinoMapper implements EntityToDtoMapper<TelegramCasinoEntity, TelegramCasino> {
    private final TelegramUserMapper telegramUserMapper;

    @Override
    public TelegramCasino toDto(TelegramCasinoEntity entity) {
        return TelegramCasino.builder()
                .id(entity.getId())
                .owner(telegramUserMapper.toDto(entity.getOwner()))
                .currentPrice(entity.getCurrentPrice())
                .chatId(entity.getChatId())
                .build();
    }

    @Override
    public TelegramCasinoEntity toEntity(TelegramCasino dto) {
        return TelegramCasinoEntity.builder()
                .chatId(dto.getChatId())
                .currentPrice(dto.getCurrentPrice())
                .id(dto.getId())
                .owner(telegramUserMapper.toEntity(dto.getOwner()))
                .build();
    }
}
