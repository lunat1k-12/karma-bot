package com.coolguys.bot.service.command;

import com.coolguys.bot.conf.BotConfig;
import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.service.BuyPropertyService;
import com.pengrad.telegrambot.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BuyCasinoCommand implements Command {

    private final BotConfig botConfig;
    private final BuyPropertyService buyPropertyService;

    @Override
    public void processCommand(Message message, ChatAccount originAccount) {
        buyPropertyService.processCasinoBuy(originAccount);
    }

    @Override
    public String getCommand() {
        return botConfig.getBuyCasinoCommand();
    }
}
