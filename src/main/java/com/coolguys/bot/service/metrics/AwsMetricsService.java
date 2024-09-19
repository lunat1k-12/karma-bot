package com.coolguys.bot.service.metrics;

import com.coolguys.bot.conf.BotConfig;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

@Service
public class AwsMetricsService {

    private final CloudWatchClient cloudWatchClient;
    private final BotConfig botConfig;

    public AwsMetricsService(BotConfig config) {
        this.cloudWatchClient = CloudWatchClient.builder()
                .build();
        this.botConfig = config;
    }

    public void sendMessageMetric(String author) {
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
    }

    public void sendForwardMetric(String channelTitle) {
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
    }
}
