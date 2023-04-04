package com.coolguys.bot.service.role;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.QueryDataDto;
import com.coolguys.bot.service.KeyboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleProcessor {
    private final List<RoleAction> actions;
    private final KeyboardService keyboardService;

    public void processAction(ChatAccount acc, QueryDataDto dto, Integer messageId) {
        if (acc.getId().equals(dto.getOriginalAccId())) {
            actions.stream()
                    .filter(action -> action.getActionType().equals(dto.getOption()))
                    .forEach(action -> action.doAction(acc));
            keyboardService.deleteOrUpdateKeyboardMessage(acc.getChat().getId(), messageId);
        }
    }
}
