package com.coolguys.bot.service;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.QueryDataDto;
import com.coolguys.bot.dto.RoleType;
import com.coolguys.bot.dto.TelegramUser;
import com.coolguys.bot.dto.Zodiac;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.google.gson.Gson;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.EditMessageReplyMarkup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.coolguys.bot.dto.QueryDataDto.ROLE_ACTION_TYPE;
import static com.coolguys.bot.dto.QueryDataDto.ROLE_SELECT_TYPE;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeyboardService {
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;

    private final TelegramBot bot;

    public InlineKeyboardMarkup getRoleActionsKeyboard(ChatAccount acc, RoleType role) {
        InlineKeyboardButton[] keys = new InlineKeyboardButton[role.getRoleActions().size()];

        Gson gson = new Gson();
        int roleIndex = 0;
        for (int i = 0; i < keys.length; i++) {
            if (role.getRoleActions().size() > roleIndex) {
                RoleType.RoleAction roleAction = role.getRoleActions().get(roleIndex);
                QueryDataDto query = QueryDataDto.builder()
                        .type(ROLE_ACTION_TYPE)
                        .option(roleAction.getId())
                        .originalAccId(acc.getId())
                        .build();
                keys[i] = new InlineKeyboardButton(roleAction.getLabel()).callbackData(gson.toJson(query));
                roleIndex++;
            }
        }

        return new InlineKeyboardMarkup(keys);
    }

    public void deleteOrUpdateKeyboardMessage(Long chatId, int messageId) {
        if (!bot.execute(new DeleteMessage(chatId, messageId)).isOk()) {
            EditMessageReplyMarkup editMessageReplyMarkup = new EditMessageReplyMarkup(chatId, messageId)
                    .replyMarkup(new InlineKeyboardMarkup());
            bot.execute(editMessageReplyMarkup);
        }
    }
    public InlineKeyboardMarkup getRolesKeyboard(ChatAccount acc) {
        int verticalRowCount = Double.valueOf(Math.ceil(Integer.valueOf(RoleType.values().length).doubleValue() / 2)).intValue();
        InlineKeyboardButton[][] keys = new InlineKeyboardButton[verticalRowCount][2];

        Gson gson = new Gson();
        int roleIndex = 0;
        for (int i = 0; i < keys.length; i++) {
            for (int j=0; j < keys[i].length; j++) {
                if (RoleType.values().length > roleIndex) {
                    RoleType roleType = RoleType.values()[roleIndex];
                    QueryDataDto query = QueryDataDto.builder()
                            .type(ROLE_SELECT_TYPE)
                            .option(roleType.getId())
                            .originalAccId(acc.getId())
                            .build();
                    keys[i][j] = new InlineKeyboardButton(roleType.getLabel()).callbackData(gson.toJson(query));
                    roleIndex++;
                }
            }
        }

        if (RoleType.values().length % 2 != 0) {
            keys[verticalRowCount - 1] = new InlineKeyboardButton[]{keys[verticalRowCount - 1][0]};
        }
        return new InlineKeyboardMarkup(keys);
    }

    public InlineKeyboardMarkup getZodiacKeyboard(ChatAccount acc, String type) {
        int verticalRowCount = Double.valueOf(Math.ceil(Integer.valueOf(Zodiac.values().length).doubleValue() / 2)).intValue();
        InlineKeyboardButton[][] keys = new InlineKeyboardButton[verticalRowCount][2];

        Gson gson = new Gson();
        int zodiacIndex = 0;
        for (int i = 0; i < keys.length; i++) {
            for (int j=0; j < keys[i].length; j++) {
                if (Zodiac.values().length > zodiacIndex) {
                    Zodiac zodiac = Zodiac.values()[zodiacIndex];
                    QueryDataDto query = QueryDataDto.builder()
                            .type(type)
                            .option(zodiac.getName())
                            .originalAccId(acc.getId())
                            .build();
                    keys[i][j] = new InlineKeyboardButton(zodiac.getButtonName()).callbackData(gson.toJson(query));
                    zodiacIndex++;
                }
            }
        }

        if (Zodiac.values().length % 2 != 0) {
            keys[verticalRowCount - 1] = new InlineKeyboardButton[]{keys[verticalRowCount - 1][0]};
        }
        return new InlineKeyboardMarkup(keys);
    }

    public InlineKeyboardMarkup getTargetAccSelectionPersonKeyboard(Long chatId, Long originalAccId, String type) {
        List<ChatAccount> users = chatAccountRepository.findActiveByChatId(chatId)
                .stream()
                .filter(acc -> !acc.getId().equals(originalAccId))
                .map(chatAccountMapper::toDto)
                .collect(Collectors.toList());

        ChatAccount cancelUser = ChatAccount.builder()
                .id(0L)
                .user(TelegramUser.builder()
                        .id(0L)
                        .username("Відмінити")
                        .build())
                .build();

        List<ChatAccount> usersToProcess = new ArrayList<>();
        usersToProcess.add(cancelUser);
        usersToProcess.addAll(users);

        int verticalRowCount = Double.valueOf(Math.ceil(Integer.valueOf(usersToProcess.size()).doubleValue() / 2)).intValue();
        InlineKeyboardButton[][] keys = new InlineKeyboardButton[verticalRowCount][2];

        Gson gson = new Gson();
        int usersIndex = 0;
        for (int i = 0; i < keys.length; i++) {
            for (int j=0; j < keys[i].length; j++) {
                if (usersToProcess.size() > usersIndex) {
                    ChatAccount user = usersToProcess.get(usersIndex);
                    QueryDataDto query = QueryDataDto.builder()
                            .type(type)
                            .option(user.getId().toString())
                            .originalAccId(originalAccId)
                            .build();
                    keys[i][j] = new InlineKeyboardButton(user.getUser().getUsername()).callbackData(gson.toJson(query));
                    usersIndex++;
                }
            }
        }

        if (usersToProcess.size() % 2 != 0) {
            keys[verticalRowCount - 1] = new InlineKeyboardButton[]{keys[verticalRowCount - 1][0]};
        }
        return new InlineKeyboardMarkup(keys);
    }
}
