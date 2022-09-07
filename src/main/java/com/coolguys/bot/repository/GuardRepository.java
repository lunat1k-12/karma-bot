package com.coolguys.bot.repository;

import com.coolguys.bot.entity.GuardEntity;
import com.coolguys.bot.entity.UserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface GuardRepository extends CrudRepository<GuardEntity, Long> {

    List<GuardEntity> findByUserAndChatIdAndExpiresAfter(UserEntity user, Long chatId, LocalDateTime date);
    void deleteByUser(UserEntity user);
}
