package com.coolguys.bot.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import static jakarta.persistence.GenerationType.IDENTITY;

@Data
@Entity(name = "telegram_order_request")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TelegramOrderEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "acc_id")
    private Long originAccId;

    @Column(name = "type")
    private String type;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "stage")
    private String stage;

    @Column(name = "iterations_count")
    private Long iterationCount;

    @Column(name = "current_iteration")
    private Long currentIteration;

    @Column(name = "respond_message")
    private String respondMessage;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "target_acc_id", referencedColumnName = "id")
    private ChatAccountEntity targetAcc;

    @OneToOne
    @JoinColumn(name = "drug_action_id", referencedColumnName = "id")
    private TelegramDrugActionEntity drugAction;

    @Column(name = "sticker_id")
    private String stickerId;
}
