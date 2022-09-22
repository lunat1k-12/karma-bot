package com.coolguys.bot.repository;

import com.coolguys.bot.entity.ChatAccountEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatAccountRepository extends CrudRepository<ChatAccountEntity, Long> {

    @Query("select ca from chat_account ca where ca.user.id = :userId and ca.chat.id = :chatId")
    Optional<ChatAccountEntity> findByUserIdAndChatId(@Param("userId") Long userId,
                                                      @Param("chatId") Long chatId);

    @Query("select ca from chat_account ca where ca.chat.id = :chatId")
    List<ChatAccountEntity> findByChatId(@Param("chatId") Long chatId);
}
