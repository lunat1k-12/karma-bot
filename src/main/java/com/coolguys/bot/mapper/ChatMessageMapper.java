package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.ChatMessage;
import com.coolguys.bot.entity.ChatMessageEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Deprecated
public class ChatMessageMapper implements EntityToDtoMapper<ChatMessageEntity, ChatMessage> {

    private final UserMapper userMapper;

    @Override
    public ChatMessage toDto(ChatMessageEntity entity) {
        return ChatMessage.builder()
                .user(userMapper.toDto(entity.getUser()))
                .message(entity.getMessage())
                .date(entity.getDate())
                .id(entity.getId())
                .chatId(entity.getChatId())
                .build();
    }

    @Override
    public ChatMessageEntity toEntity(ChatMessage dto) {
        return ChatMessageEntity.builder()
                .message(dto.getMessage())
                .date(dto.getDate())
                .id(dto.getId())
                .user(userMapper.toEntity(dto.getUser()))
                .chatId(dto.getChatId())
                .build();
    }
}
