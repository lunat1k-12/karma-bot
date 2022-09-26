package com.coolguys.bot.repository;

import com.coolguys.bot.entity.RoleEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends CrudRepository<RoleEntity, Long> {

    @Query("select r from role r where r.account.id = :accId")
    Optional<RoleEntity> findByAccountId(@Param("accId") Long accId);
}
