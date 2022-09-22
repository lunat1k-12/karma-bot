package com.coolguys.bot.repository;

import com.coolguys.bot.entity.TelegramMessageEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TelegramMessageRepository extends CrudRepository<TelegramMessageEntity, Long> {
    List<TelegramMessageEntity> findAllByDateGreaterThanAndChatId(LocalDateTime from, Long chatId);
}
