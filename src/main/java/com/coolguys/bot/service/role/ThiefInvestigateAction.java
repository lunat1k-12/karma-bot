package com.coolguys.bot.service.role;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.service.ThiefInvestigateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.coolguys.bot.service.role.RoleActionType.THIEF_INVESTIGATE_ACTION;

@Component
@RequiredArgsConstructor
public class ThiefInvestigateAction implements RoleAction {

    private final ThiefInvestigateService thiefInvestigateService;

    @Override
    public void doAction(ChatAccount acc) {
        thiefInvestigateService.processInvestigateRequest(acc);
    }

    @Override
    public String getActionType() {
        return THIEF_INVESTIGATE_ACTION;
    }
}
