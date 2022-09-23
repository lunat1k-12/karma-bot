package com.coolguys.bot.listener;

import com.coolguys.bot.conf.BotConfig;
import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.QueryDataDto;
import com.coolguys.bot.dto.TelegramCasino;
import com.coolguys.bot.dto.TelegramDrugAction;
import com.coolguys.bot.dto.TelegramGuardDepartment;
import com.coolguys.bot.dto.TelegramPoliceDepartment;
import com.coolguys.bot.entity.TelegramChatEntity;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.repository.TelegramChatRepository;
import com.coolguys.bot.service.CasinoService;
import com.coolguys.bot.service.DateConverter;
import com.coolguys.bot.service.DiceService;
import com.coolguys.bot.service.DrugsService;
import com.coolguys.bot.service.GuardDepartmentService;
import com.coolguys.bot.service.GuardService;
import com.coolguys.bot.service.KarmaService;
import com.coolguys.bot.service.MessagesService;
import com.coolguys.bot.service.OrderService;
import com.coolguys.bot.service.PoliceDepartmentService;
import com.coolguys.bot.service.StealService;
import com.coolguys.bot.service.UserService;
import com.google.gson.Gson;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Chat;
import com.pengrad.telegrambot.model.ChatMemberUpdated;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.User;
import com.pengrad.telegrambot.request.DeleteMessage;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendSticker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.coolguys.bot.dto.QueryDataDto.DROP_DRUGS_TYPE;
import static com.coolguys.bot.dto.QueryDataDto.REPLY_ORDER_TYPE;
import static com.coolguys.bot.dto.QueryDataDto.STEAL_TYPE;

@Slf4j
@Component
public class MessagesListener implements UpdatesListener {
    private final OrderService orderService;
    private final DiceService diceService;
    private final KarmaService karmaService;
    private final UserService userService;
    private final MessagesService messagesService;
    private final StealService stealService;
    private final GuardService guardService;
    private final BotConfig botConfig;
    private final CasinoService casinoService;
    private final DrugsService drugsService;
    private final PoliceDepartmentService policeDepartmentService;
    private final GuardDepartmentService guardDepartmentService;
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;
    private final TelegramChatRepository telegramChatRepository;
    private final Map<Long, ExecutorService> chatExecutors = new HashMap<>();

    public static final String UNIQ_PLUS_ID = "AgADAgADf3BGHA";
    public static final String UNIQ_MINUS_ID = "AgADAwADf3BGHA";

    private final TelegramBot bot;

    @Autowired
    public MessagesListener(OrderService orderService,
                            DiceService diceService, KarmaService karmaService,
                            UserService userService, MessagesService messagesService,
                            StealService stealService, GuardService guardService,
                            BotConfig botConfig, CasinoService casinoService,
                            DrugsService drugsService, TelegramBot bot,
                            PoliceDepartmentService policeDepartmentService,
                            GuardDepartmentService guardDepartmentService,
                            ChatAccountRepository chatAccountRepository,
                            ChatAccountMapper chatAccountMapper, TelegramChatRepository telegramChatRepository) {
        this.orderService = orderService;
        this.diceService = diceService;
        this.karmaService = karmaService;
        this.userService = userService;
        this.messagesService = messagesService;
        this.stealService = stealService;
        this.guardService = guardService;
        this.botConfig = botConfig;
        this.casinoService = casinoService;
        this.drugsService = drugsService;
        this.chatAccountRepository = chatAccountRepository;
        this.chatAccountMapper = chatAccountMapper;
        this.telegramChatRepository = telegramChatRepository;
        this.bot = bot;
        this.policeDepartmentService = policeDepartmentService;
        this.guardDepartmentService = guardDepartmentService;
        log.info("Bot Token: {}", botConfig.getToken());
        bot.setUpdatesListener(this);
    }

    public void sendMessage(Long chatId, String message) {
        bot.execute(new SendMessage(chatId, message));
    }

    public void sendSticker(Long chatId, String stickerId) {
        bot.execute(new SendSticker(chatId, stickerId));
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
            if (update.message() != null) {
                if (Chat.Type.Private.equals(update.message().chat().type())) {
                    log.info("Private message from: {}", update.message().from());
                    bot.execute(new SendMessage(update.message().chat().id(), "Додай мене до чату і надай права адміна для того щоб почати гру."));
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
                        executeAction(query.message().chat().id(),
                                () -> orderService.checkOrders(query.message().chat().id(), originAcc,
                                        dto.getOption(), -1, OrderService.Income.DATA));
                        break;
                    case STEAL_TYPE:
                        log.info("Steal query");
                        executeAction(originAcc.getChat().getId(),
                                () -> stealService.processSteal(originAcc, dto));
                        break;
                    case DROP_DRUGS_TYPE:
                        log.info("Drop drugs query");
                        executeAction(originAcc.getChat().getId(),
                                () -> drugsService.processDropDrug(originAcc, dto));
                        break;
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
        if (message.leftChatMember() != null) {
            log.info("Deactivate user");
            executeAction(message.chat().id(),
                    () -> userService.deactivateChatAccount(message.leftChatMember().id(), message.chat().id()));
        } else if (isValidForCreditsCount(message)) {
            log.info("Process karma update");
            executeAction(originAccount.getChat().getId(),
                    () -> karmaService.processKarmaUpdate(message, originAccount));
        } else if (message.text() != null && botConfig.getCreditCommand().equals(message.text())) {
            log.info("Print Credits");
            executeAction(message.chat().id(), () -> printCredits(message));
        } else if (message.text() != null && botConfig.getAutoReplyCommand().equals(message.text())) {
            log.info("Create auto-reply");
            executeAction(originAccount.getChat().getId(),
                    () -> {
                        orderService.createReplyOrder(originAccount);
                        bot.execute(new DeleteMessage(message.chat().id(), message.messageId()));
                    });
        } else if (message.text() != null && botConfig.getRemovePlayBanCommand().equals(message.text())) {
            log.info("remove play ban command");
            executeAction(originAccount.getChat().getId(),
                    () -> diceService.removePlayBan(originAccount));
        } else if (message.text() != null && botConfig.getStealCommand().equals(message.text())) {
            log.info("New steal command");
            executeAction(message.chat().id(),
                    () -> {
                        stealService.stealRequest(originAccount);
                        bot.execute(new DeleteMessage(message.chat().id(), message.messageId()));
                    });
        } else if (message.text() != null && botConfig.getBuyGuardCommand().equals(message.text())) {
            log.info("Buy guard request");
            executeAction(originAccount.getChat().getId(),
                    () -> guardService.buyGuard(originAccount));
        } else if (message.text() != null && botConfig.getBuyCasinoCommand().equals(message.text())) {
            log.info("Buy Casino request");
            executeAction(originAccount.getChat().getId(),
                    () -> processCasinoBuy(originAccount));
        } else if (message.text() != null && botConfig.getDoDrugsCommand().equals(message.text())) {
            log.info("Do drugs request for {}", originAccount.getUser().getUsername());
            executeAction(originAccount.getChat().getId(),
                    () -> drugsService.doDrugs(originAccount));
        } else if (message.text() != null && botConfig.getDropDrugsCommand().equals(message.text())) {
            log.info("Drop drugs request from {}", originAccount.getUser().getUsername());
            executeAction(originAccount.getChat().getId(),
                    () -> drugsService.dropDrugsRequest(originAccount));
        } else if (message.text() != null && botConfig.getBuyPoliceCommand().equals(message.text())) {
            log.info("Buy police department request");
            executeAction(originAccount.getChat().getId(),
                    () -> processPdBuy(originAccount));
        } else if (message.text() != null && botConfig.getMyStatsCommand().equals(message.text())) {
            log.info("Print personal stats request");
            executeAction(originAccount.getChat().getId(),
                    () -> printPersonalStats(originAccount));
        } else if (message.text() != null && botConfig.getBuyGuardDepartmentCommand().equals(message.text())) {
            log.info("Buy Guard department request");
            executeAction(originAccount.getChat().getId(),
                    () -> processGdBuy(originAccount));
        } else if (message.dice() != null) {
            log.info("Process dice");
            executeAction(originAccount.getChat().getId(),
                    () -> diceService.processDice(message, originAccount));
        } else if (message.text() != null) {
            log.info("Process text");
            executeAction(originAccount.getChat().getId(),
                    () -> {
                        messagesService.saveMessage(originAccount, message);
                        orderService.checkOrders(message.chat().id(), originAccount, message.text().trim(),
                                message.messageId(), OrderService.Income.TEXT);
                    });
        }

    }

    private void processCasinoBuy(ChatAccount originAcc) {
        if (casinoService.buyCasino(originAcc)) {
            dropPoliceDepartmentIfExists(originAcc);
            dropGuardIfExists(originAcc);
        }
    }

    private void processPdBuy(ChatAccount originAcc) {
        if (policeDepartmentService.buyPoliceDepartment(originAcc)) {
            dropCasinoIfExists(originAcc);
            dropGuardIfExists(originAcc);
        }
    }

    private void processGdBuy(ChatAccount originAcc) {
        if (guardDepartmentService.buyGuardDepartment(originAcc)) {
            dropCasinoIfExists(originAcc);
            dropPoliceDepartmentIfExists(originAcc);
        }
    }

    private void dropGuardIfExists(ChatAccount originAcc) {
        TelegramGuardDepartment gd = guardDepartmentService.findOrCreateTelegramGdByChatID(originAcc.getChat().getId());
        if (gd.getOwner() != null && gd.getOwner().getId().equals(originAcc.getUser().getId())) {
            guardDepartmentService.dropGuardOwner(originAcc.getChat().getId());
            bot.execute(new SendMessage(originAcc.getChat().getId(),
                    String.format("@%s більше не властник охороного агенства", originAcc.getUser().getUsername())));
        }
    }

    private void dropCasinoIfExists(ChatAccount originAcc) {
        TelegramCasino casino = casinoService.findOrCreateTelegramCasinoByChatID(originAcc.getChat().getId());
        if (casino.getOwner() != null && casino.getOwner().getId().equals(originAcc.getUser().getId())) {
            casinoService.dropCasinoOwner(originAcc.getChat().getId());
            bot.execute(new SendMessage(originAcc.getChat().getId(),
                    String.format("@%s більше не властник казино", originAcc.getUser().getUsername())));
        }
    }

    private void dropPoliceDepartmentIfExists(ChatAccount originAcc) {
        TelegramPoliceDepartment pd = policeDepartmentService.findOrCreateTelegramPdByChatID(originAcc.getChat().getId());
        if (pd.getOwner() != null && pd.getOwner().getId().equals(originAcc.getUser().getId())) {
            policeDepartmentService.dropPdOwner(originAcc.getChat().getId());
            bot.execute(new SendMessage(originAcc.getChat().getId(),
                    String.format("@%s більше не властник поліцейської ділянки", originAcc.getUser().getUsername())));
        }
    }

    private void printPersonalStats(ChatAccount acc) {
        StringBuilder sb = new StringBuilder().append("Показники для @")
                .append(acc.getUser().getUsername())
                .append("\n****************");

        if (guardService.doesHaveGuard(acc)) {
            sb.append("\nмає охорону до:\n");
            sb.append(guardService.getGuardTillLabel(acc));
        }
        if (stealService.isInJail(acc)) {
            sb.append("\nУ в`язниці до:\n");
            sb.append(stealService.getJailTillLabel(acc));
        }

        List<TelegramDrugAction> drugs = drugsService.findActiveDrugDeals(acc);
        if (!drugs.isEmpty()) {
            sb.append("\nМає наркотики до:\n");
            sb.append(drugs.stream()
                    .max(Comparator.comparing(TelegramDrugAction::getExpires))
                    .map(TelegramDrugAction::getExpires)
                    .map(DateConverter::localDateTimeToStringLabel)
                    .orElse(null));
        }

        sb.append("\nКредити: ").append(acc.getSocialCredit());
        bot.execute(new SendMessage(acc.getChat().getId(), sb.toString()));
    }

    private void printCredits(Message message) {
        TelegramCasino casino = casinoService.findOrCreateTelegramCasinoByChatID(message.chat().id());
        TelegramPoliceDepartment pd = policeDepartmentService.findOrCreateTelegramPdByChatID(message.chat().id());
        TelegramGuardDepartment gd = guardDepartmentService.findOrCreateTelegramGdByChatID(message.chat().id());
        List<String> lines = chatAccountRepository.findByChatId(message.chat().id()).stream()
                .map(chatAccountMapper::toDto)
                .filter(ChatAccount::isActive)
                .sorted(Comparator.comparingInt(ChatAccount::getSocialCredit)
                        .reversed())
                .map(acc -> toStringInfo(acc, casino, pd, gd))
                .collect(Collectors.toList());

        if (lines.size() >= 1) {
            lines.set(0, "\uD83E\uDD47" + lines.get(0));
        }
        if (lines.size() >= 2) {
            lines.set(1, "\uD83E\uDD48" + lines.get(1));
        }
        if (lines.size() >= 3) {
            lines.set(2, "\uD83E\uDD49" + lines.get(2));
        }
        String msg = lines.stream()
                .reduce("", (m, u2) -> m + "\n" + u2);

        log.info("Print Credits");

        bot.execute(new SendMessage(message.chat().id(),
                "\uD83C\uDFE6 Рахунки:\n" +
                        msg +
                        "\n****************" +
                        "\nСумарний Банк: " +
                        stealService.creditsSum(message.chat().id()) +
                        "\nВартість казино: " + casino.getCurrentPrice() +
                        "\nВартість поліції: " + pd.getCurrentPrice() +
                        "\nВартість охоронки: " + gd.getCurrentPrice()));
    }

    private String toStringInfo(ChatAccount acc, TelegramCasino casino, TelegramPoliceDepartment pd, TelegramGuardDepartment gd) {
        StringBuilder sb = new StringBuilder(String.format("%s : %s ", acc.getUser().getUsername(), acc.getSocialCredit()));
        if (guardService.doesHaveGuard(acc)) {
            sb.append("⚔️");
        }
        if (stealService.isInJail(acc)) {
            sb.append("⛓");
        }
        if (casino.getOwner() != null && casino.getOwner().getId().equals(acc.getUser().getId())) {
            sb.append("\uD83C\uDFB0");
        }
        if (pd.getOwner() != null && pd.getOwner().getId().equals(acc.getUser().getId())) {
            sb.append("\uD83D\uDE94");
        }
        if (gd.getOwner() != null && gd.getOwner().getId().equals(acc.getUser().getId())) {
            sb.append("\uD83D\uDEE1");
        }
        if (!drugsService.findActiveDrugDeals(acc).isEmpty()) {
            sb.append("\uD83D\uDC89");
        }
        return sb.toString();
    }

    private void updateChatMember(ChatMemberUpdated chat) {
        log.info("Process new chat");

        telegramChatRepository.findById(chat.chat().id())
                .orElseGet(() -> telegramChatRepository.save(TelegramChatEntity.builder()
                        .name(chat.chat().title())
                        .id(chat.chat().id())
                        .premium(false)
                        .build()));

        bot.execute(new SendMessage(chat.chat().id(), "Привіт хлопці"));
    }
}
