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
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    private final TelegramBot bot;

    private static final Long DEFAULT_ITERATIONS = 10L;
    private static final Integer DEFAULT_PRICE = 50;

    public void createReplyOrder(UserInfo originUser) {
        if (originUser.getSocialCredit() < DEFAULT_PRICE) {
            bot.execute(new SendMessage(originUser.getChatId(), "Підсобирай кредитів жебрак"));
            return;
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
        bot.execute(new SendMessage(originUser.getChatId(), "Обери жертву:")
                .parseMode(ParseMode.HTML)
                .replyMarkup(keyboardService.getTargetSelectionPersonKeyboard(originUser.getChatId(), originUser.getId(),
                        QueryDataDto.REPLY_ORDER_TYPE)));
    }

    public void checkOrders(Long chatId, UserInfo originUser,
                                                   String messageText, Integer messageId, Income source) {
        getActiveOrders(chatId)
                .forEach(order -> processReplyOrder(order, originUser, messageText, messageId, source));
    }

    private void processReplyOrder(Order order, UserInfo sender, String messageText, Integer messageId, Income source) {
        switch(order.getStage()) {
            case TARGET_REQUIRED:
                processReplyTarget(order, messageText, sender, source);
                break;
            case MESSAGE_REQUIRED:
                processReplyMessage(order, messageText, sender, messageId, source);
                break;
            case IN_PROGRESS:
                processInProgressMessage(order, sender, messageId, source);
                break;
        }
    }

    private void processInProgressMessage(Order order, UserInfo targetUser, Integer messageId, Income source) {
        if (!order.getTargetUser().getId().equals(targetUser.getId()) || Income.DATA.equals(source)) {
            return;
        }

        order.setCurrentIteration(order.getCurrentIteration() + 1);
        if (order.getCurrentIteration() >= order.getIterationCount()) {
            order.setStage(DONE);
        }
        repository.save(orderMapper.toEntity(order));
        bot.execute(new SendMessage(order.getChatId(), order.getRespondMessage())
                .replyToMessageId(messageId));
    }
    private void processReplyMessage(Order order, String messageText, UserInfo originUser, Integer messageId, Income source) {
        if (!order.getOriginUserId().equals(originUser.getId()) || Income.DATA.equals(source)) {
            return;
        }

        order.setRespondMessage(messageText);
        order.setStage(IN_PROGRESS);

        repository.save(orderMapper.toEntity(order));
        bot.execute(new SendMessage(order.getChatId(), "Відповідь встановленно"));
        bot.execute(new DeleteMessage(order.getChatId(), messageId));
    }

    private void processReplyTarget(Order order, String messageText, UserInfo originUser, Income source) {

        if (!order.getOriginUserId().equals(originUser.getId())) {
            return;
        }

        if (Income.TEXT.equals(source)) {
            bot.execute(new SendMessage(order.getChatId(), "Я все ще чекаю на твій вибір!")
                    .replyMarkup(keyboardService.getTargetSelectionPersonKeyboard(order.getChatId(), originUser.getId(),
                            QueryDataDto.REPLY_ORDER_TYPE)));
            return;
        }

        Long targetId;
        UserInfo targetUser;
        try {
            targetId = Long.valueOf(messageText);
            if (targetId.equals(0L)) {
                repository.deleteById(order.getId());
                originUser.plusCredit(DEFAULT_PRICE);
                userRepository.save(userMapper.toEntity(originUser));
                bot.execute(new SendMessage(order.getChatId(), "Замовлення скасовано"));
                return;
            }
            targetUser = userRepository.findById(targetId)
                    .map(userMapper::toDto)
                    .orElse(null);
            if (targetUser == null || targetUser.getId().equals(originUser.getId())) {
                bot.execute(new SendMessage(order.getChatId(), "Я все ще чекаю на твій вибір!")
                        .replyMarkup(keyboardService.getTargetSelectionPersonKeyboard(order.getChatId(), originUser.getId(),
                                QueryDataDto.REPLY_ORDER_TYPE)));
                return;
            }
        } catch (NumberFormatException ex) {
            log.info("Invalid id: {}", messageText);
            bot.execute(new SendMessage(order.getChatId(), "Я все ще чекаю на твій вибір!")
                    .replyMarkup(keyboardService.getTargetSelectionPersonKeyboard(order.getChatId(), originUser.getId(),
                            QueryDataDto.REPLY_ORDER_TYPE)));
            return;
        }

        order.setTargetUser(targetUser);
        order.setStage(MESSAGE_REQUIRED);
        repository.save(orderMapper.toEntity(order));
        bot.execute(new SendMessage(order.getChatId(), "Ок, тепер напиши який текст ти хочеш встановити на автовідповідь"));
    }
    private Stream<Order> getActiveOrders(Long chatId) {
        return repository.findAllByChatIdAndStageIsNot(chatId, ReplyOrderStage.DONE.getId())
                .stream()
                .map(orderMapper::toDto)
                .filter(o -> OrderType.MESSAGE_REPLY.equals(o.getType()));
    }

    public enum Income {
        TEXT, DATA
    }
}
