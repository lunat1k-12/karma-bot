package com.coolguys.bot.repository;

import com.coolguys.bot.entity.TelegramDiceRequestEntity;
import com.coolguys.bot.entity.TelegramUserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TelegramDiceRequestRepository extends CrudRepository<TelegramDiceRequestEntity, Long> {

    List<TelegramDiceRequestEntity> findAllByUserAndChatIdAndDateGreaterThan(TelegramUserEntity user, Long chatId, LocalDateTime date);
}
