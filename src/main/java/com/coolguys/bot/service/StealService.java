package com.coolguys.bot.service;

import com.coolguys.bot.dto.Order;
import com.coolguys.bot.dto.OrderType;
import com.coolguys.bot.dto.QueryDataDto;
import com.coolguys.bot.dto.ReplyOrderStage;
import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.entity.BanRecordEntity;
import com.coolguys.bot.mapper.OrderMapper;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.BanRecordRepository;
import com.coolguys.bot.repository.OrderRepository;
import com.coolguys.bot.repository.UserRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendDice;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class StealService {

    private final BanRecordRepository banRecordRepository;
    private final UserMapper userMapper;
    private final KeyboardService keyboardService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    public static final int PAUSE_MILLIS = 3000;
    public static final int FEE = 100;

    public void stealRequest(UserInfo originUser, TelegramBot bot) {
        log.info("New steal request from {}", originUser.getUsername());
        if (!banRecordRepository.findByUserAndChatIdAndExpiresAfter(userMapper.toEntity(originUser),
                originUser.getChatId(), LocalDateTime.now()).isEmpty()) {
            bot.execute(new SendMessage(originUser.getChatId(), "Куди ти лізеш ворюга, тебе вже за руку спіймали!"));
            return;
        }

        Order order = orderRepository.findAllByChatIdAndStageAndOriginUserId(originUser.getChatId(),
                        ReplyOrderStage.TARGET_REQUIRED.getId(),
                        originUser.getId()).stream()
                .findFirst()
                .map(orderMapper::toDto)
                .orElse(null);

        if (order != null) {
            bot.execute(new SendMessage(originUser.getChatId(), "ти вже створив замовлення\nОбери кого хочеш обокрасти:")
                    .replyMarkup(keyboardService.getTargetSelectionPersonKeyboard(originUser.getChatId(), originUser.getId(), QueryDataDto.STEAL_TYPE)));
            return;
        }

        Order newOrder = Order.builder()
                .chatId(originUser.getChatId())
                .originUserId(originUser.getId())
                .type(OrderType.STEAL)
                .stage(ReplyOrderStage.TARGET_REQUIRED)
                .build();
        orderRepository.save(orderMapper.toEntity(newOrder));

        bot.execute(new SendMessage(originUser.getChatId(), "Обери кого хочеш обокрасти:")
                .replyMarkup(keyboardService.getTargetSelectionPersonKeyboard(originUser.getChatId(), originUser.getId(), QueryDataDto.STEAL_TYPE)));
    }

    public void processSteal(UserInfo originUser, QueryDataDto query, TelegramBot bot) {
        Order order = orderRepository.findAllByChatIdAndStageAndOriginUserId(originUser.getChatId(),
                ReplyOrderStage.TARGET_REQUIRED.getId(),
                originUser.getId()).stream()
                .findFirst()
                .map(orderMapper::toDto)
                .orElse(null);

        if (order == null) {
            return;
        }

        order.setStage(ReplyOrderStage.DONE);
        orderRepository.save(orderMapper.toEntity(order));
        try {
            if ("0".equals(query.getOption())) {
                orderRepository.deleteById(order.getId());
                bot.execute(new SendMessage(originUser.getChatId(), "Крадій одумався"));
                return;
            }
            UserInfo targetUser = userRepository.findById(Long.parseLong(query.getOption()))
                    .map(userMapper::toDto)
                    .orElseThrow();

            if (targetUser.getSocialCredit() <= 0) {
                bot.execute(new SendMessage(originUser.getChatId(), String.format("Що ти хотів вкрасти у @%s? він бесхатько!", targetUser.getUsername())));
                orderRepository.deleteById(order.getId());
                return;
            }

            bot.execute(new SendMessage(originUser.getChatId(),
                    String.format("Невідомий намагається вкрасти у @%s", targetUser.getUsername())));


            Thread.sleep(PAUSE_MILLIS);
            Random random = new Random();
            int difficulty = random.nextInt(6);

            bot.execute(new SendMessage(originUser.getChatId(), String.format("Складність крадіжки: %s", difficulty)));
            Thread.sleep(PAUSE_MILLIS);

            SendResponse response = bot.execute(new SendDice(originUser.getChatId())
                    .emoji("\uD83C\uDFB2"));
            Thread.sleep(PAUSE_MILLIS);
            if (response.message().dice().value() >= difficulty) {
                int price = random.nextInt(targetUser.getSocialCredit());
                bot.execute(new SendMessage(originUser.getChatId(), String.format("Невідомий крадій успішно вкрав %s кредитів у @%s",
                        price, targetUser.getUsername())));
                targetUser.minusCredit(price);
                originUser.plusCredit(price);
                userRepository.save(userMapper.toEntity(targetUser));
                userRepository.save(userMapper.toEntity(originUser));
            } else {
                bot.execute(new SendMessage(originUser.getChatId(), String.format("Невдача! @%s спіймали за руку. Штраф %s і " +
                        "заборона на доступ до казино на 24 години!", originUser.getUsername(), FEE)));
                originUser.minusCredit(FEE);
                userRepository.save(userMapper.toEntity(originUser));
                banRecordRepository.save(BanRecordEntity.builder()
                        .user(userMapper.toEntity(originUser))
                        .expires(LocalDateTime.now().plusHours(24))
                        .chatId(originUser.getChatId())
                        .build());
            }

        } catch (InterruptedException e) {
            log.error("Error while steal", e);
        }
    }
}
