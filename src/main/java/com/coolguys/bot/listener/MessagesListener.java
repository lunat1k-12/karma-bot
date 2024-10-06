package com.coolguys.bot.listener;

import com.coolguys.bot.conf.BotConfig;
import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.QueryDataDto;
import com.coolguys.bot.entity.TelegramChatEntity;
import com.coolguys.bot.repository.TelegramChatRepository;
import com.coolguys.bot.service.DiceService;
import com.coolguys.bot.service.DoctorService;
import com.coolguys.bot.service.DrugsService;
import com.coolguys.bot.service.KarmaService;
import com.coolguys.bot.service.MessagesService;
import com.coolguys.bot.service.OrderService;
import com.coolguys.bot.service.PrivateChatService;
import com.coolguys.bot.service.StealService;
import com.coolguys.bot.service.ThiefInvestigateService;
import com.coolguys.bot.service.UserService;
import com.coolguys.bot.service.command.CommandProcessor;
import com.coolguys.bot.service.external.ZodiacService;
import com.coolguys.bot.service.metrics.AwsMetricsService;
import com.coolguys.bot.service.role.RoleProcessor;
import com.coolguys.bot.service.role.RoleService;
import com.google.gson.Gson;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.ChatMemberUpdated;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.model.message.origin.MessageOriginChannel;
import com.pengrad.telegrambot.model.request.ChatAction;
import com.pengrad.telegrambot.request.SendChatAction;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendSticker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.coolguys.bot.dto.QueryDataDto.DOCTOR_DISEASE_TYPE;
import static com.coolguys.bot.dto.QueryDataDto.DROP_DRUGS_TYPE;
import static com.coolguys.bot.dto.QueryDataDto.REPLY_ORDER_TYPE;
import static com.coolguys.bot.dto.QueryDataDto.ROLE_ACTION_TYPE;
import static com.coolguys.bot.dto.QueryDataDto.ROLE_SELECT_TYPE;
import static com.coolguys.bot.dto.QueryDataDto.STEAL_TYPE;
import static com.coolguys.bot.dto.QueryDataDto.THIEF_INVESTIGATE_TYPE;
import static com.coolguys.bot.dto.QueryDataDto.ZODIAC_TYPE;

@Slf4j
@Component
public class MessagesListener implements UpdatesListener {
    private final OrderService orderService;
    private final DiceService diceService;
    private final KarmaService karmaService;
    private final UserService userService;
    private final MessagesService messagesService;
    private final StealService stealService;
    private final DrugsService drugsService;
    private final TelegramChatRepository telegramChatRepository;
    private final PrivateChatService privateChatService;
    private final RoleService roleService;
    private final RoleProcessor roleProcessor;
    private final CommandProcessor commandProcessor;
    private final ThiefInvestigateService thiefInvestigateService;
    private final DoctorService doctorService;
    private final ZodiacService zodiacService;
    private final AwsMetricsService awsMetricsService;
    private final Map<Long, ExecutorService> chatExecutors = new HashMap<>();

    public static final String UNIQ_PLUS_ID = "AgADAgADf3BGHA";
    public static final String UNIQ_MINUS_ID = "AgADAwADf3BGHA";

    private final TelegramBot bot;

    @Autowired
    public MessagesListener(OrderService orderService,
                            DiceService diceService, KarmaService karmaService,
                            UserService userService, MessagesService messagesService,
                            StealService stealService,
                            DrugsService drugsService, TelegramBot bot,
                            BotConfig botConfig, ThiefInvestigateService thiefInvestigateService,
                            TelegramChatRepository telegramChatRepository,
                            PrivateChatService privateChatService, RoleService roleService,
                            RoleProcessor roleProcessor, CommandProcessor commandProcessor,
                            DoctorService doctorService, ZodiacService zodiacService,
                            AwsMetricsService awsMetricsService) {
        this.orderService = orderService;
        this.diceService = diceService;
        this.karmaService = karmaService;
        this.userService = userService;
        this.messagesService = messagesService;
        this.stealService = stealService;
        this.drugsService = drugsService;
        this.telegramChatRepository = telegramChatRepository;
        this.privateChatService = privateChatService;
        this.commandProcessor = commandProcessor;
        this.thiefInvestigateService = thiefInvestigateService;
        this.bot = bot;
        this.roleService = roleService;
        this.roleProcessor = roleProcessor;
        this.doctorService = doctorService;
        this.zodiacService = zodiacService;
        this.awsMetricsService = awsMetricsService;
        log.info("Bot Token: {}", botConfig.getToken());
        bot.setUpdatesListener(this);
    }

    public void sendMessage(Long chatId, String message) {
        bot.execute(new SendMessage(chatId, message));
    }

    public void sendSticker(Long chatId, String stickerId) {
        bot.execute(new SendSticker(chatId, stickerId));
    }

    public void sendChatAction(Long chatId, ChatAction action) {
        bot.execute(new SendChatAction(chatId, action));
    }

    private void executeAction(Long chatId, Runnable command) {
        if (chatExecutors.get(chatId) == null) {
            chatExecutors.put(chatId, Executors.newSingleThreadExecutor());
        }

        chatExecutors.get(chatId).execute(command);
    }

    @Override
    public int process(List<Update> updates) {
        log.info("income: {}", updates);
        updates.forEach(update -> {
            if (update.myChatMember() != null && !Chat.Type.Private.equals(update.myChatMember().chat().type())) {
                updateChatMember(update.myChatMember());
            }
            if (update.poll() != null) {
                log.info("Process Poll");
            }
            if (update.message() != null) {
                if (update.message().forwardOrigin() != null && "channel".equals(update.message().forwardOrigin().type())) {
                    MessageOriginChannel channel = (MessageOriginChannel) update.message().forwardOrigin();
                    log.info("Forwarded from channel: {}",channel.chat().username());
                    awsMetricsService.sendForwardMetric(channel.chat().username());
                }
                if (update.message().from().isBot()) {
                    log.info("Message from bot will be ignored");
                    return;
                }

                if (Chat.Type.Private.equals(update.message().chat().type())) {
                    privateChatService.processPrivateMessage(update.message());
                } else {
                    processMessage(update.message());
                }
            }
            if (update.callbackQuery() != null) {
                CallbackQuery query = update.callbackQuery();
                log.info("Query: {}", update.callbackQuery());
                Gson gson = new Gson();
                QueryDataDto dto = gson.fromJson(query.data(), QueryDataDto.class);
                ChatAccount originAcc = userService.loadChatAccount(query);
                switch (dto.getType()) {
                    case REPLY_ORDER_TYPE:
                        log.info("Reply order query");
                        executeAction(query.maybeInaccessibleMessage().chat().id(),
                                () -> orderService.checkOrders(query.maybeInaccessibleMessage(), dto.getOption(), originAcc, OrderService.Income.DATA));
                        break;
                    case STEAL_TYPE:
                        log.info("Steal query");
                        executeAction(originAcc.getChat().getId(),
                                () -> stealService.processSteal(originAcc, dto, query.maybeInaccessibleMessage().messageId()));
                        break;
                    case DROP_DRUGS_TYPE:
                        log.info("Drop drugs query");
                        executeAction(originAcc.getChat().getId(),
                                () -> drugsService.processDropDrug(originAcc, dto, query.maybeInaccessibleMessage().messageId()));
                        break;
                    case ROLE_SELECT_TYPE:
                        log.info("Role select query");
                        executeAction(originAcc.getChat().getId(),
                                () -> roleService.processRoleSelection(originAcc, dto, query.maybeInaccessibleMessage().messageId()));
                        break;
                    case ROLE_ACTION_TYPE:
                        log.info("Role Action selected");
                        executeAction(originAcc.getChat().getId(),
                                () -> roleProcessor.processAction(originAcc, dto, query.maybeInaccessibleMessage().messageId()));
                        break;
                    case THIEF_INVESTIGATE_TYPE:
                        log.info("Thief investigate type");
                        executeAction(originAcc.getChat().getId(),
                                () -> thiefInvestigateService.processInvestigate(originAcc, dto, query.maybeInaccessibleMessage().messageId()));
                        break;
                    case DOCTOR_DISEASE_TYPE:
                        log.info("Doctor disease type");
                        executeAction(originAcc.getChat().getId(),
                                () -> doctorService.processDocSelection(originAcc, dto, query.maybeInaccessibleMessage().messageId()));
                        break;
                    case ZODIAC_TYPE:
                        log.info("Select zodiac type");
                        executeAction(originAcc.getChat().getId(),
                                () -> zodiacService.processZodiacSelection(originAcc, dto, query.maybeInaccessibleMessage().messageId()));
                }
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
        ChatAccount originAccount = userService.loadChatAccount(message);
        awsMetricsService.sendMessageMetric(originAccount.getUser().getUsername());
        if (message.leftChatMember() != null) {
            log.info("Deactivate user");
            executeAction(message.chat().id(),
                    () -> userService.deactivateChatAccount(message.leftChatMember(), message.chat().id()));
        } else if (isValidForCreditsCount(message)) {
            log.info("Process karma update");
            executeAction(originAccount.getChat().getId(),
                    () -> karmaService.processKarmaUpdate(message, originAccount));
        } else if (commandProcessor.isCommandExists(message)) {
            executeAction(originAccount.getChat().getId(),
                    () -> commandProcessor.processCommand(message, originAccount));
        } else if (message.dice() != null) {
            log.info("Process dice");
            executeAction(originAccount.getChat().getId(),
                    () -> diceService.processDice(message, originAccount));
        } else if (message.text() != null) {
            log.info("Process text");
            executeAction(originAccount.getChat().getId(),
                    () -> {
                        messagesService.saveMessage(originAccount, message);
                        orderService.checkOrders(message, message.text().trim(), originAccount, OrderService.Income.TEXT);
                    });
        } else if (message.sticker() != null) {
            log.info("Process sticker");
            executeAction(originAccount.getChat().getId(),
                    () -> orderService.checkOrders(message, null, originAccount, OrderService.Income.TEXT));
        }

    }

    private void updateChatMember(ChatMemberUpdated chat) {
        log.info("Process new chat");

        TelegramChatEntity entity = telegramChatRepository.findById(chat.chat().id())
                .orElseGet(() -> telegramChatRepository.save(TelegramChatEntity.builder()
                        .name(chat.chat().title())
                        .id(chat.chat().id())
                        .active(true)
                        .premium(false)
                        .build()));
        if (!entity.getActive()) {
            entity.setActive(true);
            telegramChatRepository.save(entity);
        }

        bot.execute(new SendMessage(chat.chat().id(), "Привіт хлопці"));
    }
}
