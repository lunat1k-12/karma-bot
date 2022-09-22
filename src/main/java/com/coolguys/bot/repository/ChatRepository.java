package com.coolguys.bot.repository;

import com.coolguys.bot.entity.ChatEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Deprecated
@Repository
public interface ChatRepository extends CrudRepository<ChatEntity, Long> {
    Optional<ChatEntity> findByTelegramId(Long telegramId);
}
