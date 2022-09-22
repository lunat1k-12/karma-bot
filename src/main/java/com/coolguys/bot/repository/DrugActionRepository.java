package com.coolguys.bot.repository;

import com.coolguys.bot.entity.DrugActionEntity;
import com.coolguys.bot.entity.UserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Deprecated
@Repository
public interface DrugActionRepository extends CrudRepository<DrugActionEntity, Long> {

    List<DrugActionEntity> findByUserAndChatIdAndExpiresAfter(UserEntity user, Long chatId, LocalDateTime date);
}
