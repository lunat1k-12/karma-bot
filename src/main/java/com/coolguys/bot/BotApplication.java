package com.coolguys.bot;

import com.coolguys.bot.listner.MessagesListener;
import com.pengrad.telegrambot.TelegramBot;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@RequiredArgsConstructor
@EnableScheduling
public class BotApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(BotApplication.class, args);
    }

    private final MessagesListener listener;

    @Override
    public void run(String... args) throws Exception {

    }
}
// docker run --name chat -p5434:5432 -e POSTGRES_PASSWORD=1234 -e POSTGRES_USER=chat -e POSTGRES_DB=chat -d postgres
