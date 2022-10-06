package com.coolguys.bot.scheduled;

import com.coolguys.bot.dto.TelegramChat;
import com.coolguys.bot.mapper.TelegramChatMapper;
import com.coolguys.bot.repository.TelegramChatRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.GetChat;
import com.pengrad.telegrambot.response.GetChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledCheckChats {

    private final TelegramChatRepository telegramChatRepository;
    private final TelegramChatMapper telegramChatMapper;
    private final TelegramBot bot;

    @Scheduled(cron = "00 00 09 * * *")
    @Async
    public void checkChats() {
        telegramChatRepository.findAllActive().stream()
                .map(telegramChatMapper::toDto)
                .forEach(this::processChat);
    }

    private void processChat(TelegramChat chat) {
        GetChatResponse response = bot.execute(new GetChat(chat.getId()));
        if (!response.isOk() && response.errorCode() == 400) {
            log.info("Mark {} as inactive", chat.getName());
            chat.setActive(false);
            telegramChatRepository.save(telegramChatMapper.toEntity(chat));
        }
    }
}
