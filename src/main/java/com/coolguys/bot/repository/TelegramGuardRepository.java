package com.coolguys.bot.repository;

import com.coolguys.bot.entity.TelegramGuardEntity;
import com.coolguys.bot.entity.TelegramUserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TelegramGuardRepository extends CrudRepository<TelegramGuardEntity, Long> {

    List<TelegramGuardEntity> findByUserAndChatIdAndExpiresAfter(TelegramUserEntity user, Long chatId, LocalDateTime date);
    void deleteByUserAndChatId(TelegramUserEntity user, Long chatId);
}
