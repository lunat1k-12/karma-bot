package com.coolguys.bot.repository;

import com.coolguys.bot.entity.ChatMessageEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Deprecated
@Repository
public interface ChatMessageRepository extends CrudRepository<ChatMessageEntity, Long> {

    List<ChatMessageEntity> findAllByDateGreaterThanAndChatId(LocalDateTime from, Long chatId);
}
