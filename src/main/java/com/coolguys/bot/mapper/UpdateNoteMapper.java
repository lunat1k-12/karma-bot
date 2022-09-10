package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.UpdateNote;
import com.coolguys.bot.dto.UpdateNoteStatus;
import com.coolguys.bot.entity.UpdateNoteEntity;
import org.springframework.stereotype.Component;

@Component
public class UpdateNoteMapper implements EntityToDtoMapper<UpdateNoteEntity, UpdateNote> {
    @Override
    public UpdateNote toDto(UpdateNoteEntity entity) {
        return UpdateNote.builder()
                .message(entity.getMessage())
                .status(UpdateNoteStatus.getById(entity.getStatus()))
                .id(entity.getId())
                .build();
    }

    @Override
    public UpdateNoteEntity toEntity(UpdateNote dto) {
        return UpdateNoteEntity.builder()
                .id(dto.getId())
                .message(dto.getMessage())
                .status(dto.getStatus().getId())
                .build();
    }
}
