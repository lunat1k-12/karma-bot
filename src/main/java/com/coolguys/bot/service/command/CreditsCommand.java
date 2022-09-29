package com.coolguys.bot.service.command;

import com.coolguys.bot.conf.BotConfig;
import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.TelegramCasino;
import com.coolguys.bot.dto.TelegramGuardDepartment;
import com.coolguys.bot.dto.TelegramPoliceDepartment;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.service.CasinoService;
import com.coolguys.bot.service.DrugsService;
import com.coolguys.bot.service.GuardDepartmentService;
import com.coolguys.bot.service.GuardService;
import com.coolguys.bot.service.PoliceDepartmentService;
import com.coolguys.bot.service.StealService;
import com.coolguys.bot.service.role.RoleService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreditsCommand implements Command {

    private final BotConfig botConfig;
    private final CasinoService casinoService;
    private final PoliceDepartmentService policeDepartmentService;
    private final GuardDepartmentService guardDepartmentService;
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;
    private final StealService stealService;
    private final TelegramBot bot;
    private final RoleService roleService;
    private final DrugsService drugsService;
    private final GuardService guardService;

    @Override
    public void processCommand(Message message, ChatAccount originAccount) {
        printCredits(message);
    }

    private void printCredits(Message message) {
        log.info("Print Credits");
        TelegramCasino casino = casinoService.findOrCreateTelegramCasinoByChatID(message.chat().id());
        TelegramPoliceDepartment pd = policeDepartmentService.findOrCreateTelegramPdByChatID(message.chat().id());
        TelegramGuardDepartment gd = guardDepartmentService.findOrCreateTelegramGdByChatID(message.chat().id());
        List<String> lines = chatAccountRepository.findByChatId(message.chat().id()).stream()
                .map(chatAccountMapper::toDto)
                .filter(ChatAccount::isActive)
                .sorted(Comparator.comparingInt(ChatAccount::getSocialCredit)
                        .reversed())
                .map(acc -> toStringInfo(acc, casino, pd, gd))
                .collect(Collectors.toList());

        if (lines.size() >= 1) {
            lines.set(0, "\uD83E\uDD47" + lines.get(0));
        }
        if (lines.size() >= 2) {
            lines.set(1, "\uD83E\uDD48" + lines.get(1));
        }
        if (lines.size() >= 3) {
            lines.set(2, "\uD83E\uDD49" + lines.get(2));
        }
        String msg = lines.stream()
                .reduce("", (m, u2) -> m + "\n" + u2);

        log.info("Print Credits");

        bot.execute(new SendMessage(message.chat().id(),
                "\uD83C\uDFE6 Рахунки:\n" +
                        msg +
                        "\n****************" +
                        "\nСумарний Банк: " +
                        stealService.creditsSum(message.chat().id()) +
                        "\nВартість казино: " + casino.getCurrentPrice() +
                        "\nВартість поліції: " + pd.getCurrentPrice() +
                        "\nВартість охоронки: " + gd.getCurrentPrice()));
    }


    private String toStringInfo(ChatAccount acc, TelegramCasino casino, TelegramPoliceDepartment pd, TelegramGuardDepartment gd) {
        StringBuilder sb = new StringBuilder(String.format("%s : %s ", acc.getUser().getUsername(), acc.getSocialCredit()));
        roleService.getAccRole(acc)
                .ifPresent(r -> sb.append(r.getRole().getEmoji()));
        if (guardService.doesHaveGuard(acc)) {
            sb.append("⚔️");
        }
        if (stealService.isInJail(acc)) {
            sb.append("⛓");
        }
        if (casino.getOwner() != null && casino.getOwner().getId().equals(acc.getUser().getId())) {
            sb.append("\uD83C\uDFB0");
        }
        if (pd.getOwner() != null && pd.getOwner().getId().equals(acc.getUser().getId())) {
            sb.append("\uD83D\uDE94");
        }
        if (gd.getOwner() != null && gd.getOwner().getId().equals(acc.getUser().getId())) {
            sb.append("\uD83D\uDEE1");
        }
        if (!drugsService.findActiveDrugDeals(acc).isEmpty()) {
            sb.append("\uD83D\uDC89");
        }
        return sb.toString();
    }

    @Override
    public String getCommand() {
        return botConfig.getCreditCommand();
    }
}
