package com.coolguys.bot.service;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.TelegramPoliceDepartment;
import com.coolguys.bot.dto.TelegramUser;
import com.coolguys.bot.entity.TelegramPoliceDepartmentEntity;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.mapper.TelegramPoliceDepartmentMapper;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.repository.TelegramPoliceDepartmentRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendSticker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class PoliceDepartmentService {
    private static final Integer INITIAL_PD_PRICE = 600;
    private static final Integer PD_PRICE_STEP = 100;
    private static final String PD_BUY_STICKER = "CAACAgIAAxkBAAIFxGMgvtuIMCLUGvpcfJpLNoJE-mKcAAJJAANSiZEjjN0lWOTP9-8pBA";
    private final TelegramBot bot;
    private final TelegramPoliceDepartmentRepository telegramPoliceDepartmentRepository;
    private final TelegramPoliceDepartmentMapper telegramPoliceDepartmentMapper;
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;

    public void processFine(ChatAccount user, Integer fine) {
        TelegramPoliceDepartment pd = findOrCreateTelegramPdByChatID(user.getChat().getId());
        int policePart = Double.valueOf(Math.floor(fine / 2d)).intValue();

        if (pd.getOwner() != null) {
            TelegramUser pdOwner = pd.getOwner();
            ChatAccount pdOwnerAcc = chatAccountRepository.findByUserIdAndChatId(pdOwner.getId(), user.getChat().getId())
                    .map(chatAccountMapper::toDto)
                    .orElseThrow();
            pdOwnerAcc.plusCredit(policePart);
            bot.execute(new SendMessage(user.getChat().getId(),
                    String.format("%s забирає собі власник поліцейської ділянки", policePart)));

            chatAccountRepository.save(chatAccountMapper.toEntity(pdOwnerAcc));
        }

        user.minusCredit(fine);
        chatAccountRepository.save(chatAccountMapper.toEntity(user));
    }

    public void dropPdOwner(Long chatId) {
        TelegramPoliceDepartment pd = findOrCreateTelegramPdByChatID(chatId);
        pd.setOwner(null);
        pd.setCurrentPrice(INITIAL_PD_PRICE);
        telegramPoliceDepartmentRepository.save(telegramPoliceDepartmentMapper.toEntity(pd));
    }

    public boolean buyPoliceDepartment(ChatAccount acc) {
        TelegramPoliceDepartment pd = findOrCreateTelegramPdByChatID(acc.getChat().getId());

        if (acc.getSocialCredit() < pd.getCurrentPrice()) {
            bot.execute(new SendMessage(acc.getChat().getId(),
                    String.format("За ці копійки поліцію не купиш. Актуальна ціна - %s", pd.getCurrentPrice())));
            return false;
        }

        if (pd.getOwner() != null && pd.getOwner().getId().equals(acc.getUser().getId())) {
            bot.execute(new SendMessage(acc.getChat().getId(), "Поліція вже твоя!"));
            return false;
        }

        acc.minusCredit(pd.getCurrentPrice());
        pd.plusPrice(PD_PRICE_STEP);
        pd.setOwner(acc.getUser());
        chatAccountRepository.save(chatAccountMapper.toEntity(acc));
        telegramPoliceDepartmentRepository.save(telegramPoliceDepartmentMapper.toEntity(pd));
        bot.execute(new SendMessage(acc.getChat().getId(),
                String.format("@%s тепер новий властник Поліцейської ділянки!", acc.getUser().getUsername())));
        bot.execute(new SendSticker(acc.getChat().getId(), PD_BUY_STICKER));
        log.info("PD bought by {}", acc.getUser().getUsername());
        return true;
    }

    public TelegramPoliceDepartment findOrCreateTelegramPdByChatID(Long chatId) {
        return telegramPoliceDepartmentRepository.findByChatId(chatId)
                .map(telegramPoliceDepartmentMapper::toDto)
                .orElseGet(() -> createInitialTelegramPd(chatId));
    }

    private TelegramPoliceDepartment createInitialTelegramPd(Long chatId) {
        return telegramPoliceDepartmentMapper.toDto(
                telegramPoliceDepartmentRepository.save(TelegramPoliceDepartmentEntity.builder()
                        .chatId(chatId)
                        .currentPrice(INITIAL_PD_PRICE)
                        .build()));
    }
}
