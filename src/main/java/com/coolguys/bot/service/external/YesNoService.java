package com.coolguys.bot.service.external;

import com.coolguys.bot.dto.YesNoResponse;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendAnimation;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class YesNoService {
    private final RestTemplate rest = new RestTemplateBuilder().build();
    private final TelegramBot bot;

    public YesNoService(TelegramBot bot) {
        this.bot = bot;
    }

    public void answerIfNeeded(Message msg, Long chatId) {
        if (msg.text().endsWith("?)")) {
            answer(msg, chatId);
        }
    }

    public void answer(Message msg, Long chatId) {
        var resp = rest.getForObject("https://yesno.wtf/api", YesNoResponse.class);
        if (resp != null) {
            log.info("Response: {}, URL: {}", resp.getAnswer(), resp.getImage());
            var result = bot.execute(new SendAnimation(chatId, resp.getImage()).replyToMessageId(msg.messageId()));
            log.info("Result: {}", result);
        } else {
            log.info("Error fetching answer");
            bot.execute(new SendMessage(chatId, "Не зміг знайти відповідь"));
        }
    }
}
