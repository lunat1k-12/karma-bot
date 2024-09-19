package com.coolguys.bot.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Data
@Entity(name = "telegram_chat")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TelegramChatEntity {
    @Id
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "premium")
    private Boolean premium;

    @Column(name = "active")
    private Boolean active;
}
