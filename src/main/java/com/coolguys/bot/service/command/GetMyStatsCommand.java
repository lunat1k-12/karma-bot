package com.coolguys.bot.service.command;

import com.coolguys.bot.conf.BotConfig;
import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.service.MyStatsService;
import com.pengrad.telegrambot.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class GetMyStatsCommand implements Command {

    private final BotConfig botConfig;
    private final MyStatsService myStatsService;

    @Override
    public void processCommand(Message message, ChatAccount originAccount) {
        myStatsService.printPersonalStats(originAccount);
    }

    @Override
    public String getCommand() {
        return botConfig.getMyStatsCommand();
    }
}
