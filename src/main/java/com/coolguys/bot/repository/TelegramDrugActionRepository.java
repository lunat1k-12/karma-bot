package com.coolguys.bot.repository;

import com.coolguys.bot.entity.TelegramDrugActionEntity;
import com.coolguys.bot.entity.TelegramUserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TelegramDrugActionRepository extends CrudRepository<TelegramDrugActionEntity, Long> {
    List<TelegramDrugActionEntity> findByUserAndChatIdAndExpiresAfter(TelegramUserEntity user, Long chatId, LocalDateTime date);
}
