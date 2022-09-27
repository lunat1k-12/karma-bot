package com.coolguys.bot.service.role;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.service.DrugsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.coolguys.bot.service.role.RoleActionType.DO_DRUGS_ACTION;

@Component
@RequiredArgsConstructor
public class DoDrugsRoleAction implements RoleAction {

    private final DrugsService drugsService;

    @Override
    public void doAction(ChatAccount acc) {
        drugsService.doDrugs(acc);
    }

    @Override
    public String getActionType() {
        return DO_DRUGS_ACTION;
    }
}
