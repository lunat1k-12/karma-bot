package com.coolguys.bot.repository;

import com.coolguys.bot.entity.OrderEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends CrudRepository<OrderEntity, Long> {
    List<OrderEntity> findAllByChatIdAndTypeAndStageIsNot(Long chatId, String type, String stage);

    List<OrderEntity> findAllByChatIdAndStageAndOriginUserIdAndType(Long chatId, String stage, Long originUserId, String type);

    List<OrderEntity> findAllByChatIdAndOriginUserIdAndType(Long chatId, Long originUserId, String type);
}
