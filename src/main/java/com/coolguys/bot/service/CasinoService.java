package com.coolguys.bot.service;

import com.coolguys.bot.dto.CasinoDto;
import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.TelegramCasino;
import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.entity.CasinoEntity;
import com.coolguys.bot.entity.TelegramCasinoEntity;
import com.coolguys.bot.mapper.CasinoMapper;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.mapper.TelegramCasinoMapper;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.CasinoRepository;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.repository.TelegramCasinoRepository;
import com.coolguys.bot.repository.UserRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendSticker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CasinoService {

    private final CasinoRepository casinoRepository;
    private final CasinoMapper casinoMapper;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final TelegramBot bot;
    private final TelegramCasinoRepository telegramCasinoRepository;
    private final TelegramCasinoMapper telegramCasinoMapper;
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;
    private static final Integer INITIAL_CASINO_PRICE = 600;
    private static final Integer CASINO_PRICE_STEP = 100;
    private static final String MONEY_STICKER = "CAACAgIAAxkBAAIC-GMcXOr50dUY-IjO4W9Ry5Taq8fqAAIrDAACIjBYS8hE4ljSp2EUKQQ";

    public boolean buyCasino(ChatAccount acc) {
        TelegramCasino casino = findOrCreateTelegramCasinoByChatID(acc.getChat().getId());

        if (acc.getSocialCredit() < casino.getCurrentPrice()) {
            bot.execute(new SendMessage(acc.getChat().getId(),
                    String.format("За ці копійки казино не купиш. Актуальна ціна - %s", casino.getCurrentPrice())));
            return false;
        }

        if (casino.getOwner() != null && casino.getOwner().getId().equals(acc.getUser().getId())) {
            bot.execute(new SendMessage(acc.getChat().getId(), "Казино вже твоє!"));
            return false;
        }

        acc.minusCredit(casino.getCurrentPrice());
        casino.plusPrice(CASINO_PRICE_STEP);
        casino.setOwner(acc.getUser());
        chatAccountRepository.save(chatAccountMapper.toEntity(acc));
        telegramCasinoRepository.save(telegramCasinoMapper.toEntity(casino));
        bot.execute(new SendMessage(acc.getChat().getId(),
                String.format("@%s тепер новий властник казино!", acc.getUser().getUsername())));
        bot.execute(new SendSticker(acc.getChat().getId(), MONEY_STICKER));
        log.info("Casino bought by {}", acc.getUser().getUsername());
        return true;
    }

    @Deprecated
    public boolean buyCasino(UserInfo user) {
        CasinoDto casino = findOrCreateCasinoByChatID(user.getChatId());

        if (user.getSocialCredit() < casino.getCurrentPrice()) {
            bot.execute(new SendMessage(user.getChatId(),
                    String.format("За ці копійки казино не купиш. Актуальна ціна - %s", casino.getCurrentPrice())));
            return false;
        }

        if (casino.getOwner() != null && casino.getOwner().getId().equals(user.getId())) {
            bot.execute(new SendMessage(user.getChatId(), "Казино вже твоє!"));
            return false;
        }

        user.minusCredit(casino.getCurrentPrice());
        casino.plusPrice(CASINO_PRICE_STEP);
        casino.setOwner(user);
        userRepository.save(userMapper.toEntity(user));
        casinoRepository.save(casinoMapper.toEntity(casino));
        bot.execute(new SendMessage(user.getChatId(),
                String.format("@%s тепер новий властник казино!", user.getUsername())));
        bot.execute(new SendSticker(user.getChatId(), MONEY_STICKER));
        log.info("Casino bought by {}", user.getUsername());
        return true;
    }

    public void dropCasinoOwner(Long chatId) {
        TelegramCasino casino = findOrCreateTelegramCasinoByChatID(chatId);
        casino.setOwner(null);
        casino.setCurrentPrice(INITIAL_CASINO_PRICE);
        telegramCasinoRepository.save(telegramCasinoMapper.toEntity(casino));
    }

    @Deprecated
    public CasinoDto findOrCreateCasinoByChatID(Long chatId) {
        return casinoRepository.findByChatId(chatId)
                .map(casinoMapper::toDto)
                .orElseGet(() -> createInitialCasino(chatId));
    }

    public TelegramCasino findOrCreateTelegramCasinoByChatID(Long chatId) {
        return telegramCasinoRepository.findByChatId(chatId)
                .map(telegramCasinoMapper::toDto)
                .orElseGet(() -> createInitialTelegramCasino(chatId));

    }

    private TelegramCasino createInitialTelegramCasino(Long chatId) {
        return telegramCasinoMapper.toDto(telegramCasinoRepository.save(TelegramCasinoEntity.builder()
                .chatId(chatId)
                .currentPrice(INITIAL_CASINO_PRICE)
                .build()));
    }
    private CasinoDto createInitialCasino(Long chatId) {
        return casinoMapper.toDto(casinoRepository.save(CasinoEntity.builder()
                .currentPrice(INITIAL_CASINO_PRICE)
                .chatId(chatId)
                .build()));
    }
}
