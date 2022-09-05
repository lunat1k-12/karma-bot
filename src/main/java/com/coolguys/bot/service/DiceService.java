package com.coolguys.bot.service;

import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.entity.DiceRequestEntity;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.BanRecordRepository;
import com.coolguys.bot.repository.DiceRequestRepository;
import com.coolguys.bot.repository.UserRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendDice;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendSticker;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DiceService {
    private static final Integer REMOVE_BAN_PRICE = 100;

    private static final Integer BET_PRICE = 50;

    private static final Integer MIN_BALANCE = -50;

    private static final String PLUS_STICKER_ID = "CAACAgIAAxkBAAP2YxCaHIj1Kvj4-RG7a1T8q9Pb5MEAAt0BAAI9DegEVSWxKgJiwGEpBA";

    private static final String MINUS_STICKER_ID = "CAACAgIAAxkBAAP3YxCaiWWASKtoKwABiUu7xuGX8t8yAALFAQACPQ3oBOdBQtf1YCO8KQQ";

    private final DiceRequestRepository diceRequestRepository;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final BanRecordRepository banRecordRepository;

    public void removePlayBan(UserInfo originUser, TelegramBot bot) {
        List<DiceRequestEntity> diceRequests = diceRequestRepository.findAllByUserAndChatIdAndDateGreaterThan(userMapper.toEntity(originUser),
                originUser.getChatId(),
                LocalDateTime.now().minusHours(1L));
        if (diceRequests.size() < 3) {
            bot.execute(new SendMessage(originUser.getChatId(), "На тобі зараз нема обмежень"));
            return;
        }

        if (originUser.getSocialCredit() < REMOVE_BAN_PRICE) {
            bot.execute(new SendMessage(originUser.getChatId(), "В тебе нема кредитів на це"));
            return;
        }
        diceRequestRepository.deleteAll(diceRequests);
        originUser.minusCredit(REMOVE_BAN_PRICE);
        userRepository.save(userMapper.toEntity(originUser));
        bot.execute(new SendMessage(originUser.getChatId(), String.format("@%s можеш грати знову", originUser.getUsername())));
    }
    public void processDice(Message message, UserInfo originUser, TelegramBot bot) {

        if (diceRequestRepository.findAllByUserAndChatIdAndDateGreaterThan(userMapper.toEntity(originUser),
                message.chat().id(),
                LocalDateTime.now().minusHours(3L)).size() >= 3) {
            bot.execute(new SendMessage(message.chat().id(), "Відпочинь лудоман."));
            return;
        }

        if (!banRecordRepository.findByUserAndChatIdAndExpiresAfter(userMapper.toEntity(originUser),
                originUser.getChatId(), LocalDateTime.now()).isEmpty()) {
            bot.execute(new SendMessage(message.chat().id(), "Злочинцям не місце у казіно!."));
            return;
        }

        if ((originUser.getSocialCredit() - BET_PRICE) < MIN_BALANCE) {
            bot.execute(new SendMessage(message.chat().id(), "Тобі не місце в цьому казіно жебрак."));
            return;
        }

        SendResponse response = bot.execute(new SendDice(message.chat().id())
                .emoji(message.dice().emoji()));

        if (message.dice().value() > response.message().dice().value()) {
            bot.execute(new SendMessage(message.chat().id(), String.format("@%s переміг і отримує %s кредитів", originUser.getUsername(), BET_PRICE)));
            originUser.plusCredit(BET_PRICE);
            userRepository.save(userMapper.toEntity(originUser));
            bot.execute(new SendSticker(message.chat().id(), PLUS_STICKER_ID));
        } else if (message.dice().value().equals(response.message().dice().value())) {
            bot.execute(new SendMessage(message.chat().id(), "Нічия"));
        } else {
            bot.execute(new SendMessage(message.chat().id(), String.format("@%s програв %s кредитів", originUser.getUsername(), BET_PRICE)));
            originUser.minusCredit(BET_PRICE);
            userRepository.save(userMapper.toEntity(originUser));
            bot.execute(new SendSticker(message.chat().id(), MINUS_STICKER_ID));
        }
        diceRequestRepository.save(DiceRequestEntity.builder()
                .chatId(message.chat().id())
                .date(LocalDateTime.now())
                .user(userMapper.toEntity(originUser))
                .build());
    }
}
