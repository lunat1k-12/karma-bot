package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.KarmaUpdateType;
import com.coolguys.bot.dto.TelegramKarmaUpdate;
import com.coolguys.bot.entity.TelegramKarmaUpdateEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TelegramKarmaUpdateMapper implements EntityToDtoMapper<TelegramKarmaUpdateEntity, TelegramKarmaUpdate> {

    private final TelegramUserMapper telegramUserMapper;

    @Override
    public TelegramKarmaUpdate toDto(TelegramKarmaUpdateEntity entity) {
        return TelegramKarmaUpdate.builder()
                .targetUser(telegramUserMapper.toDto(entity.getTargetUser()))
                .type(KarmaUpdateType.getById(entity.getType()))
                .id(entity.getId())
                .originUserId(entity.getOriginUserId())
                .date(entity.getDate())
                .chatId(entity.getChatId())
                .build();
    }

    @Override
    public TelegramKarmaUpdateEntity toEntity(TelegramKarmaUpdate dto) {
        return TelegramKarmaUpdateEntity.builder()
                .targetUser(telegramUserMapper.toEntity(dto.getTargetUser()))
                .chatId(dto.getChatId())
                .date(dto.getDate())
                .type(dto.getType().getId())
                .id(dto.getId())
                .originUserId(dto.getOriginUserId())
                .build();
    }
}
