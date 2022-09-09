package com.coolguys.bot.service;

import com.coolguys.bot.dto.UpdateNote;
import com.coolguys.bot.dto.UpdateNoteStatus;
import com.coolguys.bot.entity.UserEntity;
import com.coolguys.bot.listener.MessagesListener;
import com.coolguys.bot.mapper.UpdateNoteMapper;
import com.coolguys.bot.repository.UpdateNoteRepository;
import com.coolguys.bot.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class UpdateNoteService {

    private final UpdateNoteRepository updateNoteRepository;
    private final UpdateNoteMapper updateNoteMapper;
    private final MessagesListener messagesListener;
    private final UserRepository userRepository;
    public void processNotes() {
        Set<Long> chatIds = StreamSupport.stream(userRepository.findAll().spliterator(), false)
                .map(UserEntity::getChatId)
                .collect(Collectors.toSet());

        updateNoteRepository.findAllByStatus(UpdateNoteStatus.WAITING.getId())
                .stream()
                .map(updateNoteMapper::toDto)
                .forEach(note -> processMessage(note, chatIds));
    }

    private void processMessage(UpdateNote note, Set<Long> chatIds) {
        chatIds.forEach(chatId -> messagesListener.sendMessage(chatId, note.getMessage()));
        note.setStatus(UpdateNoteStatus.SENT);
        updateNoteRepository.save(updateNoteMapper.toEntity(note));
    }
}
