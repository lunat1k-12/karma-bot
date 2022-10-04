package com.coolguys.bot.service;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.TelegramDrugAction;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MyStatsService {

    private final GuardService guardService;
    private final StealService stealService;
    private final DrugsService drugsService;
    private final TelegramBot bot;

    public void printPersonalStats(ChatAccount acc) {
        printPersonalStats(acc, acc.getUser().getId());
    }

    public boolean printPersonalStats(ChatAccount acc, Long targetChatId) {
        StringBuilder sb = new StringBuilder().append("Показники для @")
                .append(acc.getUser().getUsername())
                .append("\nЧат: ")
                .append(acc.getChat().getName())
                .append("\n****************");

        if (guardService.doesHaveGuard(acc)) {
            sb.append("\nмає охорону до:\n");
            sb.append(guardService.getGuardTillLabel(acc));
        }
        if (stealService.isInJail(acc)) {
            sb.append("\nУ в`язниці до:\n");
            sb.append(stealService.getJailTillLabel(acc));
        }

        List<TelegramDrugAction> drugs = drugsService.findActiveDrugDeals(acc);
        if (!drugs.isEmpty()) {
            sb.append("\nМає наркотики до:\n");
            sb.append(drugs.stream()
                    .max(Comparator.comparing(TelegramDrugAction::getExpires))
                    .map(TelegramDrugAction::getExpires)
                    .map(DateConverter::localDateTimeToStringLabel)
                    .orElse(null));
        }

        sb.append("\nКредити: ").append(acc.getSocialCredit());
        SendResponse response = bot.execute(new SendMessage(targetChatId, sb.toString()));
        log.info("response: {}", response);
        if (response.isOk()) {
            bot.execute(new SendMessage(acc.getChat().getId(), "Відправив стату в лічку"));
            return true;
        } else {
            bot.execute(new SendMessage(acc.getChat().getId(),
                    String.format("@%s напиши мені в лічку щоб отримувати ці сповіщення", acc.getUser().getUsername())));
            return false;
        }
    }
}
