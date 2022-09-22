package com.coolguys.bot.repository;

import com.coolguys.bot.entity.PoliceDepartmentEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Deprecated
@Repository
public interface PoliceDepartmentRepository extends CrudRepository<PoliceDepartmentEntity, Long> {

    Optional<PoliceDepartmentEntity> findByChatId(Long chatId);
}
