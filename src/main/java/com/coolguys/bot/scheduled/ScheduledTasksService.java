package com.coolguys.bot.scheduled;

import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.entity.UserEntity;
import com.coolguys.bot.listner.MessagesListener;
import com.coolguys.bot.mapper.ChatMessageMapper;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.ChatMessageRepository;
import com.coolguys.bot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasksService {

    private static final Integer PRICE = 100;

    private final ChatMessageRepository chatMessageRepository;

    private final UserRepository userRepository;

    private final ChatMessageMapper chatMessageMapper;

    private final MessagesListener messagesListener;

    private final UserMapper userMapper;

    private static final String TOP_STICKER = "CAACAgIAAxkBAAIBTGMQ3leswu0305mH8dYR1BByXz_dAAJmAQACPQ3oBOMh-z8iW4cZKQQ";

    private static final String BOTTOM_STICKER = "CAACAgIAAxkBAAIBTWMQ3suJoK8YxnByTPusiWNyxAsyAAJ_EAAC-VZgS5YaUypWFf_HKQQ";

    @Scheduled(cron = "00 00 07 * * *")
    public void getTopAndWorstUser() {
        StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .map(UserEntity::getChatId)
                .collect(Collectors.toSet())
                .forEach(this::getTopAndWorstUser);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            log.error("getTopAndWorstUser - Exception while sleep");
        }
    }

    private void getTopAndWorstUser(Long chatId) {
        List<UserInfo> users = userRepository.findByChatId(chatId).stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());

        Random random = new Random();

        int topIndex = random.nextInt(users.size());
        int bottomIndex = -1;
        do {
            bottomIndex = random.nextInt(users.size());
        } while (bottomIndex == topIndex);

        log.info("Top user - {}, chatId: {}", users.get(topIndex).getUsername(), chatId);
        log.info("Bottom user - {}, chatId: {}", users.get(bottomIndex).getUsername(), chatId);

        messagesListener.sendMessage(chatId, String.format("Шановне Панство, Увага!\nТоп хлопак на сьогодні: @%s", users.get(topIndex).getUsername()));
        messagesListener.sendSticker(chatId, TOP_STICKER);
        messagesListener.sendMessage(chatId, String.format("І на самому дні нас сьогодні чекає @%s", users.get(bottomIndex).getUsername()));
        messagesListener.sendSticker(chatId, BOTTOM_STICKER);
    }

    @Scheduled(cron = "00 00 08 * * MON")
    public void processMostActiveUser() {
        Set<Long> chatIds = StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .map(UserEntity::getChatId)
                .collect(Collectors.toSet());

        chatIds.forEach(this::processChatMostActiveUser);

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            log.error("processMostActiveUser - Exception while sleep");
        }
    }

    private void processChatMostActiveUser(Long chatId) {
        Map<UserInfo, Integer> messageCount = new HashMap<>();
        chatMessageRepository.findAllByDateGreaterThanAndChatId(LocalDateTime.now().minusWeeks(7L), chatId)
                .stream()
                .map(chatMessageMapper::toDto)
                .forEach(u -> messageCount.merge(u.getUser(), 1, Integer::sum));

        UserInfo topMessagesUser = null;
        int maxValue = 0;
        for (UserInfo userInfo : messageCount.keySet()) {
            if (messageCount.get(userInfo) > maxValue) {
                maxValue = messageCount.get(userInfo);
                topMessagesUser = userInfo;
            }
        }

        if (topMessagesUser != null) {
            topMessagesUser.setSocialCredit(topMessagesUser.getSocialCredit() + PRICE);
            userRepository.save(userMapper.toEntity(topMessagesUser));
            log.info("Top messages User: {}", topMessagesUser.getUsername());

            String topUserMessage = String.format("Шановне Панство!\nНайактивнішим в чаті за цю неділю був - %s" +
                    "\n %s кредитів цьому господину", topMessagesUser.getUsername(), PRICE);
            messagesListener.sendMessage(chatId, topUserMessage);
        } else {
            log.info("No activity in the chat: {}", chatId);
        }
    }
}
