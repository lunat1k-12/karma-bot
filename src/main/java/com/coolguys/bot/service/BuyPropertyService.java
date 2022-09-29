package com.coolguys.bot.service;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.TelegramCasino;
import com.coolguys.bot.dto.TelegramGuardDepartment;
import com.coolguys.bot.dto.TelegramPoliceDepartment;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BuyPropertyService {

    private final CasinoService casinoService;
    private final PoliceDepartmentService policeDepartmentService;
    private final GuardDepartmentService guardDepartmentService;
    private final TelegramBot bot;

    public void processCasinoBuy(ChatAccount originAcc) {
        if (casinoService.buyCasino(originAcc)) {
            dropPoliceDepartmentIfExists(originAcc);
            dropGuardIfExists(originAcc);
        }
    }

    public void processPdBuy(ChatAccount originAcc) {
        if (policeDepartmentService.buyPoliceDepartment(originAcc)) {
            dropCasinoIfExists(originAcc);
            dropGuardIfExists(originAcc);
        }
    }

    public void processGdBuy(ChatAccount originAcc) {
        if (guardDepartmentService.buyGuardDepartment(originAcc)) {
            dropCasinoIfExists(originAcc);
            dropPoliceDepartmentIfExists(originAcc);
        }
    }

    private void dropPoliceDepartmentIfExists(ChatAccount originAcc) {
        TelegramPoliceDepartment pd = policeDepartmentService.findOrCreateTelegramPdByChatID(originAcc.getChat().getId());
        if (pd.getOwner() != null && pd.getOwner().getId().equals(originAcc.getUser().getId())) {
            policeDepartmentService.dropPdOwner(originAcc.getChat().getId());
            bot.execute(new SendMessage(originAcc.getChat().getId(),
                    String.format("@%s більше не властник поліцейської ділянки", originAcc.getUser().getUsername())));
        }
    }

    private void dropCasinoIfExists(ChatAccount originAcc) {
        TelegramCasino casino = casinoService.findOrCreateTelegramCasinoByChatID(originAcc.getChat().getId());
        if (casino.getOwner() != null && casino.getOwner().getId().equals(originAcc.getUser().getId())) {
            casinoService.dropCasinoOwner(originAcc.getChat().getId());
            bot.execute(new SendMessage(originAcc.getChat().getId(),
                    String.format("@%s більше не властник казино", originAcc.getUser().getUsername())));
        }
    }

    private void dropGuardIfExists(ChatAccount originAcc) {
        TelegramGuardDepartment gd = guardDepartmentService.findOrCreateTelegramGdByChatID(originAcc.getChat().getId());
        if (gd.getOwner() != null && gd.getOwner().getId().equals(originAcc.getUser().getId())) {
            guardDepartmentService.dropGuardOwner(originAcc.getChat().getId());
            bot.execute(new SendMessage(originAcc.getChat().getId(),
                    String.format("@%s більше не властник охороного агенства", originAcc.getUser().getUsername())));
        }
    }
}
