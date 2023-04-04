package com.coolguys.bot.repository;

import com.coolguys.bot.entity.TelegramMessageEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TelegramMessageRepository extends CrudRepository<TelegramMessageEntity, Long> {
    List<TelegramMessageEntity> findAllByDateGreaterThanAndChatId(LocalDateTime from, Long chatId);

    @Query("select tm from telegram_message tm where tm.chatId = :chatId and tm.user.id in :ids and tm.date > :from")
    List<TelegramMessageEntity> findByChatIdAndUserIdsAndDate(@Param("chatId") Long chatId,
                                                              @Param("ids") List<Long> ids,
                                                              @Param("from") LocalDateTime from);

    @Query("select tm from telegram_message tm where tm.chatId = :chatId and tm.user.id = :id and tm.date > :from")
    List<TelegramMessageEntity> findByUserId(@Param("chatId") Long chatId,
                                             @Param("id") Long id,
                                             @Param("from") LocalDateTime from);
}
