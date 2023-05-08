package com.coolguys.bot.service;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.OrderType;
import com.coolguys.bot.dto.QueryDataDto;
import com.coolguys.bot.dto.ReplyOrderStage;
import com.coolguys.bot.dto.TelegramOrder;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.mapper.TelegramOrderMapper;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.repository.TelegramOrderRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Sticker;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendSticker;
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
    private final KeyboardService keyboardService;
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;
    private final TelegramOrderRepository telegramOrderRepository;
    private final TelegramOrderMapper telegramOrderMapper;
    private final TelegramBot bot;

    private static final Long DEFAULT_ITERATIONS = 10L;
    private static final Integer DEFAULT_PRICE = 50;

    public void createReplyOrder(ChatAccount originAcc) {
        if (originAcc.getSocialCredit() < DEFAULT_PRICE) {
            bot.execute(new SendMessage(originAcc.getChat().getId(), "Підсобирай кредитів жебрак"));
            return;
        }

        TelegramOrder newOrder = TelegramOrder.builder()
                .chatId(originAcc.getChat().getId())
                .originAccId(originAcc.getId())
                .type(OrderType.MESSAGE_REPLY)
                .stage(ReplyOrderStage.TARGET_REQUIRED)
                .iterationCount(DEFAULT_ITERATIONS)
                .currentIteration(0L)
                .build();

        originAcc.minusCredit(DEFAULT_PRICE);
        chatAccountRepository.save(chatAccountMapper.toEntity(originAcc));
        telegramOrderRepository.save(telegramOrderMapper.toEntity(newOrder));
        bot.execute(new SendMessage(originAcc.getChat().getId(), "Обери жертву:")
                .parseMode(ParseMode.HTML)
                .replyMarkup(keyboardService.getTargetAccSelectionPersonKeyboard(originAcc.getChat().getId(), originAcc.getId(),
                        QueryDataDto.REPLY_ORDER_TYPE)));
    }

    public void checkOrders(Message message, String text, ChatAccount originUser, Income source) {
        getActiveTelegramOrders(message.chat().id())
                .forEach(order -> processReplyOrder(order, originUser, text, message.messageId(), source, message.sticker()));
    }

    private void processReplyOrder(TelegramOrder order, ChatAccount sender, String messageText, Integer messageId, Income source, Sticker sticker) {
        switch(order.getStage()) {
            case TARGET_REQUIRED:
                processReplyTarget(order, messageText, sender, source, messageId);
                break;
            case MESSAGE_REQUIRED:
                processReplyMessage(order, messageText, sender, messageId, source, sticker);
                break;
            case IN_PROGRESS:
                processInProgressMessage(order, sender, messageId, source);
                break;
        }
    }

    private void processInProgressMessage(TelegramOrder order, ChatAccount targetAcc, Integer messageId, Income source) {
        if (!order.getTargetAcc().getId().equals(targetAcc.getId()) || Income.DATA.equals(source)) {
            return;
        }

        order.setCurrentIteration(order.getCurrentIteration() + 1);
        if (order.getCurrentIteration() >= order.getIterationCount()) {
            order.setStage(DONE);
        }

        telegramOrderRepository.save(telegramOrderMapper.toEntity(order));
        if (order.getStickerId() != null) {
            bot.execute(new SendSticker(order.getChatId(), order.getStickerId())
                    .replyToMessageId(messageId));
        } else {
            bot.execute(new SendMessage(order.getChatId(), order.getRespondMessage())
                    .replyToMessageId(messageId));
        }
    }

    private void processReplyMessage(TelegramOrder order, String messageText,
                                     ChatAccount originAcc, Integer messageId,
                                     Income source, Sticker sticker) {
        if (!order.getOriginAccId().equals(originAcc.getId()) || Income.DATA.equals(source)) {
            return;
        }

        if (sticker != null) {
            order.setStickerId(sticker.fileId());
        } else {
            order.setRespondMessage(messageText);
        }

        order.setStage(IN_PROGRESS);
        telegramOrderRepository.save(telegramOrderMapper.toEntity(order));
        bot.execute(new SendMessage(order.getChatId(), "Відповідь встановленно"));
        bot.execute(new DeleteMessage(order.getChatId(), messageId));
    }

    private void processReplyTarget(TelegramOrder order, String messageText, ChatAccount originAcc, Income source, Integer messageId) {

        if (!order.getOriginAccId().equals(originAcc.getId())) {
            return;
        }

        if (Income.TEXT.equals(source)) {
            bot.execute(new SendMessage(order.getChatId(), "Я все ще чекаю на твій вибір!")
                    .replyMarkup(keyboardService.getTargetAccSelectionPersonKeyboard(order.getChatId(), originAcc.getId(),
                            QueryDataDto.REPLY_ORDER_TYPE)));
            return;
        }

        Long targetId;
        ChatAccount targetAcc;
        try {
            targetId = Long.valueOf(messageText);
            if (targetId.equals(0L)) {
                telegramOrderRepository.deleteById(order.getId());
                originAcc.plusCredit(DEFAULT_PRICE);
                chatAccountRepository.save(chatAccountMapper.toEntity(originAcc));
                bot.execute(new SendMessage(order.getChatId(), "Замовлення скасовано"));
                keyboardService.deleteOrUpdateKeyboardMessage(originAcc.getChat().getId(),  messageId);
                return;
            }
            targetAcc = chatAccountRepository.findById(targetId)
                    .map(chatAccountMapper::toDto)
                    .orElse(null);
            if (targetAcc == null || targetAcc.getId().equals(originAcc.getId())) {
                bot.execute(new SendMessage(order.getChatId(), "Я все ще чекаю на твій вибір!")
                        .replyMarkup(keyboardService.getTargetAccSelectionPersonKeyboard(order.getChatId(), originAcc.getId(),
                                QueryDataDto.REPLY_ORDER_TYPE)));
                keyboardService.deleteOrUpdateKeyboardMessage(originAcc.getChat().getId(),  messageId);
                return;
            }
        } catch (NumberFormatException ex) {
            log.info("Invalid id: {}", messageText);
            bot.execute(new SendMessage(order.getChatId(), "Я все ще чекаю на твій вибір!")
                    .replyMarkup(keyboardService.getTargetAccSelectionPersonKeyboard(order.getChatId(), originAcc.getId(),
                            QueryDataDto.REPLY_ORDER_TYPE)));
            keyboardService.deleteOrUpdateKeyboardMessage(originAcc.getChat().getId(),  messageId);
            return;
        }

        order.setTargetAcc(targetAcc);
        order.setStage(MESSAGE_REQUIRED);
        telegramOrderRepository.save(telegramOrderMapper.toEntity(order));
        bot.execute(new SendMessage(order.getChatId(), "Ок, тепер напиши який текст або стікер ти хочеш встановити на автовідповідь"));
        keyboardService.deleteOrUpdateKeyboardMessage(originAcc.getChat().getId(),  messageId);
    }

    private Stream<TelegramOrder> getActiveTelegramOrders(Long chatId) {
        return telegramOrderRepository.findAllByChatIdAndTypeAndStageIsNot(chatId, OrderType.MESSAGE_REPLY.getId(), ReplyOrderStage.DONE.getId())
                .stream()
                .map(telegramOrderMapper::toDto);
    }

    public enum Income {
        TEXT, DATA
    }
}
