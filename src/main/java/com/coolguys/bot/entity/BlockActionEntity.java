package com.coolguys.bot.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import java.time.LocalDateTime;

import static javax.persistence.GenerationType.IDENTITY;

@Data
@Entity(name = "block_action")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BlockActionEntity {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "acc_id", referencedColumnName = "id")
    private ChatAccountEntity acc;

    @Column(name = "expires")
    private LocalDateTime expires;

    @Column(name = "type")
    private String type;
}
