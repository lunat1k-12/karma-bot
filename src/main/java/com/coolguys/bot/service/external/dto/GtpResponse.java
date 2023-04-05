package com.coolguys.bot.service.external.dto;

import lombok.Data;

import java.util.List;

@Data
public class GtpResponse {
    private String id;
    private String object;
    private String created;
    private String model;
    private List<GptMessage> choices;

    @Data
    public static class GptMessage {

        private GptContent message;

        @Data
        public static class GptContent {
            private String content;
        }
    }
}
