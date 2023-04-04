package com.coolguys.bot.service;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.OrderType;
import com.coolguys.bot.dto.QueryDataDto;
import com.coolguys.bot.dto.ReplyOrderStage;
import com.coolguys.bot.dto.TelegramCasino;
import com.coolguys.bot.dto.TelegramGuardDepartment;
import com.coolguys.bot.dto.TelegramOrder;
import com.coolguys.bot.dto.TelegramPoliceDepartment;
import com.coolguys.bot.dto.UserStatus;
import com.coolguys.bot.entity.ChatAccountEntity;
import com.coolguys.bot.entity.TelegramBanRecordEntity;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.mapper.TelegramOrderMapper;
import com.coolguys.bot.mapper.TelegramUserMapper;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.repository.TelegramBanRecordRepository;
import com.coolguys.bot.repository.TelegramOrderRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ChatAction;
import com.pengrad.telegrambot.request.SendChatAction;
import com.pengrad.telegrambot.request.SendDice;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendSticker;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class StealService {
    private final KeyboardService keyboardService;
    private final TelegramOrderRepository telegramOrderRepository;
    private final TelegramOrderMapper telegramOrderMapper;
    private final GuardService guardService;
    private final CasinoService casinoService;
    private final TelegramBot bot;
    private final PoliceDepartmentService policeDepartmentService;
    private final GuardDepartmentService guardDepartmentService;
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;
    private final TelegramBanRecordRepository telegramBanRecordRepository;
    private final TelegramUserMapper telegramUserMapper;
    public static final int PAUSE_MILLIS = 3000;
    public static final int STEAL_BORDER = 800;

    private static final int JAIL_TIME = 6;

    public static final String POLICE_STICKER = "CAACAgIAAxkBAAICjmMWTBExj7-WpA_pWEKOaWmaaK71AALkBwACRvusBOq-PekdJ3n1KQQ";
    public static final int FEE = 100;

    public void stealRequest(ChatAccount originAcc) {
        log.info("New steal request from {}", originAcc.getUser().getUsername());

        if (creditsSum(originAcc.getChat().getId()) < STEAL_BORDER) {
            bot.execute(new SendMessage(originAcc.getChat().getId(), String.format("Крадіжки будуть дозволені коли" +
                    " сумарний банк буде вище ніж %s кредитів", STEAL_BORDER)));
            log.info("Credits sum is not enough for stealing");
            return;
        }

        if (isInJail(originAcc)) {
            bot.execute(new SendMessage(originAcc.getChat().getId(), "Куди ти лізеш ворюга, тебе вже за руку спіймали!"));
            log.info("User is in jail");
            return;
        }

        TelegramOrder order = telegramOrderRepository.findAllByChatIdAndStageAndOriginAccIdAndType(originAcc.getChat().getId(),
                        ReplyOrderStage.TARGET_REQUIRED.getId(),
                        originAcc.getId(),
                        OrderType.STEAL.getId()).stream()
                .findFirst()
                .map(telegramOrderMapper::toDto)
                .orElse(null);

        if (order != null) {
            bot.execute(new SendMessage(originAcc.getChat().getId(), "ти вже створив замовлення\nОбери кого хочеш обокрасти:")
                    .replyMarkup(keyboardService.getTargetAccSelectionPersonKeyboard(originAcc.getChat().getId(), originAcc.getId(), QueryDataDto.STEAL_TYPE)));
            return;
        }

        TelegramOrder newOrder = TelegramOrder.builder()
                .chatId(originAcc.getChat().getId())
                .originAccId(originAcc.getId())
                .type(OrderType.STEAL)
                .stage(ReplyOrderStage.TARGET_REQUIRED)
                .build();

        telegramOrderRepository.save(telegramOrderMapper.toEntity(newOrder));

        bot.execute(new SendMessage(originAcc.getChat().getId(), "Обери кого хочеш обокрасти:")
                .replyMarkup(keyboardService.getTargetAccSelectionPersonKeyboard(originAcc.getChat().getId(), originAcc.getId(), QueryDataDto.STEAL_TYPE)));
        log.info("Steal request created");
    }

    public void processSteal(ChatAccount originAcc, QueryDataDto query, Integer messageId) {
        TelegramOrder order = telegramOrderRepository.findAllByChatIdAndStageAndOriginAccIdAndType(originAcc.getChat().getId(),
                        ReplyOrderStage.TARGET_REQUIRED.getId(),
                        originAcc.getId(),
                        OrderType.STEAL.getId()).stream()
                .findFirst()
                .map(telegramOrderMapper::toDto)
                .orElse(null);

        if (order == null) {
            return;
        }

        order.setStage(ReplyOrderStage.DONE);
        telegramOrderRepository.save(telegramOrderMapper.toEntity(order));
        try {
            if ("0".equals(query.getOption())) {
                telegramOrderRepository.deleteById(order.getId());
                bot.execute(new SendMessage(originAcc.getChat().getId(), "Крадій одумався"));
                keyboardService.deleteOrUpdateKeyboardMessage(originAcc.getChat().getId(), messageId);
                return;
            }
            ChatAccount targetAcc = chatAccountRepository.findById(Long.parseLong(query.getOption()))
                    .map(chatAccountMapper::toDto)
                    .orElseThrow();

            if (targetAcc.getSocialCredit() <= 0) {
                bot.execute(new SendMessage(originAcc.getChat().getId(), String.format("Що ти хотів вкрасти у @%s? він бесхатько!",
                        targetAcc.getUser().getUsername())));
                telegramOrderRepository.deleteById(order.getId());
                keyboardService.deleteOrUpdateKeyboardMessage(originAcc.getChat().getId(), messageId);
                return;
            }

            keyboardService.deleteOrUpdateKeyboardMessage(originAcc.getChat().getId(), messageId);
            bot.execute(new SendMessage(originAcc.getChat().getId(),
                    String.format("Невідомий намагається вкрасти у @%s", targetAcc.getUser().getUsername())));
            bot.execute(new SendChatAction(originAcc.getChat().getId(), ChatAction.typing));


            Thread.sleep(PAUSE_MILLIS);
            Random random = new Random();
            int difficulty = random.nextInt(3) + 1;

            boolean hasGuard = guardService.doesHaveGuard(targetAcc);
            if (hasGuard) {
                bot.execute(new SendMessage(originAcc.getChat().getId(), String.format("@%s має охорону!",
                        targetAcc.getUser().getUsername())));
                difficulty = 5;
            }

            bot.execute(new SendMessage(originAcc.getChat().getId(), String.format("Складність крадіжки: %s", difficulty)));
            bot.execute(new SendChatAction(originAcc.getChat().getId(), ChatAction.choose_sticker));
            Thread.sleep(PAUSE_MILLIS);

            SendResponse response = bot.execute(new SendDice(originAcc.getChat().getId())
                    .emoji("\uD83C\uDFB2"));
            bot.execute(new SendChatAction(originAcc.getChat().getId(), ChatAction.typing));
            Thread.sleep(PAUSE_MILLIS);
            if (response.message().dice().value() >= difficulty) {
                int half = Double.valueOf(Math.floor(targetAcc.getSocialCredit() / 2d)).intValue();
                int price;

                if (hasGuard) {
                    do {
                        price = random.nextInt(targetAcc.getSocialCredit());
                    } while (price < half);
                } else {
                    price = random.nextInt(targetAcc.getSocialCredit());
                }

                bot.execute(new SendMessage(originAcc.getChat().getId(), String.format("Невідомий крадій успішно вкрав %s кредитів у @%s",
                        price, targetAcc.getUser().getUsername())));
                targetAcc.minusCredit(price);
                originAcc.plusCredit(price);
                chatAccountRepository.save(chatAccountMapper.toEntity(targetAcc));
                chatAccountRepository.save(chatAccountMapper.toEntity(originAcc));
            } else {
                bot.execute(new SendMessage(originAcc.getChat().getId(), String.format("Невдача! @%s спіймали за руку. Штраф %s і " +
                                "заборона на доступ до казино на 6 годин! Якщо в тебе була охорона, то її більше нема.",
                        originAcc.getUser().getUsername(), FEE)));
                bot.execute(new SendSticker(originAcc.getChat().getId(), POLICE_STICKER));
                busted(originAcc);
            }

        } catch (InterruptedException e) {
            log.error("Error while steal", e);
        }
    }

    public boolean isInJail(ChatAccount acc) {
        return !telegramBanRecordRepository.findByUserAndChatIdAndExpiresAfter(telegramUserMapper.toEntity(acc.getUser()),
                acc.getChat().getId(), LocalDateTime.now()).isEmpty();
    }

    public String getJailTillLabel(ChatAccount acc) {
        return telegramBanRecordRepository.findByUserAndChatIdAndExpiresAfter(telegramUserMapper.toEntity(acc.getUser()),
                acc.getChat().getId(), LocalDateTime.now()).stream()
                .max(Comparator.comparing(TelegramBanRecordEntity::getExpires))
                .map(TelegramBanRecordEntity::getExpires)
                .map(DateConverter::localDateTimeToStringLabel)
                .orElse(null);
    }

    private void busted(ChatAccount originAcc) {
        TelegramCasino casino = casinoService.findOrCreateTelegramCasinoByChatID(originAcc.getChat().getId());
        TelegramPoliceDepartment pd = policeDepartmentService.findOrCreateTelegramPdByChatID(originAcc.getChat().getId());
        TelegramGuardDepartment gd = guardDepartmentService.findOrCreateTelegramGdByChatID(originAcc.getChat().getId());

        if (originAcc.getSocialCredit() < FEE) {
            if (casino.getOwner() != null && originAcc.getUser().getId().equals(casino.getOwner().getId())) {
                casinoService.dropCasinoOwner(originAcc.getChat().getId());
                bot.execute(new SendMessage(originAcc.getChat().getId(),
                        String.format("@%s ти більше не власник казино", originAcc.getUser().getUsername())));
                log.info("Casino owner removed");
            }
            if (pd.getOwner() != null && originAcc.getUser().getId().equals(pd.getOwner().getId())) {
                policeDepartmentService.dropPdOwner(originAcc.getChat().getId());
                bot.execute(new SendMessage(originAcc.getChat().getId(),
                        String.format("@%s ти більше не власник поліцейської ділянки", originAcc.getUser().getUsername())));
                log.info("PD owner removed");
            }
            if (gd.getOwner() != null && originAcc.getUser().getId().equals(gd.getOwner().getId())) {
                guardDepartmentService.dropGuardOwner(originAcc.getChat().getId());
                bot.execute(new SendMessage(originAcc.getChat().getId(),
                        String.format("@%s ти більше не власник охороного агенства", originAcc.getUser().getUsername())));
                log.info("GD owner removed");
            }
        }

        policeDepartmentService.processFine(originAcc, FEE);
        guardService.deleteGuard(originAcc);
        telegramBanRecordRepository.save(TelegramBanRecordEntity.builder()
                .user(telegramUserMapper.toEntity(originAcc.getUser()))
                .expires(LocalDateTime.now().plusHours(JAIL_TIME))
                .chatId(originAcc.getChat().getId())
                .build());
    }

    public void sendToJail(ChatAccount originAcc) {
        guardService.deleteGuard(originAcc);
        telegramBanRecordRepository.save(TelegramBanRecordEntity.builder()
                .user(telegramUserMapper.toEntity(originAcc.getUser()))
                .expires(LocalDateTime.now().plusHours(JAIL_TIME))
                .chatId(originAcc.getChat().getId())
                .build());
    }

    public int creditsSum(Long chatId) {
        return chatAccountRepository.findByChatId(chatId).stream()
                .filter(acc -> UserStatus.ACTIVE.getId().equals(acc.getStatus()))
                .mapToInt(ChatAccountEntity::getSocialCredit).sum();
    }
}
