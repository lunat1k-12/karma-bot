package com.coolguys.bot.service;

import com.coolguys.bot.dto.Order;
import com.coolguys.bot.dto.OrderType;
import com.coolguys.bot.dto.QueryDataDto;
import com.coolguys.bot.dto.ReplyOrderStage;
import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.mapper.OrderMapper;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.OrderRepository;
import com.coolguys.bot.repository.UserRepository;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.coolguys.bot.dto.ReplyOrderStage.DONE;
import static com.coolguys.bot.dto.ReplyOrderStage.IN_PROGRESS;
import static com.coolguys.bot.dto.ReplyOrderStage.MESSAGE_REQUIRED;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository repository;
    private final OrderMapper orderMapper;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final KeyboardService keyboardService;

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
                .replyMarkup(keyboardService.getTargetSelectionPersonKeyboard(originUser.getChatId(), originUser.getId(),
                        QueryDataDto.REPLY_ORDER_TYPE)));
    }

    public List<Optional<BaseRequest>> checkOrders(Long chatId, UserInfo originUser,
                                                   String messageText, Integer messageId, Income source) {
        return getActiveOrders(chatId)
                .flatMap(order -> processReplyOrder(order, originUser, messageText, messageId, source).stream())
                .filter(Optional::isPresent)
                .collect(Collectors.toList());
    }

    private List<Optional<BaseRequest>> processReplyOrder(Order order, UserInfo sender, String messageText, Integer messageId, Income source) {
        switch(order.getStage()) {
            case TARGET_REQUIRED:
                return List.of(processReplyTarget(order, messageText, sender, source));
            case MESSAGE_REQUIRED:
                return processReplyMessage(order, messageText, sender, messageId, source);
            case IN_PROGRESS:
                return List.of(processInProgressMessage(order, sender, messageId, source));
        }

        log.info("unknown Order stage - {}", order.getStage());
        return Collections.emptyList();
    }

    private Optional<BaseRequest> processInProgressMessage(Order order, UserInfo targetUser, Integer messageId, Income source) {
        if (!order.getTargetUser().getId().equals(targetUser.getId()) || Income.DATA.equals(source)) {
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
    private List<Optional<BaseRequest>> processReplyMessage(Order order, String messageText, UserInfo originUser, Integer messageId, Income source) {
        List<Optional<BaseRequest>> result = new ArrayList<>();
        if (!order.getOriginUserId().equals(originUser.getId()) || Income.DATA.equals(source)) {
            return result;
        }

        order.setRespondMessage(messageText);
        order.setStage(IN_PROGRESS);

        repository.save(orderMapper.toEntity(order));
        result.add(Optional.of(new SendMessage(order.getChatId(), "Відповідь встановленно")));
        result.add(Optional.of(new DeleteMessage(order.getChatId(), messageId)));
        return result;
    }

    private Optional<BaseRequest> processReplyTarget(Order order, String messageText, UserInfo originUser, Income source) {

        if (!order.getOriginUserId().equals(originUser.getId())) {
            return Optional.empty();
        }

        if (Income.TEXT.equals(source)) {
            return Optional.of(new SendMessage(order.getChatId(), "Я все ще чекаю на твій вибір!")
                    .replyMarkup(keyboardService.getTargetSelectionPersonKeyboard(order.getChatId(), originUser.getId(),
                            QueryDataDto.REPLY_ORDER_TYPE)));
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
                        .replyMarkup(keyboardService.getTargetSelectionPersonKeyboard(order.getChatId(), originUser.getId(),
                                QueryDataDto.REPLY_ORDER_TYPE)));
            }
        } catch (NumberFormatException ex) {
            log.info("Invalid id: {}", messageText);
            return Optional.of(new SendMessage(order.getChatId(), "Я все ще чекаю на твій вибір!")
                    .replyMarkup(keyboardService.getTargetSelectionPersonKeyboard(order.getChatId(), originUser.getId(),
                            QueryDataDto.REPLY_ORDER_TYPE)));
        }

        order.setTargetUser(targetUser);
        order.setStage(MESSAGE_REQUIRED);
        repository.save(orderMapper.toEntity(order));
        return Optional.of(new SendMessage(order.getChatId(), "Ок, тепер напиши який текст ти хочеш встановити на автовідповідь"));
    }
    private Stream<Order> getActiveOrders(Long chatId) {
        return repository.findAllByChatIdAndStageIsNot(chatId, ReplyOrderStage.DONE.getId())
                .stream()
                .map(orderMapper::toDto)
                .filter(o -> OrderType.MESSAGE_REPLY.equals(o.getType()));
    }

    public enum Income {
        TEXT, DATA;
    }
}
