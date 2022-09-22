package com.coolguys.bot.repository;

import com.coolguys.bot.entity.TelegramUserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramUserRepository extends CrudRepository<TelegramUserEntity, Long> {
}
