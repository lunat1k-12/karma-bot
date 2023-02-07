package com.coolguys.bot.repository;

import com.coolguys.bot.entity.PollEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PollRepository extends CrudRepository<PollEntity, Long> {

    @Query("select p from poll p where p.chat.id = :chatId and p.status = 'in_progress'")
    List<PollEntity> findActiveByChatId(@Param("chatId") Long chatId);
}
