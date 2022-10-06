package com.coolguys.bot.service;

import com.coolguys.bot.conf.BotConfig;
import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.UserStatus;
import com.coolguys.bot.entity.ChatAccountEntity;
import com.coolguys.bot.entity.TelegramChatEntity;
import com.coolguys.bot.entity.TelegramUserEntity;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.repository.TelegramChatRepository;
import com.coolguys.bot.repository.TelegramUserRepository;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;
    private final TelegramUserRepository telegramUserRepository;
    private final TelegramChatRepository telegramChatRepository;
    private final BotConfig botConfig;

    public void deactivateChatAccount(User user, Long chatId) {

        if (user.isBot() && botConfig.getUsername().equals(user.username())) {
            telegramChatRepository.findById(chatId)
                    .ifPresent(chatEntity -> {
                        chatEntity.setActive(false);
                        telegramChatRepository.save(chatEntity);
                    });
            return;
        }

        ChatAccountEntity entity = chatAccountRepository.findByUserIdAndChatId(user.id(), chatId)
                .orElse(null);
        if (entity != null) {
            entity.setStatus(UserStatus.INACTIVE.getId());
            chatAccountRepository.save(entity);
        }
    }

    public ChatAccount loadChatAccount(CallbackQuery query) {
        ChatAccountEntity acc = chatAccountRepository.findByUserIdAndChatId(query.from().id(), query.message().chat().id())
                .orElseGet(() -> createNewAccount(query));

        return chatAccountMapper.toDto(acc);
    }

    public ChatAccount loadChatAccount(Message message) {
        ChatAccountEntity acc = chatAccountRepository.findByUserIdAndChatId(message.from().id(), message.chat().id())
                .orElseGet(() -> createNewAccount(message));

        if (UserStatus.INACTIVE.getId().equals(acc.getStatus())) {
            acc.setStatus(UserStatus.ACTIVE.getId());
            acc = chatAccountRepository.save(acc);
        }

        boolean shouldUpdateUser = false;
        TelegramUserEntity userEntity = acc.getUser();
        if (message.from().firstName() != null && !message.from().firstName().equals(userEntity.getFirstName())) {
            userEntity.setFirstName(message.from().firstName());
            shouldUpdateUser = true;
        }

        if (message.from().lastName() != null && !message.from().lastName().equals(userEntity.getLastName())) {
            userEntity.setLastName(message.from().lastName());
            shouldUpdateUser = true;
        }

        if (message.from().username() != null && !message.from().username().equals(userEntity.getUsername())) {
            userEntity.setUsername(message.from().username());
            shouldUpdateUser = true;
        }

        if (shouldUpdateUser) {
            acc.setUser(telegramUserRepository.save(userEntity));
        }

        boolean shouldUpdateChat = false;
        if (!message.chat().title().equals(acc.getChat().getName())) {
            acc.getChat().setName(message.chat().title());
            shouldUpdateChat = true;
        }

        if (!acc.getChat().getActive()) {
            acc.getChat().setActive(true);
            shouldUpdateChat = true;
        }

        if (shouldUpdateChat) {
            telegramChatRepository.save(acc.getChat());
        }

        return chatAccountMapper.toDto(acc);
    }

    public TelegramUserEntity createNewUser(User from) {
        return telegramUserRepository.save(TelegramUserEntity.builder()
                .username(getOriginUsername(from))
                .lastName(from.lastName())
                .firstName(from.firstName())
                .id(from.id())
                .build());
    }

    private ChatAccountEntity createNewAccount(CallbackQuery query) {
        TelegramUserEntity userEntity = telegramUserRepository.findById(query.from().id())
                .orElseGet(() -> createNewUser(query.from()));

        TelegramChatEntity chatEntity = telegramChatRepository.findById(query.message().chat().id())
                .orElseGet(() -> telegramChatRepository.save(TelegramChatEntity.builder()
                        .premium(false)
                        .active(true)
                        .name(query.message().chat().title())
                        .id(query.message().chat().id())
                        .build()));

        return chatAccountRepository.save(ChatAccountEntity.builder()
                .user(userEntity)
                .status(UserStatus.ACTIVE.getId())
                .socialCredit(0)
                .chat(chatEntity)
                .build());
    }

    private ChatAccountEntity createNewAccount(Message message) {
        TelegramUserEntity userEntity = telegramUserRepository.findById(message.from().id())
                .orElseGet(() -> telegramUserRepository.save(TelegramUserEntity.builder()
                        .username(getOriginUsername(message))
                        .lastName(message.from().lastName())
                        .firstName(message.from().firstName())
                        .id(message.from().id())
                        .build()));

        TelegramChatEntity chatEntity = telegramChatRepository.findById(message.chat().id())
                .orElseGet(() -> telegramChatRepository.save(TelegramChatEntity.builder()
                        .premium(false)
                        .active(true)
                        .name(message.chat().title())
                        .id(message.chat().id())
                        .build()));

        return chatAccountRepository.save(ChatAccountEntity.builder()
                .user(userEntity)
                .status(UserStatus.ACTIVE.getId())
                .socialCredit(0)
                .chat(chatEntity)
                .build());
    }

    public List<ChatAccount> findActiveAccByChatId(Long chatId) {
        return chatAccountRepository.findByChatId(chatId).stream()
                .map(chatAccountMapper::toDto)
                .filter(ChatAccount::isActive)
                .collect(Collectors.toList());
    }

    public String getOriginUsername(Message message) {
        return Optional.ofNullable(message.from().username())
                .orElseGet(() -> getUsername(message.from().firstName(), message.from().lastName()));
    }

    public String getOriginUsername(User user) {
        return Optional.ofNullable(user.username())
                .orElseGet(() -> getUsername(user.firstName(), user.lastName()));
    }

    public String getUsername(String firstName, String lastName) {
        return firstName + " " + lastName;
    }
}
