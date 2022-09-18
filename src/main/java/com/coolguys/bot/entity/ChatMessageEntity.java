package com.coolguys.bot.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

import java.time.LocalDateTime;

import static javax.persistence.GenerationType.IDENTITY;

@Data
@Entity(name = "chat_message")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Deprecated
public class ChatMessageEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private UserEntity user;

    @Column(name = "message")
    private String message;

    @Column(name = "date")
    private LocalDateTime date;

    @Column(name = "chat_id")
    private Long chatId;
}
