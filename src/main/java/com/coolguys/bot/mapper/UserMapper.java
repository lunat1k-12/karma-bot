package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.dto.UserStatus;
import com.coolguys.bot.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper implements EntityToDtoMapper<UserEntity, UserInfo> {

    @Override
    public UserInfo toDto(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        return UserInfo.builder()
                .socialCredit(entity.getSocialCredit())
                .telegramId(entity.getTelegramId())
                .chatId(entity.getChatId())
                .id(entity.getId())
                .username(entity.getUsername())
                .status(UserStatus.getById(entity.getStatus()))
                .build();
    }

    @Override
    public UserEntity toEntity(UserInfo dto) {
        if (dto == null) {
            return null;
        }

        return UserEntity.builder()
                .socialCredit(dto.getSocialCredit())
                .id(dto.getId())
                .username(dto.getUsername())
                .chatId(dto.getChatId())
                .telegramId(dto.getTelegramId())
                .status(dto.getStatus().getId())
                .build();
    }
}
