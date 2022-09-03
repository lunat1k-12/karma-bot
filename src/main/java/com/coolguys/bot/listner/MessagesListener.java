package com.coolguys.bot.listner;

import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.entity.ChatEntity;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.ChatRepository;
import com.coolguys.bot.repository.UserRepository;
import com.coolguys.bot.service.DiceService;
import com.coolguys.bot.service.KarmaService;
import com.coolguys.bot.service.MessagesService;
import com.coolguys.bot.service.OrderService;
import com.coolguys.bot.service.UserService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.ChatMemberUpdated;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendSticker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class MessagesListener implements UpdatesListener {

    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    private final OrderService orderService;

    private final DiceService diceService;

    private final KarmaService karmaService;

    private final UserService userService;

    private final MessagesService messagesService;

    public static final String UNIQ_PLUS_ID = "AgADAgADf3BGHA";
    public static final String UNIQ_MINUS_ID = "AgADAwADf3BGHA";
    private static final String CREDITS_COMMAND = "/credits@CoolGuys_Karma_bot";

    private static final String AUTO_REPLY_COMMAND = "/auto_reply@CoolGuys_Karma_bot";

    private static final String REMOVE_PLAY_BAN_COMMAND = "/remove_play_ban@CoolGuys_Karma_bot";

    private final String BOT_TOKEN = "5339250421:AAG02e6jq_jbqlszvvZTcFNVsPw_2NUW6RQ";
//    private final String BOT_TOKEN = "5698496704:AAHM2Ao0CAgviFZhbktIVL9chEsqBbmjEDg";

    private final TelegramBot bot;

    @Autowired
    public MessagesListener(ChatRepository chatRepository, UserRepository userRepository,
                            UserMapper userMapper, OrderService orderService,
                            DiceService diceService, KarmaService karmaService,
                            UserService userService, MessagesService messagesService) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.orderService = orderService;
        this.diceService = diceService;
        this.karmaService = karmaService;
        this.userService = userService;
        this.messagesService = messagesService;
        this.bot = new TelegramBot(BOT_TOKEN);
        bot.setUpdatesListener(this);
    }

    public void sendMessage(Long chatId, String message) {
        bot.execute(new SendMessage(chatId, message));
    }

    public void sendSticker(Long chatId, String stickerId) {
        bot.execute(new SendSticker(chatId, stickerId));
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
        String usernameFrom = userService.getOriginUsername(message);

        String usernameReply = Optional.ofNullable(message.replyToMessage())
                .map(Message::from)
                .map(User::username)
                .orElseGet(() -> message.replyToMessage() != null ? userService.getUsername(message.replyToMessage().from().firstName(), message.replyToMessage().from().lastName()) :
                        null);

        return message.sticker() != null && List.of(UNIQ_PLUS_ID, UNIQ_MINUS_ID).contains(message.sticker().fileUniqueId())
                && usernameReply != null && !usernameFrom.equals(usernameReply)
                && !message.replyToMessage().from().isBot();
    }
    private void processMessage(Message message) {
        log.info("proces message");
        UserInfo originUser = userService.loadUser(message);
        if (isValidForCreditsCount(message)) {
            log.info("Process karma update");
            karmaService.processKarmaUpdate(message, originUser, bot);
        } else if (message.text() != null && CREDITS_COMMAND.equals(message.text())) {
            log.info("Print Credits");
            printCredits(message);
        } else if (message.text() != null && AUTO_REPLY_COMMAND.equals(message.text())) {
            log.info("Create auto-reply");
            orderService.createReplyOrder(originUser)
                    .ifPresent(bot::execute);
        } else if (message.text() != null && REMOVE_PLAY_BAN_COMMAND.equals(message.text())) {
            log.info("remove play ban command");
            diceService.removePlayBan(originUser, bot);
        } else if (message.dice() != null) {
            log.info("Process dice");
            diceService.processDice(message, originUser, bot);
        } else if (message.text() != null) {
            log.info("Process text");
            messagesService.saveMessage(originUser, message);
            orderService.checkOrders(message.chat().id(), originUser, message.text().trim(), message.messageId())
                    .forEach(action -> action.ifPresent(bot::execute));
        }

    }

    private void printCredits(Message message) {
        String msg = userRepository.findByChatId(message.chat().id()).stream()
                .map(userMapper::toDto)
                .map(u -> String.format("%s : %s", u.getUsername(), u.getSocialCredit()))
                .reduce("", (m, u2) -> m + "\n" + u2);
        log.info("Print Credits");
        bot.execute(new SendMessage(message.chat().id(), "Credits:\n" + msg));
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
