package com.coolguys.bot.service;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.QueryDataDto;
import com.coolguys.bot.entity.InvestigateActionEntity;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.repository.InvestigateActionRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.coolguys.bot.dto.QueryDataDto.THIEF_INVESTIGATE_TYPE;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThiefInvestigateService {

    public static final String CANCEL_OPTION = "0";
    private final TelegramBot bot;
    private final InvestigateActionRepository investigateActionRepository;
    private final KeyboardService keyboardService;
    private final MyStatsService myStatsService;
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;

    public void processInvestigateRequest(ChatAccount acc) {
        if (investigateActionRepository.findByAccIdAndExpiresAfter(acc.getId(), LocalDateTime.now()).size() > 0) {
            bot.execute(new SendMessage(acc.getChat().getId(), "Ти не можеш зробити це знову.\nТреба зачекати."));
            return;
        }

        bot.execute(new SendMessage(acc.getChat().getId(), "Обери про кого хочеш дізнатися більше:")
                .parseMode(ParseMode.HTML)
                .replyMarkup(keyboardService.getTargetAccSelectionPersonKeyboard(acc.getChat().getId(), acc.getId(), THIEF_INVESTIGATE_TYPE)));
    }

    public void processInvestigate(ChatAccount acc, QueryDataDto dto) {
        if (acc.getId().equals(dto.getOriginalAccId())) {
            log.info("Process Thief Investigate");

            if (CANCEL_OPTION.equals(dto.getOption())) {
                bot.execute(new SendMessage(acc.getChat().getId(), "Крадій змінив свої плани"));
                return;
            }

            ChatAccount target = chatAccountRepository.findById(Long.valueOf(dto.getOption()))
                    .map(chatAccountMapper::toDto)
                    .orElseThrow();

            if (myStatsService.printPersonalStats(target, acc.getUser().getId())) {
                investigateActionRepository.save(InvestigateActionEntity.builder()
                        .account(chatAccountMapper.toEntity(acc))
                        .expires(LocalDateTime.now().plusHours(24))
                        .build());
                log.info("Investigation done");
            }
        }
    }
}
