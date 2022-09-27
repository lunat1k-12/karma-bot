package com.coolguys.bot.repository;

import com.coolguys.bot.entity.GainMoneyActionEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GainMoneyActionRepository extends CrudRepository<GainMoneyActionEntity, Long> {

    @Query("select action from gain_money_action action where action.account.id = :id and action.expires > :date")
    List<GainMoneyActionEntity> findAllByAccIdAndDate(@Param("id") Long id,
                                                      @Param("date") LocalDateTime date);
}
