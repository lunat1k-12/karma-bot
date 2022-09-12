package com.coolguys.bot.service;

import com.coolguys.bot.dto.DrugAction;
import com.coolguys.bot.dto.Order;
import com.coolguys.bot.dto.OrderType;
import com.coolguys.bot.dto.QueryDataDto;
import com.coolguys.bot.dto.ReplyOrderStage;
import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.entity.DrugActionEntity;
import com.coolguys.bot.mapper.DrugActionMapper;
import com.coolguys.bot.mapper.OrderMapper;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.DrugActionRepository;
import com.coolguys.bot.repository.OrderRepository;
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
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final KeyboardService keyboardService;

    private static final Integer DRUGS_INCOME = 200;

    private static final Integer DROP_DRUG_PRICE = 50;

    public void dropDrugsRequest(UserInfo user) {
        log.info("Drop drug request from: {}", user.getUsername());
        if (user.getSocialCredit() < DROP_DRUG_PRICE) {
            bot.execute(new SendMessage(user.getChatId(), "В тебе недостатньо грошей"));
            return;
        }

        if (stealService.isInJail(user)) {
            bot.execute(new SendMessage(user.getChatId(), "Спочатку відсиди своє!"));
            log.info("User is in jail");
            return;
        }

        Order order = orderRepository.findLastDrugDrop(user.getChatId(), user.getId())
                .map(orderMapper::toDto)
                .orElse(null);

        if (order != null) {
            if (ReplyOrderStage.TARGET_REQUIRED.equals(order.getStage())) {
                bot.execute(new SendMessage(user.getChatId(), "Ти вже створив замовлення\nОбери кому хочеш підкинути наркотики:")
                        .replyMarkup(keyboardService.getTargetSelectionPersonKeyboard(user.getChatId(), user.getId(), QueryDataDto.DROP_DRUGS_TYPE)));
                return;
            } else if (!findActiveDrugDeals(order.getTargetUser()).isEmpty()) {
                bot.execute(new SendMessage(user.getChatId(), "Не зараз.\nОстання справа ще закінчилась"));
                return;
            }
        }
        Order newOrder = Order.builder()
                .chatId(user.getChatId())
                .originUserId(user.getId())
                .type(OrderType.DROP_DRUGS)
                .stage(ReplyOrderStage.TARGET_REQUIRED)
                .build();

        orderRepository.save(orderMapper.toEntity(newOrder));
        user.minusCredit(DROP_DRUG_PRICE);
        userRepository.save(userMapper.toEntity(user));

        bot.execute(new SendMessage(user.getChatId(), "Обери кому хочеш підкинути наркотики:")
                .replyMarkup(keyboardService.getTargetSelectionPersonKeyboard(user.getChatId(), user.getId(), QueryDataDto.DROP_DRUGS_TYPE)));
        log.info("Drop drug request created");
    }

    public void processDropDrug(UserInfo originUser, QueryDataDto query) {
        Order order = orderRepository.findAllByChatIdAndStageAndOriginUserIdAndType(originUser.getChatId(),
                        ReplyOrderStage.TARGET_REQUIRED.getId(),
                        originUser.getId(),
                        OrderType.DROP_DRUGS.getId()).stream()
                .findFirst()
                .map(orderMapper::toDto)
                .orElse(null);

        if (order == null) {
            return;
        }

        order.setStage(ReplyOrderStage.DONE);

        if ("0".equals(query.getOption())) {
            orderRepository.deleteById(order.getId());
            originUser.plusCredit(DROP_DRUG_PRICE);
            userRepository.save(userMapper.toEntity(originUser));
            bot.execute(new SendMessage(originUser.getChatId(), "Зловмисник одумався"));
            return;
        }

        UserInfo targetUser = userRepository.findById(Long.parseLong(query.getOption()))
                .map(userMapper::toDto)
                .orElseThrow();
        order.setTargetUser(targetUser);
        orderRepository.save(orderMapper.toEntity(order));

        drugActionRepository.save(DrugActionEntity.builder()
                .user(userMapper.toEntity(targetUser))
                .expires(LocalDateTime.now().plusHours(24))
                .chatId(targetUser.getChatId())
                .build());

        bot.execute(new SendMessage(originUser.getChatId(), "Наркотики підкинуто"));
    }

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

    public List<DrugAction> findActiveDrugDeals(UserInfo user) {
        return drugActionRepository.findByUserAndChatIdAndExpiresAfter(
                        userMapper.toEntity(user),
                        user.getChatId(),
                        LocalDateTime.now()
                ).stream()
                .map(drugActionMapper::toDto)
                .collect(Collectors.toList());
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
