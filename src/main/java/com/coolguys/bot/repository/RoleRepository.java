package com.coolguys.bot.repository;

import com.coolguys.bot.entity.RoleEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends CrudRepository<RoleEntity, Long> {

    @Query("select r from role r where r.account.id = :accId")
    Optional<RoleEntity> findByAccountId(@Param("accId") Long accId);

    @Query("select r from role r where r.account.chat.id = :chatId and r.role = :type and r.expires > :date")
    List<RoleEntity> findByChatAndRoleType(@Param("chatId") Long chatId,
                                           @Param("type") String type,
                                           @Param("date") LocalDateTime date);
}
