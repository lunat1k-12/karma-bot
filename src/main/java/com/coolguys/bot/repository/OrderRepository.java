package com.coolguys.bot.repository;

import com.coolguys.bot.entity.OrderEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Deprecated
@Repository
public interface OrderRepository extends CrudRepository<OrderEntity, Long> {
    List<OrderEntity> findAllByChatIdAndTypeAndStageIsNot(Long chatId, String type, String stage);

    List<OrderEntity> findAllByChatIdAndStageAndOriginUserIdAndType(Long chatId, String stage, Long originUserId, String type);

    @Query("select o from order_request o where o.id =" +
            " (select max(om.id) from order_request om where om.type='drop_drugs' " +
            "and om.originUserId = :userId " +
            "and om.chatId = :chatId)")
    Optional<OrderEntity> findLastDrugDrop(@Param("chatId") Long chatId,
                                           @Param("userId") Long originUserId);
}
