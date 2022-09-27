package com.coolguys.bot.service.role;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.QueryDataDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleProcessor {
    private final List<RoleAction> actions;

    public void processAction(ChatAccount acc, QueryDataDto dto) {
        if (acc.getId().equals(dto.getOriginalAccId())) {
            actions.stream()
                    .filter(action -> action.getActionType().equals(dto.getOption()))
                    .forEach(action -> action.doAction(acc));
        }
    }
}
