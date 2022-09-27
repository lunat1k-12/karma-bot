package com.coolguys.bot.service.role;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.service.StealService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.coolguys.bot.service.role.RoleActionType.THIEF_STEAL_ACTION;

@Component
@RequiredArgsConstructor
public class StealRoleAction implements RoleAction {

    private final StealService stealService;

    @Override
    public void doAction(ChatAccount acc) {
        stealService.stealRequest(acc);
    }

    @Override
    public String getActionType() {
        return THIEF_STEAL_ACTION;
    }
}
