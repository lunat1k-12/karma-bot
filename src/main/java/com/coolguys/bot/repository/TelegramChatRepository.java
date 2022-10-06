package com.coolguys.bot.repository;

import com.coolguys.bot.entity.TelegramChatEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TelegramChatRepository extends CrudRepository<TelegramChatEntity, Long> {

    @Query("select chat from telegram_chat chat where chat.active = true")
    List<TelegramChatEntity> findAllActive();
}
