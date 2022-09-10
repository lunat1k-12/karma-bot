package com.coolguys.bot.repository;

import com.coolguys.bot.entity.UpdateNoteEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UpdateNoteRepository extends CrudRepository<UpdateNoteEntity, Long> {

    List<UpdateNoteEntity> findAllByStatus(String status);
}
