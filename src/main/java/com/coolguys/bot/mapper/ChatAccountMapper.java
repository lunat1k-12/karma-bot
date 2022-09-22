package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.UserStatus;
import com.coolguys.bot.entity.ChatAccountEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ChatAccountMapper implements EntityToDtoMapper<ChatAccountEntity, ChatAccount> {

    private final TelegramUserMapper telegramUserMapper;
    private final TelegramChatMapper telegramChatMapper;

    @Override
    public ChatAccount toDto(ChatAccountEntity entity) {
        if (entity == null) {
            return null;
        }
        return ChatAccount.builder()
                .chat(telegramChatMapper.toDto(entity.getChat()))
                .user(telegramUserMapper.toDto(entity.getUser()))
                .id(entity.getId())
                .socialCredit(entity.getSocialCredit())
                .status(UserStatus.getById(entity.getStatus()))
                .build();
    }

    @Override
    public ChatAccountEntity toEntity(ChatAccount dto) {
        if (dto == null) {
            return null;
        }
        return ChatAccountEntity.builder()
                .chat(telegramChatMapper.toEntity(dto.getChat()))
                .id(dto.getId())
                .socialCredit(dto.getSocialCredit())
                .status(dto.getStatus().getId())
                .user(telegramUserMapper.toEntity(dto.getUser()))
                .build();
    }
}
