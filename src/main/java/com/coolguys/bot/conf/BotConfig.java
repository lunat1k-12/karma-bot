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

    private String username;
    private String token;
    private String creditCommand;
    private String autoReplyCommand;
    private String removePlayBanCommand;
    private String buyGuardCommand;
    private String buyCasinoCommand;
    private String buyPoliceCommand;
    private String myStatsCommand;
    private String buyGuardDepartmentCommand;
    private String selectRoleCommand;
    private String roleActionsCommand;
    private String infoCommand;
    private String languageDetectorApiKey;
    private String rapidApiKey;
    private String setZodiacCommand;
    private String awsMetricNamespace;
    private String awsForwardNamespace;
    private String awsLanguageNamespace;
}
