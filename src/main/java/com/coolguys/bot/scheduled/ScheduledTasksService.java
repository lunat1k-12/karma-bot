package com.coolguys.bot.scheduled;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.TelegramCasino;
import com.coolguys.bot.dto.TelegramGuardDepartment;
import com.coolguys.bot.dto.TelegramPoliceDepartment;
import com.coolguys.bot.dto.TelegramUser;
import com.coolguys.bot.entity.TelegramChatEntity;
import com.coolguys.bot.listener.MessagesListener;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.mapper.TelegramMessageMapper;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.repository.TelegramChatRepository;
import com.coolguys.bot.repository.TelegramMessageRepository;
import com.coolguys.bot.service.CasinoService;
import com.coolguys.bot.service.DrugsService;
import com.coolguys.bot.service.GuardDepartmentService;
import com.coolguys.bot.service.PoliceDepartmentService;
import com.coolguys.bot.service.StealService;
import com.coolguys.bot.service.UserService;
import com.pengrad.telegrambot.model.request.ChatAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static com.coolguys.bot.service.StealService.POLICE_STICKER;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledTasksService {

    private static final Integer PRICE = 200;

    private static final Integer TOP_PRICE = 100;
    private final MessagesListener messagesListener;
    private final DrugsService drugsService;
    private final CasinoService casinoService;
    private final StealService stealService;
    private final UserService userService;
    private final PoliceDepartmentService policeDepartmentService;
    private final GuardDepartmentService guardDepartmentService;
    private final TelegramChatRepository telegramChatRepository;
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;
    private final TelegramMessageRepository telegramMessageRepository;
    private final TelegramMessageMapper telegramMessageMapper;

    private static final String TOP_STICKER = "CAACAgIAAxkBAAIBTGMQ3leswu0305mH8dYR1BByXz_dAAJmAQACPQ3oBOMh-z8iW4cZKQQ";

    private static final String POLICE_CHECK_STICKER = "CAACAgIAAxkBAAIDRWMcw5JmJ-5YvBKHMffkfT67LnelAAJ-AwACbbBCA3EZlrX3Vpb0KQQ";
    private static final Integer DRUGS_FINE = 100;

    @Scheduled(cron = "00 00 10,13,16 * * *")
    @Async
    public void drugRaid() {
        log.info("Initiate drug raid");
        try {
            telegramChatRepository.findAllActive().stream()
                    .peek(ch -> log.info("Search for drugs in {}", ch.getName()))
                    .map(TelegramChatEntity::getId)
                    .forEach(this::chatDrugRaid);

            Thread.sleep(3000);
        } catch (Exception e) {
            log.error("drugRaid - Exception while sleep", e);
        }
    }

    private void chatDrugRaid(Long chatId) {
        try {
            List<ChatAccount> users = userService.findActiveAccByChatId(chatId);

            if (users.isEmpty()) {
                log.info("no users in chat {}", chatId);
                return;
            }

            Random random = new Random();

            int userIndex = random.nextInt(users.size());
            ChatAccount userToCheck = users.get(userIndex);

            messagesListener.sendMessage(chatId, String.format("Поліція вирішила обшукати @%s", userToCheck.getUser().getUsername()));
            messagesListener.sendSticker(chatId, POLICE_CHECK_STICKER);
            messagesListener.sendChatAction(chatId, ChatAction.typing);
            Thread.sleep(2000);

            if (drugsService.findActiveDrugDeals(userToCheck).size() > 0) {
                log.info("Drugs found in {} place", userToCheck.getUser().getUsername());
                messagesListener.sendMessage(chatId, String.format("Поліція знайшла наркотики у @%s", userToCheck.getUser().getUsername()));
                drugsService.discardDrugDeals(userToCheck);

                TelegramCasino casino = casinoService.findOrCreateTelegramCasinoByChatID(chatId);
                TelegramPoliceDepartment pd = policeDepartmentService.findOrCreateTelegramPdByChatID(chatId);
                TelegramGuardDepartment gd = guardDepartmentService.findOrCreateTelegramGdByChatID(chatId);
                if (userToCheck.getSocialCredit() >= DRUGS_FINE) {
                    log.info("{} has enough credits", userToCheck.getUser().getUsername());
                    messagesListener.sendMessage(chatId, String.format("@%s оштрафовано на %s кредитів і відправлено у в`язницю",
                            userToCheck.getUser().getUsername(), DRUGS_FINE));
                    policeDepartmentService.processFine(userToCheck, DRUGS_FINE);
                    stealService.sendToJail(userToCheck);
                    messagesListener.sendSticker(chatId, POLICE_STICKER);
                } else if (casino.getOwner() != null && userToCheck.getUser().getId().equals(casino.getOwner().getId())) {
                    log.info("{} has casino", userToCheck.getUser().getUsername());
                    messagesListener.sendMessage(chatId,
                            String.format("Коштів не вистачає для покриття штрафу.\n" +
                                    "@%s втрачає казино", userToCheck.getUser().getUsername()));
                    policeDepartmentService.processFine(userToCheck, DRUGS_FINE);
                    casinoService.dropCasinoOwner(chatId);
                    messagesListener.sendSticker(chatId, POLICE_STICKER);
                } else if (pd.getOwner() != null && userToCheck.getUser().getId().equals(pd.getOwner().getId())) {
                    log.info("{} has police department", userToCheck.getUser().getUsername());
                    messagesListener.sendMessage(chatId,
                            String.format("Коштів не вистачає для покриття штрафу.\n" +
                                    "@%s втрачає поліцейську ділянку", userToCheck.getUser().getUsername()));
                    policeDepartmentService.dropPdOwner(chatId);
                    policeDepartmentService.processFine(userToCheck, DRUGS_FINE);
                    messagesListener.sendSticker(chatId, POLICE_STICKER);
                } else if (gd.getOwner() != null && userToCheck.getUser().getId().equals(gd.getOwner().getId())) {
                    log.info("{} has guard department", userToCheck.getUser().getUsername());
                    messagesListener.sendMessage(chatId,
                            String.format("Коштів не вистачає для покриття штрафу.\n" +
                                    "@%s втрачає охороне агенство", userToCheck.getUser().getUsername()));
                    guardDepartmentService.dropGuardOwner(chatId);
                    policeDepartmentService.processFine(userToCheck, DRUGS_FINE);
                    messagesListener.sendSticker(chatId, POLICE_STICKER);
                } else {
                    log.info("{} going to jail", userToCheck.getUser().getUsername());
                    messagesListener.sendMessage(chatId,
                            String.format("Коштів не вистачає для покриття штрафу.\n" +
                                    "@%s потрапив у в'язницю", userToCheck.getUser().getUsername()));
                    policeDepartmentService.processFine(userToCheck, DRUGS_FINE);
                    stealService.sendToJail(userToCheck);
                    messagesListener.sendSticker(chatId, POLICE_STICKER);
                }
            } else {
                log.info("nothing found");
                messagesListener.sendMessage(chatId, "Поліція нічого не знайшла");
            }

        } catch (Exception e) {
            log.error("exception in time of chatDrugRaid", e);
        }
    }

    @Scheduled(cron = "00 15 09 * * *")
    @Async
    public void getTopAndWorstUser() {
        try {
            log.info("Initiate Top/Worst");
            telegramChatRepository.findAllActive().stream()
                    .map(TelegramChatEntity::getId)
                    .forEach(this::getTopAndWorstUser);

            Thread.sleep(3000);
        } catch (Exception e) {
            log.error("getTopAndWorstUser - Exception while sleep", e);
        }
    }

    private void getTopAndWorstUser(Long chatId) {
        log.info("getTopAndWorstUser - start for chat: {}", chatId);
        List<ChatAccount> users = userService.findActiveAccByChatId(chatId);

        if (users.isEmpty()) {
            log.info("getTopAndWorstUser - users is empty for chat: {}", chatId);
            return;
        }

        Random random = new Random();

        int topIndex = random.nextInt(users.size());
        int bottomIndex;
        do {
            bottomIndex = random.nextInt(users.size());
        } while (bottomIndex == topIndex && users.size() > 1);

        log.info("getTopAndWorstUser - Top user - {}, chatId: {}", users.get(topIndex).getUser().getUsername(), chatId);
        log.info("getTopAndWorstUser - Bottom user - {}, chatId: {}", users.get(bottomIndex).getUser().getUsername(), chatId);

        messagesListener.sendMessage(chatId, String.format("Шановне Панство, Увага!\nТоп хлопак на сьогодні: @%s\n" +
                "він отримує %s кредитів", users.get(topIndex).getUser().getUsername(), TOP_PRICE));
        messagesListener.sendSticker(chatId, TOP_STICKER);
        messagesListener.sendMessage(chatId,
                String.format("І на самому дні нас сьогодні чекає @%s", users.get(bottomIndex).getUser().getUsername()));
        ChatAccount topUser = users.get(topIndex);
        topUser.plusCredit(TOP_PRICE);
        chatAccountRepository.save(chatAccountMapper.toEntity(topUser));
        log.info("getTopAndWorstUser - done for chat: {}", chatId);
    }

    @Scheduled(cron = "00 00 08 * * MON")
    @Async
    public void processMostActiveUser() {
        try {
            log.info("Initiate Most Active");
            telegramChatRepository.findAllActive().stream()
                    .map(TelegramChatEntity::getId)
                    .forEach(this::processChatMostActiveUser);

            Thread.sleep(3000);
        } catch (Exception e) {
            log.error("processMostActiveUser - Exception while sleep", e);
        }
    }

    private void processChatMostActiveUser(Long chatId) {
        Map<TelegramUser, Integer> messageCount = new HashMap<>();
        telegramMessageRepository.findAllByDateGreaterThanAndChatId(LocalDateTime.now().minusWeeks(7L), chatId)
                .stream()
                .map(telegramMessageMapper::toDto)
                .forEach(u -> messageCount.merge(u.getUser(), 1, Integer::sum));

        TelegramUser topMessagesUser = null;
        int maxValue = 0;
        for (TelegramUser user : messageCount.keySet()) {
            if (messageCount.get(user) > maxValue) {
                maxValue = messageCount.get(user);
                topMessagesUser = user;
            }
        }

        if (topMessagesUser != null) {
            ChatAccount topAcc = chatAccountRepository.findByUserIdAndChatId(topMessagesUser.getId(), chatId)
                    .map(chatAccountMapper::toDto)
                    .orElseThrow();
            topAcc.plusCredit(PRICE);
            chatAccountRepository.save(chatAccountMapper.toEntity(topAcc));
            log.info("Top messages User: {}", topMessagesUser.getUsername());

            String topUserMessage = String.format("Шановне Панство!\nНайактивнішим в чаті за цю неділю був - @%s" +
                    "\n %s кредитів цьому господину", topMessagesUser.getUsername(), PRICE);
            messagesListener.sendMessage(chatId, topUserMessage);
        } else {
            log.info("No activity in the chat: {}", chatId);
        }
    }
}
