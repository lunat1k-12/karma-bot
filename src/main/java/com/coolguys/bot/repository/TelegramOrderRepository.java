package com.coolguys.bot.repository;

import com.coolguys.bot.entity.TelegramOrderEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TelegramOrderRepository extends CrudRepository<TelegramOrderEntity, Long> {

    List<TelegramOrderEntity> findAllByChatIdAndTypeAndStageIsNot(Long chatId, String type, String stage);

    List<TelegramOrderEntity> findAllByChatIdAndStageAndOriginAccIdAndType(Long chatId, String stage, Long originUserId, String type);

    @Query("select o from telegram_order_request o where o.id =" +
            " (select max(om.id) from telegram_order_request om where om.type='drop_drugs' " +
            "and om.originAccId = :userId " +
            "and om.chatId = :chatId)")
    Optional<TelegramOrderEntity> findLastDrugDrop(@Param("chatId") Long chatId,
                                                   @Param("userId") Long originAccId);
}
