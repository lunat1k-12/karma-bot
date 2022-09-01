package com.coolguys.bot.service;

import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.entity.DiceRequestEntity;
import com.coolguys.bot.mapper.UserMapper;
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

    private final String PLUS_STICKER_ID = "CAACAgEAAxkBAAMMYw5PHFp_VtUGXqHA_-QeW-BqtusAAgIAA39wRhwFzGTYNyIryCkE";

    private final String MINUS_STICKER_ID = "CAACAgEAAxkBAAMUYw5PxdZ65ASPUMgrHHyiyiSPdVQAAgMAA39wRhxDWYhLWOdGzSkE";

    private static final Integer REMOVE_BAN_PRICE = 20;

    private final DiceRequestRepository diceRequestRepository;
    private final UserMapper userMapper;
    private final UserRepository userRepository;

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
        originUser.minusCredit(20);
        userRepository.save(userMapper.toEntity(originUser));
        bot.execute(new SendMessage(originUser.getChatId(), String.format("@%s можеш грати знову", originUser.getUsername())));
    }
    public void processDice(Message message, UserInfo originUser, TelegramBot bot) {

        if (diceRequestRepository.findAllByUserAndChatIdAndDateGreaterThan(userMapper.toEntity(originUser),
                message.chat().id(),
                LocalDateTime.now().minusHours(1L)).size() >= 3) {
            bot.execute(new SendMessage(message.chat().id(), "Відпочинь лудоман."));
            return;
        }
        SendResponse response = bot.execute(new SendDice(message.chat().id())
                .emoji(message.dice().emoji()));

        if (message.dice().value() > response.message().dice().value()) {
            bot.execute(new SendMessage(message.chat().id(), String.format("@%s переміг", originUser.getUsername())));
            originUser.setSocialCredit(originUser.getSocialCredit() + 20);
            userRepository.save(userMapper.toEntity(originUser));
            bot.execute(new SendSticker(message.chat().id(), PLUS_STICKER_ID));
        } else if (message.dice().value().equals(response.message().dice().value())) {
            bot.execute(new SendMessage(message.chat().id(), "Нічия"));
        } else {
            bot.execute(new SendMessage(message.chat().id(), String.format("@%s програв", originUser.getUsername())));
            originUser.setSocialCredit(originUser.getSocialCredit() - 20);
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
