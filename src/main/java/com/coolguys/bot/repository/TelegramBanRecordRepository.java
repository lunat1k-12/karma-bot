package com.coolguys.bot.repository;

import com.coolguys.bot.entity.TelegramBanRecordEntity;
import com.coolguys.bot.entity.TelegramUserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TelegramBanRecordRepository extends CrudRepository<TelegramBanRecordEntity, Long> {
    List<TelegramBanRecordEntity> findByUserAndChatIdAndExpiresAfter(TelegramUserEntity user, Long chatId, LocalDateTime date);
}
