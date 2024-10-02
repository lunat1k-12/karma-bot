package com.coolguys.bot.service.external;

import com.coolguys.bot.conf.BotConfig;
import com.coolguys.bot.service.metrics.AwsMetricsService;
import com.detectlanguage.DetectLanguage;
import com.detectlanguage.Result;
import com.detectlanguage.errors.APIError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class LanguageDetectorService {

    private final AwsMetricsService awsMetricsService;

    public LanguageDetectorService(BotConfig botConfig, AwsMetricsService awsMetricsService) {
        log.info("Language API key: {}", botConfig.getLanguageDetectorApiKey());
        DetectLanguage.apiKey = botConfig.getLanguageDetectorApiKey();
        this.awsMetricsService = awsMetricsService;
    }

    public Language checkMessageLanguage(String message) {
        try {
            List<Result> results = DetectLanguage.detect(message);

            for (Result result : results) {
                log.info("Language: " + result.language);
                log.info("Is reliable: " + result.isReliable);
                log.info("Confidence: " + result.confidence);
            }

            Language lang = results.stream()
                    .filter(r -> r.isReliable)
                    .findFirst()
                    .map(r -> Language.getByCode(r.language))
                    .orElse(Language.NA);

            awsMetricsService.sendLanguageMetric(lang.name());
            return lang;

        } catch (APIError e) {
            log.error("Exception while Language detect", e);
        }

        return Language.NA;
    }
}
