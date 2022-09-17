package com.coolguys.bot.repository;

import com.coolguys.bot.entity.ChatAccountEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatAccountRepository extends CrudRepository<ChatAccountEntity, Long> {
}
