package com.coolguys.bot.service;

import com.coolguys.bot.dto.CasinoDto;
import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.entity.CasinoEntity;
import com.coolguys.bot.mapper.CasinoMapper;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.CasinoRepository;
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
    private static final Integer INITIAL_CASINO_PRICE = 600;
    private static final Integer CASINO_PRICE_STEP = 100;
    private static final String MONEY_STICKER = "CAACAgIAAxkBAAIC-GMcXOr50dUY-IjO4W9Ry5Taq8fqAAIrDAACIjBYS8hE4ljSp2EUKQQ";

    public void buyCasino(UserInfo user, TelegramBot bot) {
        CasinoDto casino = findOrCreateCasinoByChatID(user.getChatId());

        if (user.getSocialCredit() < casino.getCurrentPrice()) {
            bot.execute(new SendMessage(user.getChatId(),
                    String.format("За ці копійки казино не купиш. Актуальна ціна - %s", casino.getCurrentPrice())));
            return;
        }

        if (casino.getOwner() != null && casino.getOwner().getId().equals(user.getId())) {
            bot.execute(new SendMessage(user.getChatId(), "Казино вже твоє!"));
            return;
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
    }

    public void dropCasinoOwner(Long chatId) {
        CasinoDto casino = findOrCreateCasinoByChatID(chatId);
        casino.setOwner(null);
        casino.setCurrentPrice(INITIAL_CASINO_PRICE);
        casinoRepository.save(casinoMapper.toEntity(casino));
    }

    public CasinoDto findOrCreateCasinoByChatID(Long chatId) {
        return casinoRepository.findByChatId(chatId)
                .map(casinoMapper::toDto)
                .orElseGet(() -> createInitialCasino(chatId));
    }
    private CasinoDto createInitialCasino(Long chatId) {
        return casinoMapper.toDto(casinoRepository.save(CasinoEntity.builder()
                .currentPrice(INITIAL_CASINO_PRICE)
                .chatId(chatId)
                .build()));
    }
}
