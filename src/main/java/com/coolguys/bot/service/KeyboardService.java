package com.coolguys.bot.service;

import com.coolguys.bot.dto.QueryDataDto;
import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.UserRepository;
import com.google.gson.Gson;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeyboardService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public InlineKeyboardMarkup getTargetSelectionPersonKeyboard(Long chatId, Long originalUserId, String type) {
        List<UserInfo> users = userRepository.findByChatId(chatId)
                .stream()
                .filter(u -> !u.getId().equals(originalUserId))
                .map(userMapper::toDto)
                .collect(Collectors.toList());

        UserInfo cancelUser = UserInfo.builder()
                .id(0L)
                .username("Відмінити")
                .build();

        List<UserInfo> usersToProcess = new ArrayList<>();
        usersToProcess.add(cancelUser);
        usersToProcess.addAll(users);

        int verticalRowCount = Double.valueOf(Math.ceil(Integer.valueOf(usersToProcess.size()).doubleValue() / 2)).intValue();
        InlineKeyboardButton[][] keys = new InlineKeyboardButton[verticalRowCount][2];

        Gson gson = new Gson();
        int usersIndex = 0;
        for (int i = 0; i < keys.length; i++) {
            for (int j=0; j < keys[i].length; j++) {
                if (usersToProcess.size() > usersIndex) {
                    UserInfo user = usersToProcess.get(usersIndex);
                    QueryDataDto query = QueryDataDto.builder()
                            .type(type)
                            .option(user.getId().toString())
                            .build();
                    keys[i][j] = new InlineKeyboardButton(user.getUsername()).callbackData(gson.toJson(query));
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
