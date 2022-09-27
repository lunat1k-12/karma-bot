package com.coolguys.bot.service.role;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.service.DrugsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.coolguys.bot.service.role.RoleActionType.DROP_DRUGS_ACTION;

@Component
@RequiredArgsConstructor
public class DropDrugsRoleAction implements RoleAction {

    private final DrugsService drugsService;

    @Override
    public void doAction(ChatAccount acc) {
        drugsService.dropDrugsRequest(acc);
    }

    @Override
    public String getActionType() {
        return DROP_DRUGS_ACTION;
    }
}
