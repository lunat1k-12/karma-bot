package com.coolguys.bot.service;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.GuardDepartmentDto;
import com.coolguys.bot.dto.TelegramGuardDepartment;
import com.coolguys.bot.dto.TelegramUser;
import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.entity.GuardDepartmentEntity;
import com.coolguys.bot.entity.TelegramGuardDepartmentEntity;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.mapper.GuardDepartmentMapper;
import com.coolguys.bot.mapper.TelegramGuardDepartmentMapper;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.repository.GuardDepartmentRepository;
import com.coolguys.bot.repository.TelegramGuardDepartmentRepository;
import com.coolguys.bot.repository.UserRepository;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendSticker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GuardDepartmentService {

    private final GuardDepartmentRepository guardDepartmentRepository;
    private final GuardDepartmentMapper guardDepartmentMapper;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final TelegramGuardDepartmentRepository telegramGuardDepartmentRepository;
    private final TelegramGuardDepartmentMapper telegramGuardDepartmentMapper;
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;
    private final TelegramBot bot;
    private static final Integer INITIAL_GD_PRICE = 600;
    private static final Integer GD_PRICE_STEP = 100;
    private static final String GD_BUY_STICKER = "CAACAgIAAxkBAAIFw2MgurDoNb88MEL8on6NUfCiB9cGAALwDAACeAagSuHQ43VtE4uuKQQ";

    public void processGuardOwnerIncome(Long chatId, Integer price) {
        TelegramGuardDepartment guard = findOrCreateTelegramGdByChatID(chatId);

        if (guard.getOwner() != null) {
            TelegramUser owner = guard.getOwner();
            int ownerPrice = Double.valueOf(Math.floor(price / 2d)).intValue();
            ChatAccount ownerAcc = chatAccountRepository.findByUserIdAndChatId(owner.getId(), chatId)
                    .map(chatAccountMapper::toDto)
                    .orElseThrow();
            ownerAcc.plusCredit(ownerPrice);
            chatAccountRepository.save(chatAccountMapper.toEntity(ownerAcc));
            bot.execute(new SendMessage(chatId,
                    String.format("%s забирає собі властник охороного агенства", ownerPrice)));
        }
    }

    public void dropGuardOwner(Long chatId) {
        TelegramGuardDepartment guard = findOrCreateTelegramGdByChatID(chatId);
        guard.setOwner(null);
        guard.setCurrentPrice(INITIAL_GD_PRICE);
        telegramGuardDepartmentRepository.save(telegramGuardDepartmentMapper.toEntity(guard));
    }

    public boolean buyGuardDepartment(ChatAccount acc) {
        TelegramGuardDepartment gd = findOrCreateTelegramGdByChatID(acc.getChat().getId());

        if (acc.getSocialCredit() < gd.getCurrentPrice()) {
            bot.execute(new SendMessage(acc.getChat().getId(),
                    String.format("За ці копійки охороне агенство не купиш. Актуальна ціна - %s", gd.getCurrentPrice())));
            return false;
        }

        if (gd.getOwner() != null && gd.getOwner().getId().equals(acc.getUser().getId())) {
            bot.execute(new SendMessage(acc.getChat().getId(), "Охороне агенство вже твоє!"));
            return false;
        }

        acc.minusCredit(gd.getCurrentPrice());
        gd.plusPrice(GD_PRICE_STEP);
        gd.setOwner(acc.getUser());
        chatAccountRepository.save(chatAccountMapper.toEntity(acc));
        telegramGuardDepartmentRepository.save(telegramGuardDepartmentMapper.toEntity(gd));
        bot.execute(new SendMessage(acc.getChat().getId(),
                String.format("@%s тепер новий властник охороного агенства!", acc.getUser().getUsername())));
        bot.execute(new SendSticker(acc.getChat().getId(), GD_BUY_STICKER));
        log.info("GD bought by {}", acc.getUser().getUsername());
        return true;
    }

    @Deprecated
    public boolean buyGuardDepartment(UserInfo user) {
        GuardDepartmentDto gd = findOrCreateGdByChatID(user.getChatId());

        if (user.getSocialCredit() < gd.getCurrentPrice()) {
            bot.execute(new SendMessage(user.getChatId(),
                    String.format("За ці копійки охороне агенство не купиш. Актуальна ціна - %s", gd.getCurrentPrice())));
            return false;
        }

        if (gd.getOwner() != null && gd.getOwner().getId().equals(user.getId())) {
            bot.execute(new SendMessage(user.getChatId(), "Охороне агенство вже твоє!"));
            return false;
        }

        user.minusCredit(gd.getCurrentPrice());
        gd.plusPrice(GD_PRICE_STEP);
        gd.setOwner(user);
        userRepository.save(userMapper.toEntity(user));
        guardDepartmentRepository.save(guardDepartmentMapper.toEntity(gd));
        bot.execute(new SendMessage(user.getChatId(),
                String.format("@%s тепер новий властник охороного агенства!", user.getUsername())));
        bot.execute(new SendSticker(user.getChatId(), GD_BUY_STICKER));
        log.info("GD bought by {}", user.getUsername());
        return  true;
    }

    public TelegramGuardDepartment findOrCreateTelegramGdByChatID(Long chatId) {
        return telegramGuardDepartmentRepository.findByChatId(chatId)
                .map(telegramGuardDepartmentMapper::toDto)
                .orElseGet(() -> createInitialTelegramGd(chatId));
    }

    private TelegramGuardDepartment createInitialTelegramGd(Long chatId) {
        return telegramGuardDepartmentMapper.toDto(telegramGuardDepartmentRepository.save(TelegramGuardDepartmentEntity.builder()
                .chatId(chatId)
                .currentPrice(INITIAL_GD_PRICE)
                .build()));
    }

    @Deprecated
    public GuardDepartmentDto findOrCreateGdByChatID(Long chatId) {
        return guardDepartmentRepository.findByChatId(chatId)
                .map(guardDepartmentMapper::toDto)
                .orElseGet(() -> createInitialGd(chatId));
    }

    private GuardDepartmentDto createInitialGd(Long chatId) {
        return guardDepartmentMapper.toDto(guardDepartmentRepository.save(GuardDepartmentEntity.builder()
                .currentPrice(INITIAL_GD_PRICE)
                .chatId(chatId)
                .build()));
    }
}
