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
import java.util.Map;
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

    @Scheduled(cron = "00 00 13 * * MON")
    public void processMostActiveUser() {
        Set<Long> chatIds = StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .map(UserEntity::getChatId)
                .collect(Collectors.toSet());

        chatIds.forEach(this::processChatMostActiveUser);
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
