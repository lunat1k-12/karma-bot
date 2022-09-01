package com.coolguys.bot.service;

import com.coolguys.bot.dto.KarmaUpdate;
import com.coolguys.bot.dto.KarmaUpdateType;
import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.entity.KarmaUpdateEntity;
import com.coolguys.bot.mapper.KarmaUpdateMapper;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.KarmaUpdateRepository;
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
    private final KarmaUpdateRepository karmaUpdateRepository;
    private final UserMapper userMapper;
    private final KarmaUpdateMapper karmaUpdateMapper;

    public void processKarmaUpdate(Message message, UserInfo originUser, TelegramBot bot) {
        UserInfo targetUser = userService.loadUser(message.replyToMessage());

        KarmaUpdateType type = KarmaUpdateType.INCREASE;
        switch (message.sticker().fileUniqueId()) {
            case UNIQ_PLUS_ID:
                targetUser.plusCredit(20);
                type = KarmaUpdateType.INCREASE;
                break;
            case UNIQ_MINUS_ID:
                targetUser.minusCredit(20);
                type = KarmaUpdateType.DECREASE;
                break;
        }

        if (processRecentUpdates(originUser, targetUser, type, bot, message)) {
            if (isLikesForLikesChain(message)) {
                bot.execute(new SendMessage(message.chat().id(), "Це не привід карму змінювати\nНезарахованно")
                        .replyToMessageId(message.replyToMessage().messageId()));
                return;
            }
            KarmaUpdateEntity entity = KarmaUpdateEntity.builder()
                    .originUserId(originUser.getId())
                    .type(type.getId())
                    .targetUser(userMapper.toEntity(targetUser))
                    .chatId(message.chat().id())
                    .date(LocalDateTime.now())
                    .build();
            karmaUpdateRepository.save(entity);
            userService.save(targetUser);
        }
    }
    private boolean isLikesForLikesChain(Message message) {
        return message.replyToMessage().sticker() != null &&
                List.of(UNIQ_PLUS_ID, UNIQ_MINUS_ID).contains(message.replyToMessage().sticker().fileUniqueId());
    }
    private boolean processRecentUpdates(UserInfo originUser, UserInfo targetUser,
                                         KarmaUpdateType type, TelegramBot bot,
                                         Message message) {
        List<KarmaUpdate> updates = karmaUpdateRepository.findAllByOriginUserIdAndTargetUserAndTypeAndDateGreaterThan(originUser.getId(),
                userMapper.toEntity(targetUser),
                type.getId(),
                LocalDateTime.now().minusMinutes(30)).stream()
                .map(karmaUpdateMapper::toDto)
                .collect(Collectors.toList());

        if (updates.size() > 0) {
            bot.execute(new SendMessage(originUser.getChatId(), type.getMessageForDuplicate())
                    .replyToMessageId(message.messageId()));
            return false;
        }

        return true;
    }
}
