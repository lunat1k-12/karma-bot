package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.Role;
import com.coolguys.bot.dto.RoleType;
import com.coolguys.bot.entity.RoleEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleMapper implements EntityToDtoMapper<RoleEntity, Role> {

    private final ChatAccountMapper chatAccountMapper;

    @Override
    public Role toDto(RoleEntity entity) {
        return Role.builder()
                .role(RoleType.getById(entity.getRole()))
                .id(entity.getId())
                .account(chatAccountMapper.toDto(entity.getAccount()))
                .expires(entity.getExpires())
                .build();
    }

    @Override
    public RoleEntity toEntity(Role dto) {
        return RoleEntity.builder()
                .account(chatAccountMapper.toEntity(dto.getAccount()))
                .role(dto.getRole().getId())
                .expires(dto.getExpires())
                .id(dto.getId())
                .build();
    }
}
