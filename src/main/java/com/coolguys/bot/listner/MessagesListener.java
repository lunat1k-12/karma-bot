package com.coolguys.bot.listner;

import com.coolguys.bot.dto.ChatMessage;
import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.entity.ChatEntity;
import com.coolguys.bot.entity.DiceRequestEntity;
import com.coolguys.bot.entity.UserEntity;
import com.coolguys.bot.mapper.ChatMessageMapper;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.ChatMessageRepository;
import com.coolguys.bot.repository.ChatRepository;
import com.coolguys.bot.repository.DiceRequestRepository;
import com.coolguys.bot.repository.UserRepository;
import com.coolguys.bot.service.OrderService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.ChatMemberUpdated;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.request.SendDice;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendSticker;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class MessagesListener implements UpdatesListener {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    private final OrderService orderService;

    private final ChatMessageMapper chatMessageMapper;

    private final ChatMessageRepository chatMessageRepository;

    private final DiceRequestRepository diceRequestRepository;

    private final String PLUS_STICKER_ID = "CAACAgEAAxkBAAMMYw5PHFp_VtUGXqHA_-QeW-BqtusAAgIAA39wRhwFzGTYNyIryCkE";

    private final String MINUS_STICKER_ID = "CAACAgEAAxkBAAMUYw5PxdZ65ASPUMgrHHyiyiSPdVQAAgMAA39wRhxDWYhLWOdGzSkE";

    private final String UNIQ_PLUS_ID = "AgADAgADf3BGHA";
    private final String UNIQ_MINUS_ID = "AgADAwADf3BGHA";
    private final String CREDITS_COMMAND = "/credits@CoolGuys_Karma_bot";

    private final String AUTO_REPLY_COMMAND = "/auto_reply@CoolGuys_Karma_bot";

    private final String BOT_TOKEN = "5339250421:AAG02e6jq_jbqlszvvZTcFNVsPw_2NUW6RQ";
//    private final String BOT_TOKEN = "5698496704:AAHM2Ao0CAgviFZhbktIVL9chEsqBbmjEDg";

    private final TelegramBot bot;

    @Autowired
    public MessagesListener(ChatRepository chatRepository, UserRepository userRepository,
                            UserMapper userMapper, OrderService orderService,
                            ChatMessageMapper chatMessageMapper, ChatMessageRepository chatMessageRepository,
                            DiceRequestRepository diceRequestRepository) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.orderService = orderService;
        this.chatMessageMapper = chatMessageMapper;
        this.chatMessageRepository = chatMessageRepository;
        this.diceRequestRepository = diceRequestRepository;
        this.bot = new TelegramBot(BOT_TOKEN);
        bot.setUpdatesListener(this);
    }

    public void sendMessage(Long chatId, String message) {
        bot.execute(new SendMessage(chatId, message));
    }

    @Override
    public int process(List<Update> updates) {
        log.info("income: {}", updates);
        updates.forEach(update -> {
            if (update.myChatMember() != null) {
                updateChatMember(update.myChatMember());
            }
            if (update.message() != null) {
                processMessage(update.message());
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    private boolean isValidForCreditsCount(Message message) {
        String usernameFrom = getOriginUsername(message);

        String usernameReply = Optional.ofNullable(message.replyToMessage())
                .map(Message::from)
                .map(User::username)
                .orElseGet(() -> message.replyToMessage() != null ? getUsername(message.replyToMessage().from().firstName(), message.replyToMessage().from().lastName()) :
                        null);

        return message.sticker() != null && List.of(UNIQ_PLUS_ID, UNIQ_MINUS_ID).contains(message.sticker().fileUniqueId())
                && usernameReply != null && !usernameFrom.equals(usernameReply)
                && !message.replyToMessage().from().isBot();
    }
    private void processMessage(Message message) {
        log.info("proces message");
        UserInfo originUser = loadUser(message);
        if (isValidForCreditsCount(message)) {
            UserInfo targetUser = loadUser(message.replyToMessage());
            switch (message.sticker().fileUniqueId()) {
                case UNIQ_PLUS_ID:
                    targetUser.plusCredit(20);
                    break;
                case UNIQ_MINUS_ID:
                    targetUser.minusCredit(20);
                    break;
            }

            userRepository.save(userMapper.toEntity(targetUser));
        } else if (message.text() != null && CREDITS_COMMAND.equals(message.text())) {
            String msg = userRepository.findByChatId(message.chat().id()).stream()
                    .map(userMapper::toDto)
                    .map(u -> String.format("%s : %s", u.getUsername(), u.getSocialCredit()))
                    .reduce("", (m, u2) -> m + "\n" + u2);
            log.info("Print Credits");
            bot.execute(new SendMessage(message.chat().id(), "Credits:\n" + msg));
        } else if (message.text() != null && AUTO_REPLY_COMMAND.equals(message.text())) {
            orderService.createReplyOrder(originUser)
                    .ifPresent(bot::execute);
        } else if (message.dice() != null) {
            processDice(message, originUser);
        } else if (message.text() != null) {
            saveMessage(originUser, message);
            orderService.checkOrders(message.chat().id(), originUser, message.text().trim(), message.messageId())
                    .forEach(action -> action.ifPresent(bot::execute));
        }

    }

    private void processDice(Message message, UserInfo originUser) {

        if (diceRequestRepository.findAllByUserAndChatIdAndDateGreaterThan(userMapper.toEntity(originUser),
                message.chat().id(),
                LocalDateTime.now().minusHours(1L)).size() >= 3) {
            this.sendMessage(message.chat().id(), "Відпочинь лудоман.");
            return;
        }
        SendResponse response = bot.execute(new SendDice(message.chat().id())
                .emoji(message.dice().emoji()));

        if (message.dice().value() > response.message().dice().value()) {
            bot.execute(new SendMessage(message.chat().id(), String.format("@%s переміг", originUser.getUsername())));
            originUser.setSocialCredit(originUser.getSocialCredit() + 20);
            userRepository.save(userMapper.toEntity(originUser));
            bot.execute(new SendSticker(message.chat().id(), PLUS_STICKER_ID));
        } else if (message.dice().value().equals(response.message().dice().value())) {
            bot.execute(new SendMessage(message.chat().id(), "Нічия"));
        } else {
            bot.execute(new SendMessage(message.chat().id(), String.format("@%s програв", originUser.getUsername())));
            originUser.setSocialCredit(originUser.getSocialCredit() - 20);
            userRepository.save(userMapper.toEntity(originUser));
            bot.execute(new SendSticker(message.chat().id(), MINUS_STICKER_ID));
        }
        diceRequestRepository.save(DiceRequestEntity.builder()
                .chatId(message.chat().id())
                .date(LocalDateTime.now())
                .user(userMapper.toEntity(originUser))
                .build());
    }
    private void saveMessage(UserInfo originUser, Message message) {
        ChatMessage msg = ChatMessage.builder()
                .user(originUser)
                .date(LocalDateTime.now())
                .message(message.text())
                .chatId(message.chat().id())
                .build();
        chatMessageRepository.save(chatMessageMapper.toEntity(msg));
    }

    private String getOriginUsername(Message message) {
        return Optional.ofNullable(message.from().username())
                .orElseGet(() -> getUsername(message.from().firstName(), message.from().lastName()));
    }
    private UserInfo loadUser(Message message) {
        String username = getOriginUsername(message);
        UserEntity entity = userRepository.findByUsernameAndChatId(username, message.chat().id())
                .orElseGet(() -> userRepository.save(UserEntity.builder()
                        .chatId(message.chat().id())
                        .username(username)
                        .socialCredit(0)
                        .telegramId(message.from().id())
                        .build()));

        return userMapper.toDto(entity);
    }

    private String getUsername(String firstName, String lastName) {
        return firstName + " " + lastName;
    }

    private void updateChatMember(ChatMemberUpdated chat) {
        log.info("Process new chat");
        chatRepository.save(ChatEntity.builder()
                        .name(chat.chat().title())
                        .telegramId(chat.chat().id())
                .build());

        bot.execute(new SendMessage(chat.chat().id(), "Привіт хлопці"));
    }
}
