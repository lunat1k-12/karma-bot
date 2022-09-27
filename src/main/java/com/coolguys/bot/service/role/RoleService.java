package com.coolguys.bot.service.role;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.QueryDataDto;
import com.coolguys.bot.dto.Role;
import com.coolguys.bot.dto.RoleType;
import com.coolguys.bot.entity.RoleEntity;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.mapper.RoleMapper;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.repository.RoleRepository;
import com.coolguys.bot.service.KeyboardService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoleService {

    private final RoleRepository roleRepository;
    private final RoleMapper roleMapper;
    private final TelegramBot bot;
    private final KeyboardService keyboardService;
    private final ChatAccountMapper chatAccountMapper;
    private final ChatAccountRepository chatAccountRepository;

    public void showRoleActions(ChatAccount acc) {
        Role role = roleRepository.findByAccountId(acc.getId())
                .map(roleMapper::toDto)
                .orElse(null);

        if (role == null || LocalDateTime.now().isAfter(role.getExpires())) {
            bot.execute(new SendMessage(acc.getChat().getId(), "Тобі спочатку треба обрати роль"));
            return;
        }

        SendResponse resp = bot.execute(new SendMessage(acc.getChat().getId(), "Обери що хочеш зробити:")
                .parseMode(ParseMode.HTML)
                .replyMarkup(keyboardService.getRoleActionsKeyboard(acc, role.getRole())));

        log.info("resp: {}", resp);
    }

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

        List<ChatAccount> chatAccounts = chatAccountRepository.findByChatId(originAcc.getChat().getId()).stream()
                .map(chatAccountMapper::toDto)
                .collect(Collectors.toList());

        int roleMaxUsers = Double.valueOf(Math.ceil(Integer.valueOf(chatAccounts.size()).doubleValue() / RoleType.values().length))
                .intValue();
        log.info("Max {} users per role {} in chat {}", roleMaxUsers, selectedRole.getId(), originAcc.getChat().getName());

        if (roleRepository.findByChatAndRoleType(originAcc.getChat().getId(), selectedRole.getId(), LocalDateTime.now())
                .size() >= roleMaxUsers) {
            log.info("Too much {}", selectedRole.getLabel());
            bot.execute(new SendMessage(originAcc.getChat().getId(),
                    String.format("Занадто багато гравців у ролі '%s'\nОбери іншу", selectedRole.getLabel())));
            return;
        }

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
