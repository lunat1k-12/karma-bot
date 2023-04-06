package com.coolguys.bot.service.external;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.QueryDataDto;
import com.coolguys.bot.dto.Zodiac;
import com.coolguys.bot.mapper.TelegramUserMapper;
import com.coolguys.bot.repository.TelegramUserRepository;
import com.coolguys.bot.service.KeyboardService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZodiacService {

    private final KeyboardService keyboardService;
    private final TelegramUserRepository telegramUserRepository;
    private final TelegramUserMapper telegramUserMapper;
    private final TelegramBot bot;

    public void processZodiacSelection(ChatAccount acc, QueryDataDto dto, Integer messageId) {
        if (!acc.getId().equals(dto.getOriginalAccId())) {
            return;
        }

        keyboardService.deleteOrUpdateKeyboardMessage(acc.getChat().getId(), messageId);
        acc.getUser().setZodiac(Zodiac.getByName(dto.getOption()));
        telegramUserRepository.save(telegramUserMapper.toEntity(acc.getUser()));
        log.info("{} zodiac set for {}", acc.getUser().getZodiac().getName(), acc.getUser().getUsername());
        bot.execute(new SendMessage(acc.getChat().getId(), "Новий знак зодіаку встановленно."));
    }
}
