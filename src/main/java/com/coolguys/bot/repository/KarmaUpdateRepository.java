package com.coolguys.bot.repository;

import com.coolguys.bot.entity.KarmaUpdateEntity;
import com.coolguys.bot.entity.UserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Deprecated
@Repository
public interface KarmaUpdateRepository extends CrudRepository<KarmaUpdateEntity, Long> {

    List<KarmaUpdateEntity> findAllByOriginUserIdAndTargetUserAndTypeAndDateGreaterThan(Long originUserId,
                                                                                        UserEntity targetUser,
                                                                                        String type,
                                                                                        LocalDateTime date);
}
