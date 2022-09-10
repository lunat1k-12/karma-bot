package com.coolguys.bot.repository;

import com.coolguys.bot.entity.CasinoEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CasinoRepository extends CrudRepository<CasinoEntity, Long> {

    Optional<CasinoEntity> findByChatId(Long chatId);
}
