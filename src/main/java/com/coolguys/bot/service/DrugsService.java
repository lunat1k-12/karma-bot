package com.coolguys.bot.service;

import com.coolguys.bot.dto.DrugAction;
import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.entity.DrugActionEntity;
import com.coolguys.bot.mapper.DrugActionMapper;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.DrugActionRepository;
import com.coolguys.bot.repository.UserRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DrugsService {

    private final DrugActionRepository drugActionRepository;
    private final DrugActionMapper drugActionMapper;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final StealService stealService;
    private final TelegramBot bot;

    private static final Integer DRUGS_INCOME = 200;

    public void doDrugs(UserInfo user) {
        if (findActiveDrugDeals(user).size() > 0) {
            bot.execute(new SendMessage(user.getChatId(), "Тобі треба залягти на дно"));
            return;
        }

        if (stealService.isInJail(user)) {
            bot.execute(new SendMessage(user.getChatId(), "Відсиди спочатку"));
            return;
        }

        user.plusCredit(DRUGS_INCOME);
        userRepository.save(userMapper.toEntity(user));
        drugActionRepository.save(DrugActionEntity.builder()
                .user(userMapper.toEntity(user))
                .expires(LocalDateTime.now().plusHours(24))
                .chatId(user.getChatId())
                .build());

        bot.execute(new SendMessage(user.getChatId(), String.format("@%s підняв грошей на наркоті!", user.getUsername())));
    }

    public List<DrugAction> findActiveDrugDeals(UserInfo user) {
        return drugActionRepository.findByUserAndChatIdAndExpiresAfter(
                        userMapper.toEntity(user),
                        user.getChatId(),
                        LocalDateTime.now()
                ).stream()
                .map(drugActionMapper::toDto)
                .collect(Collectors.toList());
    }

    public void discardDrugDeals(UserInfo user) {
        drugActionRepository.findByUserAndChatIdAndExpiresAfter(
                        userMapper.toEntity(user),
                        user.getChatId(),
                        LocalDateTime.now()
                ).stream()
                .map(drugActionMapper::toDto)
                .peek(d -> d.setExpires(LocalDateTime.now()))
                .map(drugActionMapper::toEntity)
                .forEach(drugActionRepository::save);
    }
}
