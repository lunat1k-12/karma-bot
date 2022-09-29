package com.coolguys.bot.service.command;

import com.coolguys.bot.conf.BotConfig;
import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.service.OrderService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.DeleteMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AutoReplyCommand implements Command {

    private final BotConfig botConfig;
    private final OrderService orderService;
    private final TelegramBot bot;

    @Override
    public void processCommand(Message message, ChatAccount originAccount) {
        orderService.createReplyOrder(originAccount);
        bot.execute(new DeleteMessage(message.chat().id(), message.messageId()));
    }

    @Override
    public String getCommand() {
        return botConfig.getAutoReplyCommand();
    }
}
