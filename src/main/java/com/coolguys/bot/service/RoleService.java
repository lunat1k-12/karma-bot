package com.coolguys.bot.service;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.QueryDataDto;
import com.coolguys.bot.dto.Role;
import com.coolguys.bot.dto.RoleType;
import com.coolguys.bot.entity.RoleEntity;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.mapper.RoleMapper;
import com.coolguys.bot.repository.RoleRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final TelegramBot bot;
    private final KeyboardService keyboardService;
    private final ChatAccountMapper chatAccountMapper;

    public Optional<Role> getAccRole(ChatAccount acc) {
        return roleRepository.findByAccountId(acc.getId())
                .map(roleMapper::toDto)
                .filter(role -> LocalDateTime.now().isBefore(role.getExpires()));
    }
    public void selectRoleRequest(ChatAccount acc) {
        Role role = roleRepository.findByAccountId(acc.getId())
                .map(roleMapper::toDto)
                .orElse(null);

        if (role != null && LocalDateTime.now().isBefore(role.getExpires())) {
            bot.execute(new SendMessage(acc.getChat().getId(),
                    String.format("Ти обрав роль %s. Зараз ти не можеш її змінити", role.getRole().getLabel())));
            return;
        }

        bot.execute(new SendMessage(acc.getChat().getId(), "Обери собі роль:")
                .parseMode(ParseMode.HTML)
                .replyMarkup(keyboardService.getRolesKeyboard(acc)));
        log.info("Options sent");
    }

    public void processRoleSelection(ChatAccount originAcc, QueryDataDto dto) {
        if (!originAcc.getId().equals(dto.getOriginalAccId())) {
            log.info("Invalid user click");
            return;
        }

        RoleType selectedRole = RoleType.getById(dto.getOption());

        roleRepository.findByAccountId(originAcc.getId())
                .map(roleMapper::toDto)
                .ifPresentOrElse(r -> updateRole(r, selectedRole), () -> createRole(originAcc, selectedRole));

        bot.execute(new SendMessage(originAcc.getChat().getId(),
                String.format("@%s тепер %s", originAcc.getUser().getUsername(), selectedRole.getLabel())));
        log.info("Role selection applied for {}", originAcc.getUser().getUsername());
    }

    private void updateRole(Role role, RoleType selectedRole) {
        role.setRole(selectedRole);
        role.setExpires(LocalDateTime.now().plusDays(7));

        roleRepository.save(roleMapper.toEntity(role));
    }

    private void createRole(ChatAccount originAcc, RoleType selectedRole) {
        roleRepository.save(RoleEntity.builder()
                .account(chatAccountMapper.toEntity(originAcc))
                .expires(LocalDateTime.now()
                        .plusDays(7))
                .role(selectedRole.getId())
                .build());
    }
}
