package com.coolguys.bot.service.role;

import com.coolguys.bot.dto.ChatAccount;

public interface RoleAction {

    void doAction(ChatAccount acc);

    String getActionType();
}
