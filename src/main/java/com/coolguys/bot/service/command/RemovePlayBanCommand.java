package com.coolguys.bot.service.command;

import com.coolguys.bot.conf.BotConfig;
import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.service.DiceService;
import com.pengrad.telegrambot.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RemovePlayBanCommand implements Command {

    private final BotConfig botConfig;
    private final DiceService diceService;
    @Override
    public void processCommand(Message message, ChatAccount originAccount) {
        diceService.removePlayBan(originAccount);
    }

    @Override
    public String getCommand() {
        return botConfig.getRemovePlayBanCommand();
    }
}
