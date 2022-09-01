package com.coolguys.bot.service;

import com.coolguys.bot.dto.ChatMessage;
import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.mapper.ChatMessageMapper;
import com.coolguys.bot.repository.ChatMessageRepository;
import com.pengrad.telegrambot.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MessagesService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatMessageMapper chatMessageMapper;

    public void saveMessage(UserInfo originUser, Message message) {
        ChatMessage msg = ChatMessage.builder()
                .user(originUser)
                .date(LocalDateTime.now())
                .message(message.text())
                .chatId(message.chat().id())
                .build();
        chatMessageRepository.save(chatMessageMapper.toEntity(msg));
    }
}
