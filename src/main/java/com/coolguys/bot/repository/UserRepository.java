package com.coolguys.bot.repository;

import com.coolguys.bot.entity.UserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsernameAndChatId(String username, Long chatId);
    List<UserEntity> findByChatId(Long chatId);
}
