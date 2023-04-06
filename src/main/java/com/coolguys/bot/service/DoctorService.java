package com.coolguys.bot.service;

import com.coolguys.bot.dto.BlockActionDto;
import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.QueryDataDto;
import com.coolguys.bot.dto.Role;
import com.coolguys.bot.dto.RoleType;
import com.coolguys.bot.mapper.BlockActionMapper;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.mapper.RoleMapper;
import com.coolguys.bot.repository.BlockActionRepository;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.repository.RoleRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendSticker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorService {

    public static final String SICK_STICKER = "CAACAgIAAxkBAAIKTGQuygNQ4pfZK1218iWxSf7V6k4iAAIYAQACIjeOBJxu63pSIsQmLwQ";
    private final TelegramBot bot;
    private final KeyboardService keyboardService;
    private final StealService stealService;
    private final BlockActionRepository blockActionRepository;
    private final BlockActionMapper blockActionMapper;
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final UserService userService;

    public void processPatientSelection(ChatAccount acc) {
        log.info("Doctor action");
        if (stealService.isInJail(acc)) {
            bot.execute(new SendMessage(acc.getChat().getId(), "Полегше лікар, ти сидиш!"));
            log.info("Doctor is in jail");
            return;
        }

        List<BlockActionDto> blockActions = blockActionRepository.findAllAccIdAndTypeAndDate(acc.getId(),
                        BlockActionDto.BlockType.DOCTOR_DISEASE.getValue(), LocalDateTime.now())
                .stream()
                .map(blockActionMapper::toDto)
                .collect(Collectors.toList());

        if (!blockActions.isEmpty()) {
            bot.execute(new SendMessage(acc.getChat().getId(), "Не так часто Док"));
            log.info("Doctor should wait for action");
            return;
        }

        bot.execute(new SendMessage(acc.getChat().getId(), "Обери пацієнта:")
                .parseMode(ParseMode.HTML)
                .replyMarkup(keyboardService
                        .getTargetAccSelectionPersonKeyboard(acc.getChat().getId(), acc.getId(), QueryDataDto.DOCTOR_DISEASE_TYPE)));
        log.info("Keyboard have sent for doctor");
    }

    public void processDocSelection(ChatAccount acc, QueryDataDto dto, Integer messageId) {
        if ("0".equals(dto.getOption())) {
            bot.execute(new SendMessage(acc.getChat().getId(), "Лікар одумався"));
            keyboardService.deleteOrUpdateKeyboardMessage(acc.getChat().getId(), messageId);
            log.info("Disease canceled");
            return;
        }

        ChatAccount targetChatAcc = chatAccountMapper.toDto(chatAccountRepository.findById(Long.parseLong(dto.getOption())).orElseThrow());
        Role role = roleRepository.findByAccountId(targetChatAcc.getId(), LocalDateTime.now())
                .map(roleMapper::toDto)
                .orElse(null);

        if (role != null && RoleType.DOCTOR.equals(role.getRole())) {
            bot.execute(new SendMessage(acc.getChat().getId(), "не можна заразити іншого лікаря"));
            keyboardService.deleteOrUpdateKeyboardMessage(acc.getChat().getId(), messageId);
            return;
        }

        BlockActionDto sick = BlockActionDto.builder()
                .expires(LocalDateTime.now().plusHours(3))
                .type(BlockActionDto.BlockType.SICK)
                .acc(targetChatAcc)
                .build();

        blockActionRepository.save(blockActionMapper.toEntity(sick));
        keyboardService.deleteOrUpdateKeyboardMessage(acc.getChat().getId(), messageId);
        bot.execute(new SendMessage(acc.getChat().getId(), String.format("@%s захворів(ла)", targetChatAcc.getUser().getUsername())));
        bot.execute(new SendSticker(acc.getChat().getId(), SICK_STICKER));
        log.info("New SICK action added");

        BlockActionDto actionBlock = BlockActionDto.builder()
                .expires(LocalDateTime.now().plusHours(3))
                .type(BlockActionDto.BlockType.DOCTOR_DISEASE)
                .acc(acc)
                .build();
        blockActionRepository.save(blockActionMapper.toEntity(actionBlock));
    }

    public void checkMessageSickReply(ChatAccount originUser, Message message) {
        if (message.replyToMessage() == null || message.replyToMessage().from().isBot()) {
            return;
        }

        if (!blockActionRepository.findAllAccIdAndTypeAndDate(
                originUser.getId(), BlockActionDto.BlockType.SICK.getValue(), LocalDateTime.now()).isEmpty()) {
            log.info("{} already sick", originUser.getUser().getUsername());
            return;
        }

        ChatAccount target = userService.loadChatAccount(message.replyToMessage());
        RoleType role = roleRepository.findByAccountId(originUser.getId(), LocalDateTime.now())
                .map(roleMapper::toDto)
                .map(Role::getRole)
                .orElse(null);
        if (!blockActionRepository.findAllAccIdAndTypeAndDate(
                target.getId(), BlockActionDto.BlockType.SICK.getValue(), LocalDateTime.now()).isEmpty() &&
                !RoleType.DOCTOR.equals(role)
        ) {
            BlockActionDto sick = BlockActionDto.builder()
                    .expires(LocalDateTime.now().plusHours(3))
                    .type(BlockActionDto.BlockType.SICK)
                    .acc(originUser)
                    .build();
            blockActionRepository.save(blockActionMapper.toEntity(sick));
            bot.execute(new SendMessage(originUser.getChat().getId(), String.format("@%s заразився від @%s",
                    originUser.getUser().getUsername(),
                    target.getUser().getUsername())));
        }
    }
}
