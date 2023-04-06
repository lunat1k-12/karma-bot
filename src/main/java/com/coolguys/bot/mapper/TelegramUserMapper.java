package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.TelegramUser;
import com.coolguys.bot.dto.Zodiac;
import com.coolguys.bot.entity.TelegramUserEntity;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TelegramUserMapper implements EntityToDtoMapper<TelegramUserEntity, TelegramUser>{
    @Override
    public TelegramUser toDto(TelegramUserEntity entity) {
        if (entity == null) {
            return null;
        }
        return TelegramUser.builder()
                .firstName(entity.getFirstName())
                .id(entity.getId())
                .lastName(entity.getLastName())
                .username(entity.getUsername())
                .zodiac(Zodiac.getByName(entity.getZodiac()))
                .build();
    }

    @Override
    public TelegramUserEntity toEntity(TelegramUser dto) {
        if (dto == null) {
            return null;
        }
        return TelegramUserEntity.builder()
                .firstName(dto.getFirstName())
                .id(dto.getId())
                .lastName(dto.getLastName())
                .username(dto.getUsername())
                .zodiac(Optional.ofNullable(dto.getZodiac())
                        .map(Zodiac::getName)
                        .orElse(null))
                .build();
    }
}
