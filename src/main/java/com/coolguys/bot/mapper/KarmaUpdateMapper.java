package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.KarmaUpdate;
import com.coolguys.bot.dto.KarmaUpdateType;
import com.coolguys.bot.entity.KarmaUpdateEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KarmaUpdateMapper implements EntityToDtoMapper<KarmaUpdateEntity, KarmaUpdate> {

    private final UserMapper userMapper;
    @Override
    public KarmaUpdate toDto(KarmaUpdateEntity entity) {
        return KarmaUpdate.builder()
                .targetUser(userMapper.toDto(entity.getTargetUser()))
                .type(KarmaUpdateType.getById(entity.getType()))
                .originUserId(entity.getOriginUserId())
                .id(entity.getId())
                .chatId(entity.getChatId())
                .date(entity.getDate())
                .build();
    }

    @Override
    public KarmaUpdateEntity toEntity(KarmaUpdate dto) {
        return KarmaUpdateEntity.builder()
                .chatId(dto.getChatId())
                .targetUser(userMapper.toEntity(dto.getTargetUser()))
                .id(dto.getId())
                .type(dto.getType().getId())
                .originUserId(dto.getOriginUserId())
                .date(dto.getDate())
                .build();
    }
}
