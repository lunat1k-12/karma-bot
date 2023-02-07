package com.coolguys.bot.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import static javax.persistence.GenerationType.IDENTITY;

@Data
@Entity(name = "poll")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PollEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "message_id", referencedColumnName = "id")
    private TelegramMessageEntity message;

    @OneToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "chat_id", referencedColumnName = "id")
    private TelegramChatEntity chat;

    @Column(name = "telegram_poll_id")
    private String telegramPollId;

    @Column(name = "status")
    private String status;
}
