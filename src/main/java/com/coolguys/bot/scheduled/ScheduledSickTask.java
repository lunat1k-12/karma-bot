package com.coolguys.bot.scheduled;

import com.coolguys.bot.dto.BlockActionDto;
import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.Role;
import com.coolguys.bot.dto.RoleType;
import com.coolguys.bot.dto.TelegramChat;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.mapper.RoleMapper;
import com.coolguys.bot.mapper.TelegramChatMapper;
import com.coolguys.bot.repository.BlockActionRepository;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.repository.RoleRepository;
import com.coolguys.bot.repository.TelegramChatRepository;
import com.coolguys.bot.service.UserService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledSickTask {

    private final TelegramChatRepository telegramChatRepository;
    private final TelegramChatMapper telegramChatMapper;
    private final UserService userService;
    private final BlockActionRepository blockActionRepository;
    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;
    private final TelegramBot bot;

    @Scheduled(cron = "00 17 * * * *", zone = "Europe/Kiev")
    @Async
    public void checkDisease() {
        telegramChatRepository.findAllActive().stream()
                .map(telegramChatMapper::toDto)
                .forEach(this::checkChatDisease);
    }

    private void checkChatDisease(TelegramChat chat) {

        List<Role> activeRoles = roleRepository.findByChatAndRoleType(chat.getId(), RoleType.DOCTOR.getId(), LocalDateTime.now())
                .stream()
                .map(roleMapper::toDto)
                .collect(Collectors.toList());
        userService.findActiveAccByChatId(chat.getId())
                .forEach(acc -> checkAccDisease(acc, activeRoles));
    }

    private void checkAccDisease(ChatAccount acc, List<Role> activeRoles) {
        if (blockActionRepository.findAllAccIdAndTypeAndDate(
                acc.getId(), BlockActionDto.BlockType.SICK.getValue(), LocalDateTime.now()).isEmpty()) {
            log.info("{} is healthy", acc.getUser().getUsername());
            return;
        }
        log.info("{} is sick", acc.getUser().getUsername());
        int creditsForHealth = 30;

        if (!activeRoles.isEmpty()) {
            activeRoles.forEach(r -> {
                r.getAccount().plusCredit(30);
                chatAccountRepository.save(chatAccountMapper.toEntity(r.getAccount()));
                log.info("{} got medical credits", r.getAccount().getUser().getUsername());
            });
            creditsForHealth = 30 * activeRoles.size();
        }

        acc.minusCredit(creditsForHealth);
        chatAccountRepository.save(chatAccountMapper.toEntity(acc));
        bot.execute(new SendMessage(acc.getChat().getId(), String.format("@%s витрачає %s кредитів на лікарів",
                acc.getUser().getUsername(), creditsForHealth)));
    }
}
