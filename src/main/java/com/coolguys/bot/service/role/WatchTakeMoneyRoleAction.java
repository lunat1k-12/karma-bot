package com.coolguys.bot.service.role;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.service.WatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.coolguys.bot.service.role.RoleActionType.PRISON_WATCH_TAKE_MONEY_ACTION;

@Component
@RequiredArgsConstructor
public class WatchTakeMoneyRoleAction implements RoleAction {

    private final WatchService watchService;

    @Override
    public void doAction(ChatAccount acc) {
        watchService.gainMoney(acc);
    }

    @Override
    public String getActionType() {
        return PRISON_WATCH_TAKE_MONEY_ACTION;
    }
}
