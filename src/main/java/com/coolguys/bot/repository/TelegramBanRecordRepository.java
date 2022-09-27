package com.coolguys.bot.repository;

import com.coolguys.bot.entity.TelegramBanRecordEntity;
import com.coolguys.bot.entity.TelegramUserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TelegramBanRecordRepository extends CrudRepository<TelegramBanRecordEntity, Long> {
    List<TelegramBanRecordEntity> findByUserAndChatIdAndExpiresAfter(TelegramUserEntity user, Long chatId, LocalDateTime date);

    @Query("select ban from telegram_ban_record ban where ban.chatId = :chatId and ban.expires > :date")
    List<TelegramBanRecordEntity> findByChatIdAndDate(@Param("chatId") Long chatId,
                                                      @Param("date") LocalDateTime date);
}
