package com.coolguys.bot.scheduled;

import com.coolguys.bot.dto.CasinoDto;
import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.entity.UserEntity;
import com.coolguys.bot.listener.MessagesListener;
import com.coolguys.bot.mapper.ChatMessageMapper;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.ChatMessageRepository;
import com.coolguys.bot.repository.UserRepository;
import com.coolguys.bot.service.CasinoService;
import com.coolguys.bot.service.DrugsService;
import com.coolguys.bot.service.StealService;
import com.coolguys.bot.service.UserService;
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

import static com.coolguys.bot.service.StealService.POLICE_STICKER;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasksService {

    private static final Integer PRICE = 200;

    private static final Integer TOP_PRICE = 100;

    private final ChatMessageRepository chatMessageRepository;

    private final UserRepository userRepository;

    private final ChatMessageMapper chatMessageMapper;

    private final MessagesListener messagesListener;

    private final UserMapper userMapper;
    private final DrugsService drugsService;
    private final CasinoService casinoService;
    private final StealService stealService;
    private final UserService userService;

    private static final String TOP_STICKER = "CAACAgIAAxkBAAIBTGMQ3leswu0305mH8dYR1BByXz_dAAJmAQACPQ3oBOMh-z8iW4cZKQQ";

    private static final String BOTTOM_STICKER = "CAACAgIAAxkBAAIBTWMQ3suJoK8YxnByTPusiWNyxAsyAAJ_EAAC-VZgS5YaUypWFf_HKQQ";

    private static final String POLICE_CHECK_STICKER = "CAACAgIAAxkBAAIDRWMcw5JmJ-5YvBKHMffkfT67LnelAAJ-AwACbbBCA3EZlrX3Vpb0KQQ";
    private static final Integer DRUGS_FINE = 300;

    @Scheduled(cron = "00 00 10 * * *")
    public void drugRaid() {
        StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .map(UserEntity::getChatId)
                .collect(Collectors.toSet())
                .forEach(this::chatDrugRaid);
    }

    private void chatDrugRaid(Long chatId) {
        log.info("Search for drugs in {}", chatId);
        List<UserInfo> users = userService.findActiveByChatId(chatId);

        Random random = new Random();

        int userIndex = random.nextInt(users.size());
        UserInfo userToCheck = users.get(userIndex);

        messagesListener.sendMessage(chatId, String.format("Поліція вирішила обшукати @%s", userToCheck.getUsername()));
        messagesListener.sendSticker(chatId, POLICE_CHECK_STICKER);

        try {
            Thread.sleep(1000);

            if (drugsService.findActiveDrugDeals(userToCheck).size() > 0) {
                log.info("Drugs found in {} place", userToCheck.getUsername());
                messagesListener.sendMessage(chatId, String.format("Поліція знайшла наркотики у @%s", userToCheck.getUsername()));
                drugsService.discardDrugDeals(userToCheck);

                CasinoDto casino = casinoService.findOrCreateCasinoByChatID(chatId);
                if (userToCheck.getSocialCredit() >= DRUGS_FINE) {
                    userToCheck.minusCredit(DRUGS_FINE);
                    userRepository.save(userMapper.toEntity(userToCheck));
                    messagesListener.sendMessage(chatId, String.format("@%s оштрафовано на %s кредитів",
                            userToCheck.getUsername(), DRUGS_FINE));
                    messagesListener.sendSticker(chatId, POLICE_STICKER);
                } else if (casino.getOwner() != null && userToCheck.getId().equals(casino.getOwner().getId())) {
                    messagesListener.sendMessage(chatId,
                            String.format("Коштів не вистачає для покриття штрафу.\n" +
                                    "@%s втрачає казино", userToCheck.getUsername()));
                    userToCheck.minusCredit(DRUGS_FINE);
                    userRepository.save(userMapper.toEntity(userToCheck));
                    casinoService.dropCasinoOwner(chatId);
                    messagesListener.sendSticker(chatId, POLICE_STICKER);
                } else {
                    messagesListener.sendMessage(chatId,
                            String.format("Коштів не вистачає для покриття штрафу.\n" +
                                    "@%s потрапив у в'язницю", userToCheck.getUsername()));
                    userToCheck.minusCredit(DRUGS_FINE);
                    userRepository.save(userMapper.toEntity(userToCheck));
                    stealService.sendToJail(userToCheck);
                    messagesListener.sendSticker(chatId, POLICE_STICKER);
                }
            } else {
                messagesListener.sendMessage(chatId, "Поліція нічого не знайшла");
            }

        } catch (InterruptedException e) {
            log.error("chatDrugRaid - Exception while sleep");
        }
    }

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
        List<UserInfo> users = userService.findActiveByChatId(chatId);

        Random random = new Random();

        int topIndex = random.nextInt(users.size());
        int bottomIndex;
        do {
            bottomIndex = random.nextInt(users.size());
        } while (bottomIndex == topIndex);

        log.info("Top user - {}, chatId: {}", users.get(topIndex).getUsername(), chatId);
        log.info("Bottom user - {}, chatId: {}", users.get(bottomIndex).getUsername(), chatId);

        messagesListener.sendMessage(chatId, String.format("Шановне Панство, Увага!\nТоп хлопак на сьогодні: @%s\n" +
                "він отримує %s кредитів", users.get(topIndex).getUsername(), TOP_PRICE));
        messagesListener.sendSticker(chatId, TOP_STICKER);
        messagesListener.sendMessage(chatId, String.format("І на самому дні нас сьогодні чекає @%s", users.get(bottomIndex).getUsername()));
        messagesListener.sendSticker(chatId, BOTTOM_STICKER);
        UserInfo topUser = users.get(topIndex);
        topUser.plusCredit(TOP_PRICE);
        userRepository.save(userMapper.toEntity(topUser));
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
