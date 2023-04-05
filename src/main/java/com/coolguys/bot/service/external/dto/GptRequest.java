package com.coolguys.bot.service.external.dto;

import lombok.Value;

import java.util.List;

@Value(staticConstructor = "of")
public class GptRequest {
    String model;
    List<Message> messages;

    @Value(staticConstructor = "of")
    public static class Message {
        String role;
        String content;
    }
}
