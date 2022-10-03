package com.coolguys.bot.service.command;

import com.coolguys.bot.conf.BotConfig;
import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.service.InfoService;
import com.pengrad.telegrambot.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InfoCommand implements Command {

    private final BotConfig botConfig;
    private final InfoService infoService;

    @Override
    public void processCommand(Message message, ChatAccount originAccount) {
        infoService.printInfo(originAccount.getChat().getId(), originAccount.getChat().getName());
    }

    @Override
    public String getCommand() {
        return botConfig.getInfoCommand();
    }
}
