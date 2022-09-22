package com.coolguys.bot.repository;

import com.coolguys.bot.entity.TelegramCasinoEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TelegramCasinoRepository extends CrudRepository<TelegramCasinoEntity, Long> {

    Optional<TelegramCasinoEntity> findByChatId(Long chatId);
}
