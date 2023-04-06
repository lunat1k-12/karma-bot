package com.coolguys.bot.repository;

import com.coolguys.bot.entity.BlockActionEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BlockActionRepository extends CrudRepository<BlockActionEntity, Long> {

    @Query("select ba from block_action ba where ba.acc.id = :accId and ba.type = :type and ba.expires > :date")
    List<BlockActionEntity> findAllAccIdAndTypeAndDate(@Param("accId") Long accId,
                                                       @Param("type") String type,
                                                       @Param("date") LocalDateTime date);
}
