package com.coolguys.bot.service;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.entity.TelegramMessageEntity;
import com.coolguys.bot.mapper.TelegramUserMapper;
import com.coolguys.bot.repository.TelegramMessageRepository;
import com.coolguys.bot.service.external.Language;
import com.coolguys.bot.service.external.LanguageDetectorService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MessagesService {
    private final TelegramMessageRepository telegramMessageRepository;
    private final TelegramUserMapper telegramUserMapper;
    private final LanguageDetectorService languageDetectorService;

    public void saveMessage(ChatAccount originUser, Message message) {
        Language lang = languageDetectorService.checkMessageLanguage(message.text());

        telegramMessageRepository.save(TelegramMessageEntity.builder()
                .user(telegramUserMapper.toEntity(originUser.getUser()))
                .date(LocalDateTime.now())
                .message(message.text())
                .chatId(message.chat().id())
                .language(lang.getCode())
                .build());
    }
}
