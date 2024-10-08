package com.coolguys.bot.service.metrics;

import com.coolguys.bot.conf.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

@Service
@Slf4j
public class AwsMetricsService {

    private final CloudWatchClient cloudWatchClient;
    private final BotConfig botConfig;

    public AwsMetricsService(BotConfig config) {
        this.cloudWatchClient = CloudWatchClient.builder()
                .build();
        this.botConfig = config;
    }

    public void sendMessageMetric(String author) {

        try {
            MetricDatum metric = MetricDatum.builder()
                    .metricName("MessageCount")
                    .value(1D)
                    .dimensions(Dimension.builder()
                            .name("Author")
                            .value(author)
                            .build())
                    .unit(StandardUnit.COUNT)
                    .build();

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace(botConfig.getAwsMetricNamespace())
                    .metricData(metric)
                    .build();

            cloudWatchClient.putMetricData(request);
        } catch (Exception ex) {
            log.info("Failed to send message metric", ex);
        }
    }

    public void sendForwardMetric(String channelTitle) {
        try {
            MetricDatum metric = MetricDatum.builder()
                    .metricName("ForwardCount")
                    .value(1D)
                    .dimensions(Dimension.builder()
                            .name("ChannelName")
                            .value(channelTitle)
                            .build())
                    .unit(StandardUnit.COUNT)
                    .build();

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace(botConfig.getAwsForwardNamespace())
                    .metricData(metric)
                    .build();

            cloudWatchClient.putMetricData(request);
        } catch (Exception ex) {
            log.info("Failed to send forward metric", ex);
        }
    }

    public void sendLanguageMetric(String lang) {
        if (lang == null) {
            return;
        }

        try {
            MetricDatum metric = MetricDatum.builder()
                    .metricName("LanguageCount")
                    .value(1D)
                    .dimensions(Dimension.builder()
                            .name("LanguageName")
                            .value(lang)
                            .build())
                    .unit(StandardUnit.COUNT)
                    .build();

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace(botConfig.getAwsLanguageNamespace())
                    .metricData(metric)
                    .build();

            cloudWatchClient.putMetricData(request);
        } catch (Exception ex) {
            log.info("Failed to send language metric", ex);
        }
    }

    public void sendReactionMetric(String author) {
        if (author == null) {
            return;
        }

        try {
            MetricDatum metric = MetricDatum.builder()
                    .metricName("ReactionCount")
                    .value(1D)
                    .dimensions(Dimension.builder()
                            .name("ReactionAuthor")
                            .value(author)
                            .build())
                    .unit(StandardUnit.COUNT)
                    .build();

            PutMetricDataRequest request = PutMetricDataRequest.builder()
                    .namespace(botConfig.getAwsMetricNamespace())
                    .metricData(metric)
                    .build();

            cloudWatchClient.putMetricData(request);
        } catch (Exception ex) {
            log.info("Failed to send language metric", ex);
        }
    }
}
