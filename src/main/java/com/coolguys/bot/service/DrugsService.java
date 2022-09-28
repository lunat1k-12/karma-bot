package com.coolguys.bot.service;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.OrderType;
import com.coolguys.bot.dto.QueryDataDto;
import com.coolguys.bot.dto.ReplyOrderStage;
import com.coolguys.bot.dto.TelegramDrugAction;
import com.coolguys.bot.dto.TelegramOrder;
import com.coolguys.bot.entity.TelegramDrugActionEntity;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.mapper.TelegramDrugActionMapper;
import com.coolguys.bot.mapper.TelegramOrderMapper;
import com.coolguys.bot.mapper.TelegramUserMapper;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.repository.TelegramDrugActionRepository;
import com.coolguys.bot.repository.TelegramOrderRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DrugsService {
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

    private static final Integer DRUGS_INCOME = 400;

    public void dropDrugsRequest(ChatAccount acc) {
        log.info("Drop drug request from: {}", acc.getUser().getUsername());

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
            } else if (order.getDrugAction() != null && order.getDrugAction().getExpires().isAfter(LocalDateTime.now())) {
                long diff = ChronoUnit.MINUTES.between(LocalDateTime.now(), order.getDrugAction().getExpires());
                double hoursDiff = Math.floor(diff / 60d);

                bot.execute(new SendMessage(acc.getChat().getId(),
                        String.format("Не зараз.\nОстання справа ще закінчилась\nСпробуй через %s годин(и) та %s хвилин(и)",
                                hoursDiff,
                                diff - (hoursDiff * 60))));
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

        bot.execute(new SendMessage(acc.getChat().getId(), "Обери кому хочеш підкинути наркотики:")
                .replyMarkup(keyboardService.getTargetAccSelectionPersonKeyboard(acc.getChat().getId(), acc.getId(), QueryDataDto.DROP_DRUGS_TYPE)));
        log.info("Drop drug request created");
    }

    @Transactional
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
            bot.execute(new SendMessage(originAcc.getChat().getId(), "Зловмисник одумався"));
            return;
        }

        ChatAccount targetAcc = chatAccountRepository.findById(Long.parseLong(query.getOption()))
                .map(chatAccountMapper::toDto)
                .orElseThrow();
        order.setTargetAcc(targetAcc);

        var drugAction = telegramDrugActionRepository.save(TelegramDrugActionEntity.builder()
                .user(telegramUserMapper.toEntity(targetAcc.getUser()))
                .expires(LocalDateTime.now().plusHours(24))
                .chatId(targetAcc.getChat().getId())
                .build());

        order.setDrugAction(telegramDrugActionMapper.toDto(drugAction));
        telegramOrderRepository.save(telegramOrderMapper.toEntity(order));

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

    public List<TelegramDrugAction> findActiveDrugDeals(ChatAccount acc) {
        return telegramDrugActionRepository.findByUserAndChatIdAndExpiresAfter(
                        telegramUserMapper.toEntity(acc.getUser()),
                        acc.getChat().getId(),
                        LocalDateTime.now()
                ).stream()
                .map(telegramDrugActionMapper::toDto)
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
}
