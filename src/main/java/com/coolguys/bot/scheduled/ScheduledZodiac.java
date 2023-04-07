package com.coolguys.bot.scheduled;

import com.coolguys.bot.conf.BotConfig;
import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.TelegramChat;
import com.coolguys.bot.dto.TelegramUser;
import com.coolguys.bot.dto.TranslateRequest;
import com.coolguys.bot.dto.TranslateResponseDto;
import com.coolguys.bot.dto.Zodiac;
import com.coolguys.bot.dto.ZodiacResponseDto;
import com.coolguys.bot.mapper.TelegramChatMapper;
import com.coolguys.bot.repository.TelegramChatRepository;
import com.coolguys.bot.service.UserService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledZodiac {

    private final TelegramChatRepository telegramChatRepository;
    private final TelegramChatMapper telegramChatMapper;
    private final UserService userService;
    private final BotConfig botConfig;
    private final TelegramBot bot;

    private final RestTemplate rest = new RestTemplateBuilder()
            .setConnectTimeout(Duration.of(1, ChronoUnit.MINUTES))
            .setReadTimeout(Duration.of(1, ChronoUnit.MINUTES))
            .build();

    @Scheduled(cron = "00 20 09 * * *", zone = "Europe/Kiev")
    @Async
    public void checkZodiac() {
        telegramChatRepository.findAllActive().stream()
                .map(telegramChatMapper::toDto)
                .forEach(this::processZodiacChat);
    }

    private void processZodiacChat(TelegramChat chat) {
        log.info("Process zodiac for {}", chat.getName());
        Set<Zodiac> zodiacs = userService.findActiveAccByChatId(chat.getId()).stream()
                .map(ChatAccount::getUser)
                .map(TelegramUser::getZodiac)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (zodiacs.isEmpty()) {
            log.info("not zodiacts for {}", chat.getName());
            return;
        }

        for (Zodiac zodiac : zodiacs) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                headers.set("X-RapidAPI-Key", botConfig.getRapidApiKey());
                headers.set("X-RapidAPI-Host", "horoscope-api.p.rapidapi.com");
                HttpEntity<?> entity = new HttpEntity<>(headers);
                var resp = rest.exchange("https://horoscope-api.p.rapidapi.com/pt/{sign}",
                        HttpMethod.GET, entity, ZodiacResponseDto.class, zodiac.getName());

                String horoscope = translate(resp.getBody());
                if (horoscope != null) {
                    bot.execute(new SendMessage(chat.getId(), horoscope));
                }
            } catch(Exception ex) {
                log.error("Exception while trying to get horoscope for {}", zodiac.getName());
            }
        }
    }

    private String translate(ZodiacResponseDto horoscope) {
        if (horoscope == null) {
            return null;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.set("X-RapidAPI-Key", botConfig.getRapidApiKey());
        headers.set("X-RapidAPI-Host", "google-translator9.p.rapidapi.com");

        TranslateRequest request = TranslateRequest.builder()
                .q(horoscope.getTitle() + "\n" + horoscope.getText())
                .format("text")
                .source("pt")
                .target("uk")
                .build();
        HttpEntity<TranslateRequest> entity = new HttpEntity<>(request, headers);
        var resp = rest.exchange("https://google-translator9.p.rapidapi.com/v2",
                HttpMethod.POST, entity, TranslateResponseDto.class);

        return Optional.ofNullable(resp.getBody())
                .map(TranslateResponseDto::getData)
                .stream().map(TranslateResponseDto.TranslationData::getTranslations)
                .flatMap(Collection::stream)
                .map(TranslateResponseDto.TranslationData.Translation::getTranslatedText)
                .findFirst()
                .orElse(null);
    }
}
