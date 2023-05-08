package com.coolguys.bot.service.command;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.repository.ChatAccountRepository;
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
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;

    public void processCommand(Message message, ChatAccount originAccount) {
        ChatAccount refreshedAcc = chatAccountRepository.findById(originAccount.getId())
                .map(chatAccountMapper::toDto)
                .orElse(originAccount);
        commands.stream()
                .filter(c -> c.getCommand().equals(message.text()) || concatCommand(c.getCommand()).equals(message.text()))
                .forEach(c -> {
                    log.info("Process {} command", c.getCommand());
                    c.processCommand(message, refreshedAcc);
                });
    }

    public boolean isCommandExists(Message message) {
        if (message.text() == null) {
            return false;
        }

        return commands.stream()
                .map(Command::getCommand)
                .anyMatch(c -> c.equals(message.text()) || concatCommand(c).equals(message.text()));
    }
    private String concatCommand(String command) {
        return command.substring(0, command.indexOf('@'));
    }
}
