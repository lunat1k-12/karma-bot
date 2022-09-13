package com.coolguys.bot.service;

import com.coolguys.bot.dto.PoliceDepartmentDto;
import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.entity.PoliceDepartmentEntity;
import com.coolguys.bot.mapper.PoliceDepartmentMapper;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.PoliceDepartmentRepository;
import com.coolguys.bot.repository.UserRepository;
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
    private final PoliceDepartmentRepository policeDepartmentRepository;
    private final PoliceDepartmentMapper policeDepartmentMapper;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final TelegramBot bot;

    public void processFine(UserInfo user, Integer fine) {
        PoliceDepartmentDto pd = findOrCreatePdByChatID(user.getChatId());
        int policePart = Double.valueOf(Math.floor(fine / 2d)).intValue();

        if (pd.getOwner() != null) {
            UserInfo pdOwner = pd.getOwner();
            pdOwner.plusCredit(policePart);
            bot.execute(new SendMessage(user.getChatId(),
                    String.format("%s забирає собі власник поліцейської ділянки", policePart)));

            userRepository.save(userMapper.toEntity(pdOwner));
        }

        user.minusCredit(fine);
        userRepository.save(userMapper.toEntity(user));
    }

    public void dropPdOwner(Long chatId) {
        PoliceDepartmentDto pd = findOrCreatePdByChatID(chatId);
        pd.setOwner(null);
        pd.setCurrentPrice(INITIAL_PD_PRICE);
        policeDepartmentRepository.save(policeDepartmentMapper.toEntity(pd));
    }

    public boolean buyPoliceDepartment(UserInfo user) {
        PoliceDepartmentDto pd = findOrCreatePdByChatID(user.getChatId());

        if (user.getSocialCredit() < pd.getCurrentPrice()) {
            bot.execute(new SendMessage(user.getChatId(),
                    String.format("За ці копійки поліцію не купиш. Актуальна ціна - %s", pd.getCurrentPrice())));
            return false;
        }

        if (pd.getOwner() != null && pd.getOwner().getId().equals(user.getId())) {
            bot.execute(new SendMessage(user.getChatId(), "Поліція вже твоя!"));
            return false;
        }

        user.minusCredit(pd.getCurrentPrice());
        pd.plusPrice(PD_PRICE_STEP);
        pd.setOwner(user);
        userRepository.save(userMapper.toEntity(user));
        policeDepartmentRepository.save(policeDepartmentMapper.toEntity(pd));
        bot.execute(new SendMessage(user.getChatId(),
                String.format("@%s тепер новий властник Поліцейської ділянки!", user.getUsername())));
        bot.execute(new SendSticker(user.getChatId(), PD_BUY_STICKER));
        log.info("PD bought by {}", user.getUsername());
        return  true;
    }

    public PoliceDepartmentDto findOrCreatePdByChatID(Long chatId) {
        return policeDepartmentRepository.findByChatId(chatId)
                .map(policeDepartmentMapper::toDto)
                .orElseGet(() -> createInitialPd(chatId));
    }

    private PoliceDepartmentDto createInitialPd(Long chatId) {
        return policeDepartmentMapper.toDto(policeDepartmentRepository.save(PoliceDepartmentEntity.builder()
                .currentPrice(INITIAL_PD_PRICE)
                .chatId(chatId)
                .build()));
    }
}
