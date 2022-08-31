package com.coolguys.bot.repository;

import com.coolguys.bot.entity.ChatEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
public interface ChatRepository extends CrudRepository<ChatEntity, Long> {

    @Transactional
    void deleteByTelegramId(Long telegramId);
}
