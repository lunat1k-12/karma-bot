package com.coolguys.bot.dto;

import lombok.Data;

import java.util.List;

@Data
public class TranslateResponseDto {
    private TranslationData data;
    @Data
    public static class TranslationData {
        private List<Translation> translations;

        @Data
        public static class Translation {
            private String translatedText;
        }
    }
}
