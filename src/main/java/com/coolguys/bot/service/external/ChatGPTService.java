package com.coolguys.bot.service.external;

import com.coolguys.bot.conf.BotConfig;
import com.coolguys.bot.service.external.dto.GptRequest;
import com.coolguys.bot.service.external.dto.GtpResponse;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.ChatAction;
import com.pengrad.telegrambot.request.SendChatAction;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ChatGPTService {

    private final RestTemplate rest = new RestTemplateBuilder()
            .setConnectTimeout(Duration.of(1, ChronoUnit.MINUTES))
            .setReadTimeout(Duration.of(1, ChronoUnit.MINUTES))
            .build();
    private final TelegramBot bot;
    private final YesNoService yesNoService;
    private final String rapidApiKey;

    public ChatGPTService(TelegramBot bot, YesNoService yesNoService,
                          BotConfig botConfig) {
        this.bot = bot;
        this.yesNoService = yesNoService;
        this.rapidApiKey = botConfig.getRapidApiKey();
        log.info("Rapid API key - {}", rapidApiKey);
    }

    public void getAnswer(Message message, Long chatId) {
        if (message.text().endsWith("?")) {
            log.info("Call chatGPT");
            bot.execute(new SendChatAction(chatId, ChatAction.typing));
            StringBuilder sb = new StringBuilder();

            Optional.ofNullable(message.replyToMessage())
                    .map(Message::text)
                    .ifPresent(text -> sb.append(text).append(". "));
            sb.append(message.text());

            GptRequest request = GptRequest.of("gpt-3.5-turbo",
                    List.of(GptRequest.Message.of("user", sb.toString())));

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-RapidAPI-Key", rapidApiKey);
            headers.set("X-RapidAPI-Host", "openai80.p.rapidapi.com");

            HttpEntity<GptRequest> entity = new HttpEntity<>(request, headers);
            try {
                ResponseEntity<GtpResponse> resp = rest.exchange("https://openai80.p.rapidapi.com/chat/completions",
                        HttpMethod.POST, entity, GtpResponse.class);

                if (!resp.getStatusCode().equals(HttpStatus.OK)) {
                    yesNoService.answer(message, chatId);
                    return;
                }

                Optional.ofNullable(resp.getBody())
                        .map(GtpResponse::getChoices)
                        .stream().flatMap(Collection::stream)
                        .map(GtpResponse.GptMessage::getMessage)
                        .map(GtpResponse.GptMessage.GptContent::getContent)
                        .findFirst()
                        .ifPresentOrElse(text -> bot.execute(new SendMessage(chatId, text)),
                                () -> yesNoService.answer(message, chatId));
            } catch (Exception ex) {
                log.error("Exception while communication to ChatGPT", ex);
                yesNoService.answer(message, chatId);
            }
        }
    }
}
