package com.coolguys.bot.service;

import com.coolguys.bot.dto.CasinoDto;
import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.TelegramCasino;
import com.coolguys.bot.dto.TelegramUser;
import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.entity.DiceRequestEntity;
import com.coolguys.bot.entity.TelegramDiceRequestEntity;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.mapper.TelegramUserMapper;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.BanRecordRepository;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.repository.DiceRequestRepository;
import com.coolguys.bot.repository.TelegramBanRecordRepository;
import com.coolguys.bot.repository.TelegramDiceRequestRepository;
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
    private final TelegramDiceRequestRepository telegramDiceRequestRepository;
    private final TelegramUserMapper telegramUserMapper;
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final BanRecordRepository banRecordRepository;
    private final CasinoService casinoService;
    private final TelegramBanRecordRepository telegramBanRecordRepository;
    private final TelegramBot bot;

    public void removePlayBan(ChatAccount originAcc) {
        List<TelegramDiceRequestEntity> diceRequests = telegramDiceRequestRepository.findAllByUserAndChatIdAndDateGreaterThan(telegramUserMapper.toEntity(originAcc.getUser()),
                originAcc.getChat().getId(),
                LocalDateTime.now().minusHours(1L));

        if (diceRequests.size() < 3) {
            bot.execute(new SendMessage(originAcc.getChat().getId(), "На тобі зараз нема обмежень"));
            return;
        }

        if (originAcc.getSocialCredit() < REMOVE_BAN_PRICE) {
            bot.execute(new SendMessage(originAcc.getChat().getId(), "В тебе нема кредитів на це"));
            return;
        }
        telegramDiceRequestRepository.deleteAll(diceRequests);
        originAcc.minusCredit(REMOVE_BAN_PRICE);
        chatAccountRepository.save(chatAccountMapper.toEntity(originAcc));
        bot.execute(new SendMessage(originAcc.getChat().getId(), String.format("@%s можеш грати знову", originAcc.getUser().getUsername())));
    }

    public void processDice(Message message, ChatAccount originAcc) {

        if (telegramDiceRequestRepository.findAllByUserAndChatIdAndDateGreaterThan(telegramUserMapper.toEntity(originAcc.getUser()),
                message.chat().id(),
                LocalDateTime.now().minusHours(3L)).size() >= 3) {
            bot.execute(new SendMessage(message.chat().id(), "Відпочинь лудоман."));
            return;
        }

        if (!telegramBanRecordRepository.findByUserAndChatIdAndExpiresAfter(telegramUserMapper.toEntity(originAcc.getUser()),
                originAcc.getChat().getId(), LocalDateTime.now()).isEmpty()) {
            bot.execute(new SendMessage(message.chat().id(), "Злочинцям не місце у казино!."));
            return;
        }

        if ((originAcc.getSocialCredit() - BET_PRICE) < MIN_BALANCE) {
            bot.execute(new SendMessage(message.chat().id(), "Тобі не місце в цьому казино жебрак."));
            return;
        }

        SendResponse response = bot.execute(new SendDice(message.chat().id())
                .emoji(message.dice().emoji()));

        if (message.dice().value() > response.message().dice().value()) {
            bot.execute(new SendMessage(message.chat().id(), String.format("@%s переміг і отримує %s кредитів", originAcc.getUser().getUsername(), BET_PRICE)));
            originAcc.plusCredit(BET_PRICE);
            chatAccountRepository.save(chatAccountMapper.toEntity(originAcc));
            bot.execute(new SendSticker(message.chat().id(), PLUS_STICKER_ID));
        } else if (message.dice().value().equals(response.message().dice().value())) {
            bot.execute(new SendMessage(message.chat().id(), "Нічия"));
        } else {
            bot.execute(new SendMessage(message.chat().id(), String.format("@%s програв %s кредитів", originAcc.getUser().getUsername(), BET_PRICE)));
            originAcc.minusCredit(BET_PRICE);
            chatAccountRepository.save(chatAccountMapper.toEntity(originAcc));
            bot.execute(new SendSticker(message.chat().id(), MINUS_STICKER_ID));
            TelegramCasino casino = casinoService.findOrCreateTelegramCasinoByChatID(message.chat().id());
            if (casino.getOwner() != null) {
                TelegramUser casinoOwner = casino.getOwner();
                Integer casinoPart = Double.valueOf(Math.floor(BET_PRICE/2d)).intValue();
                ChatAccount casinoChatOwner = chatAccountRepository.findByUserIdAndChatId(casinoOwner.getId(), originAcc.getChat().getId())
                        .map(chatAccountMapper::toDto)
                        .orElseThrow();
                casinoChatOwner.plusCredit(casinoPart);
                chatAccountRepository.save(chatAccountMapper.toEntity(casinoChatOwner));
                bot.execute(new SendMessage(message.chat().id(),
                        String.format("%s кредитів відходять власнику казино", casinoPart)));
            }
        }
        telegramDiceRequestRepository.save(TelegramDiceRequestEntity.builder()
                .chatId(message.chat().id())
                .date(LocalDateTime.now())
                .user(telegramUserMapper.toEntity(originAcc.getUser()))
                .build());
    }

    @Deprecated
    public void processDice(Message message, UserInfo originUser) {

        if (diceRequestRepository.findAllByUserAndChatIdAndDateGreaterThan(userMapper.toEntity(originUser),
                message.chat().id(),
                LocalDateTime.now().minusHours(3L)).size() >= 3) {
            bot.execute(new SendMessage(message.chat().id(), "Відпочинь лудоман."));
            return;
        }

        if (!banRecordRepository.findByUserAndChatIdAndExpiresAfter(userMapper.toEntity(originUser),
                originUser.getChatId(), LocalDateTime.now()).isEmpty()) {
            bot.execute(new SendMessage(message.chat().id(), "Злочинцям не місце у казино!."));
            return;
        }

        if ((originUser.getSocialCredit() - BET_PRICE) < MIN_BALANCE) {
            bot.execute(new SendMessage(message.chat().id(), "Тобі не місце в цьому казино жебрак."));
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
            CasinoDto casino = casinoService.findOrCreateCasinoByChatID(message.chat().id());
            if (casino.getOwner() != null) {
                UserInfo casinoOwner = casino.getOwner();
                Integer casinoPart = Double.valueOf(Math.floor(BET_PRICE/2d)).intValue();
                casinoOwner.plusCredit(casinoPart);
                userRepository.save(userMapper.toEntity(casinoOwner));
                bot.execute(new SendMessage(message.chat().id(),
                        String.format("%s кредитів відходять власнику казино", casinoPart)));
            }
        }
        diceRequestRepository.save(DiceRequestEntity.builder()
                .chatId(message.chat().id())
                .date(LocalDateTime.now())
                .user(userMapper.toEntity(originUser))
                .build());
    }
}
