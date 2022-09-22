package com.coolguys.bot.repository;

import com.coolguys.bot.entity.GuardDepartmentEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Deprecated
@Repository
public interface GuardDepartmentRepository extends CrudRepository<GuardDepartmentEntity, Long> {

    Optional<GuardDepartmentEntity> findByChatId(Long chatId);
}
