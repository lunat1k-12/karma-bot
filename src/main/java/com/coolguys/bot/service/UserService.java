package com.coolguys.bot.service;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.dto.UserStatus;
import com.coolguys.bot.entity.ChatAccountEntity;
import com.coolguys.bot.entity.TelegramChatEntity;
import com.coolguys.bot.entity.TelegramUserEntity;
import com.coolguys.bot.entity.UserEntity;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.repository.TelegramChatRepository;
import com.coolguys.bot.repository.TelegramUserRepository;
import com.coolguys.bot.repository.UserRepository;
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

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;
    private final TelegramUserRepository telegramUserRepository;
    private final TelegramChatRepository telegramChatRepository;

    public void deactivateUser(UserInfo user) {
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(userMapper.toEntity(user));
    }

    public void deactivateChatAccount(Long userId, Long chatId) {
        ChatAccountEntity entity = chatAccountRepository.findByUserIdAndChatId(userId, chatId)
                .orElse(null);
        if (entity != null) {
            entity.setStatus(UserStatus.INACTIVE.getId());
            chatAccountRepository.save(entity);
        }
    }
    public void save(UserInfo user) {
        userRepository.save(userMapper.toEntity(user));
    }

    @Deprecated
    public UserInfo loadUser(CallbackQuery query) {
        String username = getOriginUsername(query.from());
        UserEntity entity = userRepository.findByUsernameAndChatId(username, query.message().chat().id())
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .chatId(query.message().chat().id())
                        .username(username)
                        .socialCredit(0)
                        .telegramId(query.from().id())
                        .build()));

        return userMapper.toDto(entity);
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

        return chatAccountMapper.toDto(acc);
    }

    private ChatAccountEntity createNewAccount(CallbackQuery query) {
        TelegramUserEntity userEntity = telegramUserRepository.findById(query.from().id())
                .orElseGet(() -> telegramUserRepository.save(TelegramUserEntity.builder()
                        .username(getOriginUsername(query.from()))
                        .lastName(query.from().lastName())
                        .firstName(query.from().firstName())
                        .id(query.from().id())
                        .build()));

        TelegramChatEntity chatEntity = telegramChatRepository.findById(query.message().chat().id())
                .orElseGet(() -> telegramChatRepository.save(TelegramChatEntity.builder()
                        .premium(false)
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

    public UserInfo loadUser(Message message) {
        String username = getOriginUsername(message);
        UserEntity entity = userRepository.findByUsernameAndChatId(username, message.chat().id())
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .chatId(message.chat().id())
                        .username(username)
                        .socialCredit(0)
                        .telegramId(message.from().id())
                        .status(UserStatus.ACTIVE.getId())
                        .build()));

        if (UserStatus.INACTIVE.getId().equals(entity.getStatus())) {
            entity.setStatus(UserStatus.ACTIVE.getId());
            entity = userRepository.save(entity);
        }

        return userMapper.toDto(entity);
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
