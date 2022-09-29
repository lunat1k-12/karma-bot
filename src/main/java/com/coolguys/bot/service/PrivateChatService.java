package com.coolguys.bot.service;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.TelegramUser;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.mapper.TelegramUserMapper;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.repository.TelegramUserRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrivateChatService {

    private final TelegramUserRepository telegramUserRepository;
    private final TelegramUserMapper telegramUserMapper;
    private final UserService userService;
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;
    private final InfoService infoService;
    private final TelegramBot bot;

    public void processPrivateMessage(Message message) {
        log.info("private message from: {}", message.from().id());
        TelegramUser user = telegramUserRepository.findById(message.from().id())
                .map(telegramUserMapper::toDto)
                .orElse(null);

        List<ChatAccount> accounts = chatAccountRepository.findByUserId(message.from().id()).stream()
                .map(chatAccountMapper::toDto)
                .collect(Collectors.toList());

        if (user == null) {
            userService.createNewUser(message.from());
            infoService.printInfo(message.chat().id());
        } else if (accounts.size() > 0){
            bot.execute(new SendMessage(message.chat().id(), "Тут ти будеш отримувати персональні сповіщення з чатів"));
        }
    }
}
