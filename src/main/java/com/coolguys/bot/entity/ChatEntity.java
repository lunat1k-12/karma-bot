package com.coolguys.bot.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import static javax.persistence.GenerationType.IDENTITY;

@Data
@Entity(name = "chats")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    @Column(name = "telegram_id")
    private Long telegramId;
    @Column(name = "name")
    private String name;
}
