package com.coolguys.bot.repository;

import com.coolguys.bot.entity.TelegramPoliceDepartmentEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TelegramPoliceDepartmentRepository extends CrudRepository<TelegramPoliceDepartmentEntity, Long> {

    Optional<TelegramPoliceDepartmentEntity> findByChatId(Long chatId);
}
