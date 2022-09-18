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
import java.time.LocalDateTime;

import static javax.persistence.GenerationType.IDENTITY;

@Data
@Entity(name = "telegram_karma_update")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TelegramKarmaUpdateEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long originUserId;

    @Column(name = "type")
    private String type;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "date")
    private LocalDateTime date;

    @OneToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "target_user_id", referencedColumnName = "id")
    private TelegramUserEntity targetUser;
}
