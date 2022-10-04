package com.coolguys.bot.repository;

import com.coolguys.bot.entity.InvestigateActionEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InvestigateActionRepository extends CrudRepository<InvestigateActionEntity, Long> {

    @Query("select a from investigate_action a where a.account.id = :accId and a.expires > :date")
    List<InvestigateActionEntity> findByAccIdAndExpiresAfter(@Param("accId") Long accId,
                                                             @Param("date") LocalDateTime date);
}
