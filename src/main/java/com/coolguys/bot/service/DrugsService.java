package com.coolguys.bot.service;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.DrugAction;
import com.coolguys.bot.dto.OrderType;
import com.coolguys.bot.dto.QueryDataDto;
import com.coolguys.bot.dto.ReplyOrderStage;
import com.coolguys.bot.dto.TelegramDrugAction;
import com.coolguys.bot.dto.TelegramOrder;
import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.entity.DrugActionEntity;
import com.coolguys.bot.entity.TelegramDrugActionEntity;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.mapper.DrugActionMapper;
import com.coolguys.bot.mapper.TelegramDrugActionMapper;
import com.coolguys.bot.mapper.TelegramOrderMapper;
import com.coolguys.bot.mapper.TelegramUserMapper;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.repository.DrugActionRepository;
import com.coolguys.bot.repository.TelegramDrugActionRepository;
import com.coolguys.bot.repository.TelegramOrderRepository;
import com.coolguys.bot.repository.UserRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrugsService {

    private final DrugActionRepository drugActionRepository;
    private final DrugActionMapper drugActionMapper;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final StealService stealService;
    private final TelegramBot bot;
    private final KeyboardService keyboardService;
    private final TelegramDrugActionRepository telegramDrugActionRepository;
    private final TelegramDrugActionMapper telegramDrugActionMapper;
    private final TelegramUserMapper telegramUserMapper;
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;
    private final TelegramOrderRepository telegramOrderRepository;
    private final TelegramOrderMapper telegramOrderMapper;

    private static final Integer DRUGS_INCOME = 200;

    private static final Integer DROP_DRUG_PRICE = 50;

    public void dropDrugsRequest(ChatAccount acc) {
        log.info("Drop drug request from: {}", acc.getUser().getUsername());
        if (acc.getSocialCredit() < DROP_DRUG_PRICE) {
            bot.execute(new SendMessage(acc.getChat().getId(), "В тебе недостатньо грошей"));
            return;
        }

        if (stealService.isInJail(acc)) {
            bot.execute(new SendMessage(acc.getChat().getId(), "Спочатку відсиди своє!"));
            log.info("User is in jail");
            return;
        }

        TelegramOrder order = telegramOrderRepository.findLastDrugDrop(acc.getChat().getId(), acc.getId())
                .map(telegramOrderMapper::toDto)
                .orElse(null);

        if (order != null) {
            if (ReplyOrderStage.TARGET_REQUIRED.equals(order.getStage())) {
                bot.execute(new SendMessage(acc.getChat().getId(), "Ти вже створив замовлення\nОбери кому хочеш підкинути наркотики:")
                        .replyMarkup(keyboardService.getTargetAccSelectionPersonKeyboard(acc.getChat().getId(), acc.getId(), QueryDataDto.DROP_DRUGS_TYPE)));
                return;
            } else if (!findActiveDrugDeals(order.getTargetAcc()).isEmpty()) {
                bot.execute(new SendMessage(acc.getChat().getId(), "Не зараз.\nОстання справа ще закінчилась"));
                return;
            }
        }

        TelegramOrder newOrder = TelegramOrder.builder()
                .chatId(acc.getChat().getId())
                .originAccId(acc.getId())
                .type(OrderType.DROP_DRUGS)
                .stage(ReplyOrderStage.TARGET_REQUIRED)
                .build();

        telegramOrderRepository.save(telegramOrderMapper.toEntity(newOrder));
        acc.minusCredit(DROP_DRUG_PRICE);
        chatAccountRepository.save(chatAccountMapper.toEntity(acc));

        bot.execute(new SendMessage(acc.getChat().getId(), "Обери кому хочеш підкинути наркотики:")
                .replyMarkup(keyboardService.getTargetAccSelectionPersonKeyboard(acc.getChat().getId(), acc.getId(), QueryDataDto.DROP_DRUGS_TYPE)));
        log.info("Drop drug request created");
    }

    public void processDropDrug(ChatAccount originAcc, QueryDataDto query) {
        TelegramOrder order = telegramOrderRepository.findAllByChatIdAndStageAndOriginAccIdAndType(originAcc.getChat().getId(),
                        ReplyOrderStage.TARGET_REQUIRED.getId(),
                        originAcc.getId(),
                        OrderType.DROP_DRUGS.getId()).stream()
                .findFirst()
                .map(telegramOrderMapper::toDto)
                .orElse(null);

        if (order == null) {
            return;
        }

        order.setStage(ReplyOrderStage.DONE);

        if ("0".equals(query.getOption())) {
            telegramOrderRepository.deleteById(order.getId());
            originAcc.plusCredit(DROP_DRUG_PRICE);
            chatAccountRepository.save(chatAccountMapper.toEntity(originAcc));
            bot.execute(new SendMessage(originAcc.getChat().getId(), "Зловмисник одумався"));
            return;
        }

        ChatAccount targetAcc = chatAccountRepository.findById(Long.parseLong(query.getOption()))
                .map(chatAccountMapper::toDto)
                .orElseThrow();
        order.setTargetAcc(targetAcc);
        telegramOrderRepository.save(telegramOrderMapper.toEntity(order));

        telegramDrugActionRepository.save(TelegramDrugActionEntity.builder()
                .user(telegramUserMapper.toEntity(targetAcc.getUser()))
                .expires(LocalDateTime.now().plusHours(24))
                .chatId(targetAcc.getChat().getId())
                .build());

        bot.execute(new SendMessage(originAcc.getChat().getId(), "Наркотики підкинуто"));
    }

    public void doDrugs(ChatAccount user) {
        if (findActiveDrugDeals(user).size() > 0) {
            bot.execute(new SendMessage(user.getChat().getId(), "Тобі треба залягти на дно"));
            return;
        }

        if (stealService.isInJail(user)) {
            bot.execute(new SendMessage(user.getChat().getId(), "Відсиди спочатку"));
            return;
        }

        user.plusCredit(DRUGS_INCOME);
        chatAccountRepository.save(chatAccountMapper.toEntity(user));

        telegramDrugActionRepository.save(TelegramDrugActionEntity.builder()
                .user(telegramUserMapper.toEntity(user.getUser()))
                .expires(LocalDateTime.now().plusHours(24))
                .chatId(user.getChat().getId())
                .build());

        bot.execute(new SendMessage(user.getChat().getId(),
                String.format("@%s підняв грошей на наркоті!", user.getUser().getUsername())));
    }

    @Deprecated
    public void doDrugs(UserInfo user) {
        if (findActiveDrugDeals(user).size() > 0) {
            bot.execute(new SendMessage(user.getChatId(), "Тобі треба залягти на дно"));
            return;
        }

        if (stealService.isInJail(user)) {
            bot.execute(new SendMessage(user.getChatId(), "Відсиди спочатку"));
            return;
        }

        user.plusCredit(DRUGS_INCOME);
        userRepository.save(userMapper.toEntity(user));
        drugActionRepository.save(DrugActionEntity.builder()
                .user(userMapper.toEntity(user))
                .expires(LocalDateTime.now().plusHours(24))
                .chatId(user.getChatId())
                .build());

        bot.execute(new SendMessage(user.getChatId(), String.format("@%s підняв грошей на наркоті!", user.getUsername())));
    }

    public List<TelegramDrugAction> findActiveDrugDeals(ChatAccount acc) {
        return telegramDrugActionRepository.findByUserAndChatIdAndExpiresAfter(
                        telegramUserMapper.toEntity(acc.getUser()),
                        acc.getChat().getId(),
                        LocalDateTime.now()
                ).stream()
                .map(telegramDrugActionMapper::toDto)
                .collect(Collectors.toList());
    }

    @Deprecated
    public List<DrugAction> findActiveDrugDeals(UserInfo user) {
        return drugActionRepository.findByUserAndChatIdAndExpiresAfter(
                        userMapper.toEntity(user),
                        user.getChatId(),
                        LocalDateTime.now()
                ).stream()
                .map(drugActionMapper::toDto)
                .collect(Collectors.toList());
    }

    public void discardDrugDeals(ChatAccount user) {
        telegramDrugActionRepository.findByUserAndChatIdAndExpiresAfter(
                        telegramUserMapper.toEntity(user.getUser()),
                        user.getChat().getId(),
                        LocalDateTime.now()
                ).stream()
                .map(telegramDrugActionMapper::toDto)
                .peek(d -> d.setExpires(LocalDateTime.now()))
                .map(telegramDrugActionMapper::toEntity)
                .forEach(telegramDrugActionRepository::save);
    }

    public void discardDrugDeals(UserInfo user) {
        drugActionRepository.findByUserAndChatIdAndExpiresAfter(
                        userMapper.toEntity(user),
                        user.getChatId(),
                        LocalDateTime.now()
                ).stream()
                .map(drugActionMapper::toDto)
                .peek(d -> d.setExpires(LocalDateTime.now()))
                .map(drugActionMapper::toEntity)
                .forEach(drugActionRepository::save);
    }
}
