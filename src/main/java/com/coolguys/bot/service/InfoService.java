package com.coolguys.bot.service;

import com.coolguys.bot.conf.BotConfig;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InfoService {

    private final TelegramBot bot;
    private final BotConfig botConfig;

    @Value("classpath:instructions.txt")
    private Resource resource;

    public void printInfo(Long chatId) {
        try {
            String text = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))
                    .lines()
                    .collect(Collectors.joining("\n"));
            bot.execute(new SendMessage(chatId, text.replace("${credits}", botConfig.getCreditCommand())
                    .replace("${autoReply}", botConfig.getAutoReplyCommand())
                    .replace("${removePlayBan}", botConfig.getRemovePlayBanCommand())
                    .replace("${buyGuard}", botConfig.getBuyGuardCommand())
                    .replace("${buyCasino}", botConfig.getBuyCasinoCommand())
                    .replace("${buyPolice}", botConfig.getBuyPoliceCommand())
                    .replace("${buyGuardDep}", botConfig.getBuyGuardDepartmentCommand())
                    .replace("${selectRole}", botConfig.getSelectRoleCommand())
                    .replace("${roleActions}", botConfig.getRoleActionsCommand())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
