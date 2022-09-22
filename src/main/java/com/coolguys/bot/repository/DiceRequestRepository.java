package com.coolguys.bot.repository;

import com.coolguys.bot.entity.DiceRequestEntity;
import com.coolguys.bot.entity.UserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Deprecated
@Repository
public interface DiceRequestRepository extends CrudRepository<DiceRequestEntity, Long> {
    List<DiceRequestEntity> findAllByUserAndChatIdAndDateGreaterThan(UserEntity user, Long chatId, LocalDateTime date);
}
