package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.TelegramDrugAction;
import com.coolguys.bot.entity.TelegramDrugActionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramDrugActionMapper implements EntityToDtoMapper<TelegramDrugActionEntity, TelegramDrugAction> {

    private final TelegramUserMapper telegramUserMapper;

    @Override
    public TelegramDrugAction toDto(TelegramDrugActionEntity entity) {
        return TelegramDrugAction.builder()
                .user(telegramUserMapper.toDto(entity.getUser()))
                .id(entity.getId())
                .expires(entity.getExpires())
                .chatId(entity.getChatId())
                .build();
    }

    @Override
    public TelegramDrugActionEntity toEntity(TelegramDrugAction dto) {
        return TelegramDrugActionEntity.builder()
                .chatId(dto.getChatId())
                .expires(dto.getExpires())
                .id(dto.getId())
                .user(telegramUserMapper.toEntity(dto.getUser()))
                .build();
    }
}
