package com.coolguys.bot.service;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.TelegramBanRecord;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.mapper.TelegramBanRecordMapper;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.repository.GainMoneyActionRepository;
import com.coolguys.bot.repository.TelegramBanRecordRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WatchService {

    private final TelegramBanRecordRepository telegramBanRecordRepository;
    private final TelegramBanRecordMapper telegramBanRecordMapper;
    private final StealService stealService;
    private final GainMoneyActionRepository gainMoneyActionRepository;
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;
    private final TelegramBot bot;

    public void gainMoney(ChatAccount acc) {
        log.info("gain money request from {}", acc.getUser().getUsername());
        if (stealService.isInJail(acc)) {
            bot.execute(new SendMessage(acc.getChat().getId(), "Сам відсиди спочатку"));
            return;
        }

        if (gainMoneyActionRepository.findAllByAccIdAndDate(acc.getId(), LocalDateTime.now()).size() > 0) {
            bot.execute(new SendMessage(acc.getChat().getId(), "Зараз не можеш це зробити. Дай їм відпочити."));
            return;
        }

        List<TelegramBanRecord> banRecords = telegramBanRecordRepository.findByChatIdAndDate(acc.getChat().getId(), LocalDateTime.now())
                .stream()
                .map(telegramBanRecordMapper::toDto)
                .collect(Collectors.toList());

        if (banRecords.size() == 0) {
            bot.execute(new SendMessage(acc.getChat().getId(), "У в`язниці зараз пусто"));
            return;
        }

        int totalIncome = 0;
        for (TelegramBanRecord record : banRecords) {
            int income = getIncomeFromPrisoner(record);
            totalIncome += income;
            acc.plusCredit(income);
        }

        bot.execute(new SendMessage(acc.getChat().getId(),
                String.format("@%s заробив на в`язнях %s кредитів", acc.getUser().getUsername(), totalIncome)));
        chatAccountRepository.save(chatAccountMapper.toEntity(acc));
    }

    private int getIncomeFromPrisoner(TelegramBanRecord record) {
        ChatAccount prisoner = chatAccountRepository.findByUserIdAndChatId(record.getUser().getId(), record.getChatId())
                .map(chatAccountMapper::toDto)
                .orElseThrow();

        if (prisoner.getSocialCredit() < 0) {
            return 0;
        }

        return Double.valueOf(Math.floor(prisoner.getSocialCredit() / 2d)).intValue();
    }
}
