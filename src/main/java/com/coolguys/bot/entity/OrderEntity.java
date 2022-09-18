package com.coolguys.bot.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

import static javax.persistence.GenerationType.IDENTITY;

@Data
@Entity(name = "order_request")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Deprecated
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long originUserId;

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
    @JoinColumn(name = "target_user_id", referencedColumnName = "id")
    private UserEntity targetUser;
}
