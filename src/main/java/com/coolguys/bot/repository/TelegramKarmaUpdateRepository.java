package com.coolguys.bot.repository;

import com.coolguys.bot.entity.TelegramKarmaUpdateEntity;
import com.coolguys.bot.entity.TelegramUserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TelegramKarmaUpdateRepository extends CrudRepository<TelegramKarmaUpdateEntity, Long> {

    List<TelegramKarmaUpdateEntity> findAllByOriginUserIdAndTargetUserAndTypeAndDateGreaterThan(Long originUserId,
                                                                                                TelegramUserEntity targetUser,
                                                                                                String type,
                                                                                                LocalDateTime date);
}
