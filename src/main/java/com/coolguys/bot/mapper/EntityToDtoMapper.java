package com.coolguys.bot.mapper;

public interface EntityToDtoMapper<E, D> {

    D toDto(E entity);
    E toEntity(D dto);
}
