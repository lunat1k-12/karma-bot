package com.coolguys.bot.repository;

import com.coolguys.bot.entity.OrderEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends CrudRepository<OrderEntity, Long> {

    List<OrderEntity> findAllByChatIdAndStageIsNot(Long chatId, String stage);
    List<OrderEntity> findAllByChatIdAndStageAndOriginUserId(Long chatId, String stage, Long originUserId);
}
