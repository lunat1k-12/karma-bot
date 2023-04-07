package com.coolguys.bot.scheduled;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.TelegramChat;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.mapper.TelegramChatMapper;
import com.coolguys.bot.mapper.TelegramMessageMapper;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.repository.TelegramChatRepository;
import com.coolguys.bot.repository.TelegramMessageRepository;
import com.coolguys.bot.service.UserService;
import com.coolguys.bot.service.external.Language;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledStateLang {

    private final TelegramChatRepository telegramChatRepository;
    private final TelegramChatMapper telegramChatMapper;
    private final TelegramMessageRepository telegramMessageRepository;
    private final TelegramMessageMapper telegramMessageMapper;
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;
    private final UserService userService;
    private final TelegramBot bot;

    @Scheduled(cron = "00 00 23 * * *", zone = "Europe/Kiev")
    @Async
    public void checkLanguage() {
        telegramChatRepository.findAllActive().stream()
                .map(telegramChatMapper::toDto)
                .forEach(this::checkLanguage);
    }

    private void checkLanguage(TelegramChat chat) {
        var chatAccounts = userService.findActiveAccByChatId(chat.getId());

        if (chatAccounts.isEmpty()) {
            log.info("no users in chat {}", chat.getName());
            return;
        }

        StringBuilder sb = new StringBuilder("Перевірка на державну:\n");

        for (ChatAccount chatAcc : chatAccounts) {
            var userMessages = telegramMessageRepository.findByUserId(chat.getId(),
                    chatAcc.getUser().getId(), LocalDateTime.now().minusDays(1)).stream()
                    .map(telegramMessageMapper::toDto)
                    .collect(Collectors.toList());

            long ukCount = userMessages.stream()
                    .filter(m -> Language.UKRAINIAN.equals(m.getLanguage()))
                    .count();

            int percentage = Double.valueOf(Math.ceil((ukCount * 100) / Integer.valueOf(userMessages.size()).doubleValue())).intValue();

            sb.append("Відсоток державності для @")
                    .append(chatAcc.getUser().getUsername())
                    .append(" дорівнює ")
                    .append(percentage)
                    .append("%\n");

            if (percentage > 60) {
                sb.append("рівень державності достатній +50 кредитів\n");
                chatAcc.plusCredit(50);
                chatAccountRepository.save(chatAccountMapper.toEntity(chatAcc));
            } else {
                sb.append("рівень державності не достатній\n");
            }
            sb.append("---------\n");
        }

        bot.execute(new SendMessage(chat.getId(), sb.toString()));
    }
}
