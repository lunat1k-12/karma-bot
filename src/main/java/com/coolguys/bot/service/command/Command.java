package com.coolguys.bot.service.command;

import com.coolguys.bot.dto.ChatAccount;
import com.pengrad.telegrambot.model.Message;

public interface Command {

    void processCommand(Message message, ChatAccount originAccount);
    String getCommand();
}
