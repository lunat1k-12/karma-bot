package com.coolguys.bot.service;

import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.entity.GuardEntity;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.BanRecordRepository;
import com.coolguys.bot.repository.GuardRepository;
import com.coolguys.bot.repository.UserRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class GuardService {

    public static final int GUARD_PRICE = 200;
    private final GuardRepository guardRepository;
    private final UserRepository userRepository;
    private final BanRecordRepository banRecordRepository;
    private final UserMapper userMapper;
    private final TelegramBot bot;

    public void buyGuard(UserInfo originUser) {
        if (GUARD_PRICE > originUser.getSocialCredit()) {
            bot.execute(new SendMessage(originUser.getChatId(), "В тебе нема грошей на наші послуги"));
            return;
        }

        if (!banRecordRepository.findByUserAndChatIdAndExpiresAfter(userMapper.toEntity(originUser),
                originUser.getChatId(), LocalDateTime.now()).isEmpty()) {
            bot.execute(new SendMessage(originUser.getChatId(), "Ти не можеш купити охорону поки ти у в'язниці"));
            return;
        }

        if (doesHaveGuard(originUser)) {
            bot.execute(new SendMessage(originUser.getChatId(), "В тебе вже є охорона!"));
            return;
        }

        originUser.minusCredit(GUARD_PRICE);
        userRepository.save(userMapper.toEntity(originUser));
        guardRepository.save(GuardEntity.builder()
                .user(userMapper.toEntity(originUser))
                .expires(LocalDateTime.now().plusHours(24))
                .chatId(originUser.getChatId())
                .build());

        bot.execute(new SendMessage(originUser.getChatId(), "Тепер твої гроші під охороною хлопче!"));
    }

    public boolean doesHaveGuard(UserInfo targetUser) {
        return !guardRepository.findByUserAndChatIdAndExpiresAfter(userMapper.toEntity(targetUser),
                targetUser.getChatId(), LocalDateTime.now()).isEmpty();
    }

    @Transactional
    public void deleteGuard(UserInfo originUser) {
        guardRepository.deleteByUser(userMapper.toEntity(originUser));
    }
}
