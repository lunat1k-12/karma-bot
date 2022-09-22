package com.coolguys.bot.service;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.KarmaUpdateType;
import com.coolguys.bot.dto.TelegramKarmaUpdate;
import com.coolguys.bot.entity.TelegramKarmaUpdateEntity;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.mapper.TelegramKarmaUpdateMapper;
import com.coolguys.bot.mapper.TelegramUserMapper;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.repository.TelegramKarmaUpdateRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KarmaService {

    private final String UNIQ_PLUS_ID = "AgADAgADf3BGHA";
    private final String UNIQ_MINUS_ID = "AgADAwADf3BGHA";

    private final UserService userService;
    private final TelegramKarmaUpdateRepository telegramKarmaUpdateRepository;
    private final TelegramKarmaUpdateMapper telegramKarmaUpdateMapper;
    private final TelegramUserMapper telegramUserMapper;
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;
    private final TelegramBot bot;

    public void processKarmaUpdate(Message message, ChatAccount originAcc) {
        ChatAccount targetAcc = userService.loadChatAccount(message.replyToMessage());

        KarmaUpdateType type = KarmaUpdateType.INCREASE;
        switch (message.sticker().fileUniqueId()) {
            case UNIQ_PLUS_ID:
                targetAcc.plusCredit(20);
                break;
            case UNIQ_MINUS_ID:
                targetAcc.minusCredit(20);
                type = KarmaUpdateType.DECREASE;
                break;
        }

        if (processRecentUpdates(originAcc, targetAcc, type, bot, message)) {
            if (isLikesForLikesChain(message)) {
                bot.execute(new SendMessage(message.chat().id(), "Це не привід карму змінювати\nНезарахованно")
                        .replyToMessageId(message.replyToMessage().messageId()));
                return;
            }
            TelegramKarmaUpdateEntity entity = TelegramKarmaUpdateEntity.builder()
                    .originUserId(originAcc.getUser().getId())
                    .type(type.getId())
                    .targetUser(telegramUserMapper.toEntity(targetAcc.getUser()))
                    .chatId(originAcc.getChat().getId())
                    .date(LocalDateTime.now())
                    .build();

            telegramKarmaUpdateRepository.save(entity);
            chatAccountRepository.save(chatAccountMapper.toEntity(targetAcc));
        }
    }

    private boolean isLikesForLikesChain(Message message) {
        return message.replyToMessage().sticker() != null &&
                List.of(UNIQ_PLUS_ID, UNIQ_MINUS_ID).contains(message.replyToMessage().sticker().fileUniqueId());
    }

    private boolean processRecentUpdates(ChatAccount originAcc, ChatAccount targetAcc,
                                         KarmaUpdateType type, TelegramBot bot,
                                         Message message) {
        List<TelegramKarmaUpdate> updates = telegramKarmaUpdateRepository.findAllByOriginUserIdAndTargetUserAndTypeAndDateGreaterThan(originAcc.getUser().getId(),
                        telegramUserMapper.toEntity(targetAcc.getUser()),
                        type.getId(),
                        LocalDateTime.now().minusMinutes(30)).stream()
                .map(telegramKarmaUpdateMapper::toDto)
                .collect(Collectors.toList());

        if (updates.size() > 0) {
            bot.execute(new SendMessage(originAcc.getChat().getId(), type.getMessageForDuplicate())
                    .replyToMessageId(message.messageId()));
            return false;
        }

        return true;
    }
}
