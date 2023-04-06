package com.coolguys.bot.service.command;

import com.coolguys.bot.conf.BotConfig;
import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.service.KeyboardService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.coolguys.bot.dto.QueryDataDto.ZODIAC_TYPE;

@Component
@RequiredArgsConstructor
@Slf4j
public class SetZodiacCommand implements Command {

    private final BotConfig botConfig;
    private final KeyboardService keyboardService;
    private final TelegramBot bot;

    @Override
    public void processCommand(Message message, ChatAccount originAccount) {
        bot.execute(new SendMessage(originAccount.getChat().getId(), "Обери свій знак зодіаку:")
                .parseMode(ParseMode.HTML)
                .replyMarkup(keyboardService.getZodiacKeyboard(originAccount, ZODIAC_TYPE)));
        log.info("request for zodiac from {}", originAccount.getUser().getUsername());
    }

    @Override
    public String getCommand() {
        return botConfig.getSetZodiacCommand();
    }
}
