package com.coolguys.bot.repository;

import com.coolguys.bot.entity.TelegramGuardDepartmentEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TelegramGuardDepartmentRepository extends CrudRepository<TelegramGuardDepartmentEntity, Long> {

    Optional<TelegramGuardDepartmentEntity> findByChatId(Long chatId);
}
