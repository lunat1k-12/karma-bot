package com.coolguys.bot.service;

import com.coolguys.bot.dto.Order;
import com.coolguys.bot.dto.OrderType;
import com.coolguys.bot.dto.ReplyOrderStage;
import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.mapper.OrderMapper;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.OrderRepository;
import com.coolguys.bot.repository.UserRepository;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.coolguys.bot.dto.ReplyOrderStage.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository repository;
    private final OrderMapper orderMapper;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    private static final Long DEFAULT_ITERATIONS = 10L;
    private static final Integer DEFAULT_PRICE = 50;

    public Optional<SendMessage> createReplyOrder(UserInfo originUser) {
        if (originUser.getSocialCredit() < DEFAULT_PRICE) {
            return Optional.of(new SendMessage(originUser.getChatId(), "Підсобирай кредитів жебрак"));
        }

        Order newOrder = Order.builder()
                .chatId(originUser.getChatId())
                .originUserId(originUser.getId())
                .type(OrderType.MESSAGE_REPLY)
                .stage(ReplyOrderStage.TARGET_REQUIRED)
                .iterationCount(DEFAULT_ITERATIONS)
                .currentIteration(0L)
                .build();

        originUser.setSocialCredit(originUser.getSocialCredit() - DEFAULT_PRICE);
        userRepository.save(userMapper.toEntity(originUser));
        repository.save(orderMapper.toEntity(newOrder));
        return Optional.of(new SendMessage(originUser.getChatId(), "Обери жертву:")
                .parseMode(ParseMode.HTML)
                .replyMarkup(getTargetSelectionPersonKeyboard(originUser.getChatId(), originUser.getId())));
    }

    public List<Optional<SendMessage>> checkOrders(Long chatId,  UserInfo originUser,
                                                   String messageText, Integer messageId, Income source) {
        return getActiveOrders(chatId)
                .map(order -> processReplyOrder(order, originUser, messageText, messageId, source))
                .filter(Optional::isPresent)
                .collect(Collectors.toList());
    }

    private Optional<SendMessage> processReplyOrder(Order order, UserInfo sender, String messageText, Integer messageId, Income source) {
        switch(order.getStage()) {
            case TARGET_REQUIRED:
                return processReplyTarget(order, messageText, sender, source);
            case MESSAGE_REQUIRED:
                return processReplyMessage(order, messageText, sender, messageId);
            case IN_PROGRESS:
                return processInProgressMessage(order, sender, messageId);
        }

        log.info("unknown Order stage - {}", order.getStage());
        return Optional.empty();
    }

    private Optional<SendMessage> processInProgressMessage(Order order, UserInfo targetUser, Integer messageId) {
        if (!order.getTargetUser().getId().equals(targetUser.getId())) {
            return Optional.empty();
        }

        order.setCurrentIteration(order.getCurrentIteration() + 1);
        if (order.getCurrentIteration() >= order.getIterationCount()) {
            order.setStage(DONE);
        }
        repository.save(orderMapper.toEntity(order));
        return Optional.of(new SendMessage(order.getChatId(), order.getRespondMessage())
                .replyToMessageId(messageId));
    }
    private Optional<SendMessage> processReplyMessage(Order order, String messageText, UserInfo originUser, Integer messageId) {
        if (!order.getOriginUserId().equals(originUser.getId())) {
            return Optional.empty();
        }

        order.setRespondMessage(messageText);
        order.setStage(IN_PROGRESS);

        repository.save(orderMapper.toEntity(order));
        return Optional.of(new SendMessage(order.getChatId(), "Відповідь встановленно")
                .replyToMessageId(messageId));
    }

    private Optional<SendMessage> processReplyTarget(Order order, String messageText, UserInfo originUser, Income source) {

        if (!order.getOriginUserId().equals(originUser.getId())) {
            return Optional.empty();
        }

        if (Income.TEXT.equals(source)) {
            return Optional.of(new SendMessage(order.getChatId(), "Я все ще чекаю на твій вибір!")
                    .replyMarkup(getTargetSelectionPersonKeyboard(order.getChatId(), originUser.getId())));
        }

        Long targetId = null;
        UserInfo targetUser = null;
        try {
            targetId = Long.valueOf(messageText);
            if (targetId.equals(0L)) {
                repository.deleteById(order.getId());
                originUser.plusCredit(DEFAULT_PRICE);
                userRepository.save(userMapper.toEntity(originUser));
                return Optional.of(new SendMessage(order.getChatId(), "Замовлення скасовано"));
            }
            targetUser = userRepository.findById(targetId)
                    .map(userMapper::toDto)
                    .orElse(null);
            if (targetUser == null || targetUser.getId().equals(originUser.getId())) {
                return Optional.of(new SendMessage(order.getChatId(), "Я все ще чекаю на твій вибір!")
                        .replyMarkup(getTargetSelectionPersonKeyboard(order.getChatId(), originUser.getId())));
            }
        } catch (NumberFormatException ex) {
            log.info("Invalid id: {}", messageText);
            return Optional.of(new SendMessage(order.getChatId(), "Я все ще чекаю на твій вибір!")
                    .replyMarkup(getTargetSelectionPersonKeyboard(order.getChatId(), originUser.getId())));
        }

        order.setTargetUser(targetUser);
        order.setStage(MESSAGE_REQUIRED);
        repository.save(orderMapper.toEntity(order));
        return Optional.of(new SendMessage(order.getChatId(), "Ок, тепер напиши який текст ти хочеш встановити на автовідповідь"));
    }
    private Stream<Order> getActiveOrders(Long chatId) {
        return repository.findAllByChatIdAndStageIsNot(chatId, ReplyOrderStage.DONE.getId())
                .stream()
                .map(orderMapper::toDto);
    }

    private InlineKeyboardMarkup getTargetSelectionPersonKeyboard(Long chatId, Long originalUserId) {
        List<UserInfo> users = userRepository.findByChatId(chatId)
                .stream()
                .filter(u -> !u.getId().equals(originalUserId))
                .map(userMapper::toDto)
                .collect(Collectors.toList());

        UserInfo cancelUser = UserInfo.builder()
                .id(0L)
                .username("Відмінити")
                .build();

        List<UserInfo> usersToProcess = new ArrayList<>();
        usersToProcess.add(cancelUser);
        usersToProcess.addAll(users);

        int verticalRowCount = Double.valueOf(Math.ceil(Integer.valueOf(usersToProcess.size()).doubleValue() / 2)).intValue();
        InlineKeyboardButton[][] keys = new InlineKeyboardButton[verticalRowCount][2];

        int usersIndex = 0;
        for (int i = 0; i < keys.length; i++) {
            for (int j=0; j < keys[i].length; j++) {
                if (usersToProcess.size() > usersIndex) {
                    UserInfo user = usersToProcess.get(usersIndex);
                    keys[i][j] = new InlineKeyboardButton(user.getUsername()).callbackData(user.getId().toString());
                    usersIndex++;
                }
            }
        }

        if (usersToProcess.size() % 2 != 0) {
            keys[verticalRowCount - 1] = new InlineKeyboardButton[]{keys[verticalRowCount - 1][0]};
        }
        return new InlineKeyboardMarkup(keys);
    }

    public enum Income {
        TEXT, DATA;
    }
}
