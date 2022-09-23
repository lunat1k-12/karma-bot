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
}
