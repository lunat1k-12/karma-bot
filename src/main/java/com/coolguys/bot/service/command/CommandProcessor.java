package com.coolguys.bot.service.command;

import com.coolguys.bot.dto.ChatAccount;
import com.pengrad.telegrambot.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommandProcessor {

    private final List<Command> commands;

    public void processCommand(Message message, ChatAccount originAccount) {
        commands.stream()
                .filter(c -> c.getCommand().equals(message.text()))
                .forEach(c -> {
                    log.info("Process {} command", c.getCommand());
                    c.processCommand(message, originAccount);
                });
    }

    public boolean isCommandExists(Message message) {
        if (message.text() == null) {
            return false;
        }

        return commands.stream()
                .map(Command::getCommand)
                .anyMatch(c -> c.equals(message.text()));
    }
}
