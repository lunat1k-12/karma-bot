package com.coolguys.bot.conf;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ConfigurationProperties(prefix = "bot")
@Component
public class BotConfig {

    private String token;
}
