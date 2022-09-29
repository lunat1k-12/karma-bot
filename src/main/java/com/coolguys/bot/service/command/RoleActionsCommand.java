package com.coolguys.bot.service.command;

import com.coolguys.bot.conf.BotConfig;
import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.service.role.RoleService;
import com.pengrad.telegrambot.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoleActionsCommand implements Command {

    private final BotConfig botConfig;
    private final RoleService roleService;

    @Override
    public void processCommand(Message message, ChatAccount originAccount) {
        roleService.showRoleActions(originAccount);
    }

    @Override
    public String getCommand() {
        return botConfig.getRoleActionsCommand();
    }
}
