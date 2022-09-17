package com.coolguys.bot.service;

import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.dto.UserStatus;
import com.coolguys.bot.entity.ChatAccountEntity;
import com.coolguys.bot.entity.UserEntity;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.ChatAccountRepository;
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

    public ChatAccountEntity getChatAccount(Long userId, Long chatId) {
        return chatAccountRepository.findByUserIdAndChatId(userId, chatId)
                .orElse(null);
    }
    public void deactivateUser(UserInfo user) {
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(userMapper.toEntity(user));
    }
    public void save(UserInfo user) {
        userRepository.save(userMapper.toEntity(user));
    }

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

    public List<UserInfo> findActiveByChatId(Long chatId) {
        return userRepository.findByChatId(chatId).stream()
                .map(userMapper::toDto)
                .filter(UserInfo::isActive)
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
