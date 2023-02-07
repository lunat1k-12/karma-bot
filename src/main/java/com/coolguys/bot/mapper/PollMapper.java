package com.coolguys.bot.mapper;

import com.coolguys.bot.dto.Poll;
import com.coolguys.bot.dto.PollStatus;
import com.coolguys.bot.entity.PollEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PollMapper implements EntityToDtoMapper<PollEntity, Poll> {

    private final TelegramMessageMapper telegramMessageMapper;
    private final TelegramChatMapper telegramChatMapper;

    @Override
    public Poll toDto(PollEntity entity) {
        return Poll.builder()
                .status(PollStatus.getById(entity.getStatus()))
                .message(telegramMessageMapper.toDto(entity.getMessage()))
                .chat(telegramChatMapper.toDto(entity.getChat()))
                .id(entity.getId())
                .telegramPollId(entity.getTelegramPollId())
                .build();
    }

    @Override
    public PollEntity toEntity(Poll dto) {
        return PollEntity.builder()
                .chat(telegramChatMapper.toEntity(dto.getChat()))
                .message(telegramMessageMapper.toEntity(dto.getMessage()))
                .status(dto.getStatus().getId())
                .id(dto.getId())
                .telegramPollId(dto.getTelegramPollId())
                .build();
    }
}
