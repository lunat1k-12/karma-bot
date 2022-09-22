package com.coolguys.bot.service;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.entity.GuardEntity;
import com.coolguys.bot.entity.TelegramGuardEntity;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.mapper.TelegramUserMapper;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.repository.GuardRepository;
import com.coolguys.bot.repository.TelegramBanRecordRepository;
import com.coolguys.bot.repository.TelegramGuardRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;

@Service
@RequiredArgsConstructor
public class GuardService {

    public static final int GUARD_PRICE = 200;
    private final GuardRepository guardRepository;
    private final UserMapper userMapper;
    private final TelegramBot bot;
    private final GuardDepartmentService guardDepartmentService;
    private final TelegramGuardRepository telegramGuardRepository;
    private final TelegramUserMapper telegramUserMapper;
    private final TelegramBanRecordRepository telegramBanRecordRepository;
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;

    public void buyGuard(ChatAccount originAcc) {
        if (GUARD_PRICE > originAcc.getSocialCredit()) {
            bot.execute(new SendMessage(originAcc.getChat().getId(), "В тебе нема грошей на наші послуги"));
            return;
        }

        if (!telegramBanRecordRepository.findByUserAndChatIdAndExpiresAfter(telegramUserMapper.toEntity(originAcc.getUser()),
                originAcc.getChat().getId(), LocalDateTime.now()).isEmpty()) {
            bot.execute(new SendMessage(originAcc.getChat().getId(), "Ти не можеш купити охорону поки ти у в'язниці"));
            return;
        }

        if (doesHaveGuard(originAcc)) {
            bot.execute(new SendMessage(originAcc.getChat().getId(), "В тебе вже є охорона!"));
            return;
        }

        originAcc.minusCredit(GUARD_PRICE);
        chatAccountRepository.save(chatAccountMapper.toEntity(originAcc));
        telegramGuardRepository.save(TelegramGuardEntity.builder()
                .user(telegramUserMapper.toEntity(originAcc.getUser()))
                .expires(LocalDateTime.now().plusHours(24))
                .chatId(originAcc.getChat().getId())
                .build());

        guardDepartmentService.processGuardOwnerIncome(originAcc.getChat().getId(), GUARD_PRICE);
        bot.execute(new SendMessage(originAcc.getChat().getId(), "Тепер твої гроші під охороною хлопче!"));
    }

    public boolean doesHaveGuard(ChatAccount targetAccount) {
        return !telegramGuardRepository.findByUserAndChatIdAndExpiresAfter(telegramUserMapper.toEntity(targetAccount.getUser()),
                targetAccount.getChat().getId(), LocalDateTime.now()).isEmpty();
    }

    @Deprecated
    public boolean doesHaveGuard(UserInfo targetUser) {
        return !guardRepository.findByUserAndChatIdAndExpiresAfter(userMapper.toEntity(targetUser),
                targetUser.getChatId(), LocalDateTime.now()).isEmpty();
    }

    public String getGuardTillLabel(ChatAccount targetAcc) {
        return telegramGuardRepository.findByUserAndChatIdAndExpiresAfter(telegramUserMapper.toEntity(targetAcc.getUser()),
                        targetAcc.getChat().getId(), LocalDateTime.now()).stream()
                .max(Comparator.comparing(TelegramGuardEntity::getExpires))
                .map(TelegramGuardEntity::getExpires)
                .map(DateConverter::localDateTimeToStringLabel)
                .orElse(null);
    }

    public String getGuardTillLabel(UserInfo targetUser) {
        return guardRepository.findByUserAndChatIdAndExpiresAfter(userMapper.toEntity(targetUser),
                        targetUser.getChatId(), LocalDateTime.now()).stream()
                .max(Comparator.comparing(GuardEntity::getExpires))
                .map(GuardEntity::getExpires)
                .map(DateConverter::localDateTimeToStringLabel)
                .orElse(null);
    }

    @Transactional
    @Deprecated
    public void deleteGuard(UserInfo originUser) {
        guardRepository.deleteByUser(userMapper.toEntity(originUser));
    }

    public void deleteGuard(ChatAccount originAcc) {
        telegramGuardRepository.deleteByUserAndChatId(telegramUserMapper.toEntity(originAcc.getUser()), originAcc.getChat().getId());
    }
}
