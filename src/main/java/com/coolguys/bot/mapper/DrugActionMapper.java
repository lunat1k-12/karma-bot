package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.DrugAction;
import com.coolguys.bot.entity.DrugActionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DrugActionMapper implements EntityToDtoMapper<DrugActionEntity, DrugAction> {

    private final UserMapper userMapper;

    @Override
    public DrugAction toDto(DrugActionEntity entity) {
        return DrugAction.builder()
                .id(entity.getId())
                .expires(entity.getExpires())
                .user(userMapper.toDto(entity.getUser()))
                .chatId(entity.getChatId())
                .build();
    }

    @Override
    public DrugActionEntity toEntity(DrugAction dto) {
        return DrugActionEntity.builder()
                .expires(dto.getExpires())
                .chatId(dto.getChatId())
                .id(dto.getId())
                .user(userMapper.toEntity(dto.getUser()))
                .build();
    }
}
