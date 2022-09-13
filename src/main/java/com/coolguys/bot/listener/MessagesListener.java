package com.coolguys.bot.listener;

import com.coolguys.bot.conf.BotConfig;
import com.coolguys.bot.dto.CasinoDto;
import com.coolguys.bot.dto.DrugAction;
import com.coolguys.bot.dto.GuardDepartmentDto;
import com.coolguys.bot.dto.PoliceDepartmentDto;
import com.coolguys.bot.dto.QueryDataDto;
import com.coolguys.bot.dto.UserInfo;
import com.coolguys.bot.entity.ChatEntity;
import com.coolguys.bot.mapper.UserMapper;
import com.coolguys.bot.repository.ChatRepository;
import com.coolguys.bot.repository.UserRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.coolguys.bot.dto.QueryDataDto.DROP_DRUGS_TYPE;
import static com.coolguys.bot.dto.QueryDataDto.REPLY_ORDER_TYPE;
import static com.coolguys.bot.dto.QueryDataDto.STEAL_TYPE;

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
    private final StealService stealService;
    private final GuardService guardService;
    private final BotConfig botConfig;
    private final CasinoService casinoService;
    private final DrugsService drugsService;
    private final PoliceDepartmentService policeDepartmentService;
    private final GuardDepartmentService guardDepartmentService;

    public static final String UNIQ_PLUS_ID = "AgADAgADf3BGHA";
    public static final String UNIQ_MINUS_ID = "AgADAwADf3BGHA";

    private final TelegramBot bot;

    @Autowired
    public MessagesListener(ChatRepository chatRepository, UserRepository userRepository,
                            UserMapper userMapper, OrderService orderService,
                            DiceService diceService, KarmaService karmaService,
                            UserService userService, MessagesService messagesService,
                            StealService stealService, GuardService guardService,
                            BotConfig botConfig, CasinoService casinoService,
                            DrugsService drugsService, TelegramBot bot,
                            PoliceDepartmentService policeDepartmentService,
                            GuardDepartmentService guardDepartmentService) {
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
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
            if (update.callbackQuery() != null) {
                CallbackQuery query = update.callbackQuery();
                log.info("Query: {}", update.callbackQuery());
                Gson gson = new Gson();
                QueryDataDto dto = gson.fromJson(query.data(), QueryDataDto.class);
                UserInfo originUser = userService.loadUser(query);
                switch (dto.getType()) {
                    case REPLY_ORDER_TYPE:
                        log.info("Reply order query");
                        orderService.checkOrders(query.message().chat().id(), originUser,
                                dto.getOption(), -1, OrderService.Income.DATA);
                        break;
                    case STEAL_TYPE:
                        log.info("Steal query");
                        stealService.processPerChatAsyncSteal(originUser, dto);
                        break;
                    case DROP_DRUGS_TYPE:
                        log.info("Drop drugs query");
                        drugsService.processDropDrug(originUser, dto);
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
        UserInfo originUser = userService.loadUser(message);
        if (message.leftChatMember() != null) {
            log.info("Deactivate user");
            String username = userService.getOriginUsername(message.leftChatMember());
            userRepository.findByUsernameAndChatId(username, message.chat().id())
                    .map(userMapper::toDto)
                    .ifPresent(userService::deactivateUser);
        } else if (isValidForCreditsCount(message)) {
            log.info("Process karma update");
            karmaService.processKarmaUpdate(message, originUser);
        } else if (message.text() != null && botConfig.getCreditCommand().equals(message.text())) {
            log.info("Print Credits");
            printCredits(message);
        } else if (message.text() != null && botConfig.getAutoReplyCommand().equals(message.text())) {
            log.info("Create auto-reply");
            orderService.createReplyOrder(originUser);
            bot.execute(new DeleteMessage(message.chat().id(), message.messageId()));
        } else if (message.text() != null && botConfig.getRemovePlayBanCommand().equals(message.text())) {
            log.info("remove play ban command");
            diceService.removePlayBan(originUser);
        } else if (message.text() != null && botConfig.getStealCommand().equals(message.text())) {
            log.info("New steal command");
            stealService.stealRequest(originUser);
            bot.execute(new DeleteMessage(message.chat().id(), message.messageId()));
        } else if (message.text() != null && botConfig.getBuyGuardCommand().equals(message.text())) {
            log.info("Buy guard request");
            guardService.buyGuard(originUser);
        } else if (message.text() != null && botConfig.getBuyCasinoCommand().equals(message.text())) {
            log.info("Buy Casino request");
            processCasinoBuy(originUser);
        } else if (message.text() != null && botConfig.getDoDrugsCommand().equals(message.text())) {
            log.info("Do drugs request for {}", originUser.getUsername());
            drugsService.doDrugs(originUser);
        } else if (message.text() != null && botConfig.getDropDrugsCommand().equals(message.text())) {
            log.info("Drop drugs request from {}", originUser.getUsername());
            drugsService.dropDrugsRequest(originUser);
        } else if (message.text() != null && botConfig.getBuyPoliceCommand().equals(message.text())) {
            log.info("Buy police department request");
            processPdBuy(originUser);
        } else if (message.text() != null && botConfig.getMyStatsCommand().equals(message.text())) {
            log.info("Print personal stats request");
            printPersonalStats(originUser);
        } else if (message.text() != null && botConfig.getBuyGuardDepartmentCommand().equals(message.text())) {
            log.info("Buy Guard department request");
            processGdBuy(originUser);
        } else if (message.dice() != null) {
            log.info("Process dice");
            diceService.processDice(message, originUser);
        } else if (message.text() != null) {
            log.info("Process text");
            messagesService.saveMessage(originUser, message);
            orderService.checkOrders(message.chat().id(), originUser, message.text().trim(),
                    message.messageId(), OrderService.Income.TEXT);
        }

    }

    private void processCasinoBuy(UserInfo originUser) {
        if (casinoService.buyCasino(originUser)) {
            dropPoliceDepartmentIfExists(originUser);
            dropGuardIfExists(originUser);
        }
    }

    private void processPdBuy(UserInfo originUser) {
        if (policeDepartmentService.buyPoliceDepartment(originUser)) {
            dropCasinoIfExists(originUser);
            dropGuardIfExists(originUser);
        }
    }

    private void processGdBuy(UserInfo originUser) {
        if (guardDepartmentService.buyGuardDepartment(originUser)) {
            dropCasinoIfExists(originUser);
            dropPoliceDepartmentIfExists(originUser);
        }
    }

    private void dropGuardIfExists(UserInfo originUser) {
        GuardDepartmentDto gd = guardDepartmentService.findOrCreateGdByChatID(originUser.getChatId());
        if (gd.getOwner() != null && gd.getOwner().getId().equals(originUser.getId())) {
            guardDepartmentService.dropGuardOwner(originUser.getChatId());
            bot.execute(new SendMessage(originUser.getChatId(),
                    String.format("@%s більше не властник охороного агенства", originUser.getUsername())));
        }
    }
    private void dropCasinoIfExists(UserInfo originUser) {
        CasinoDto casino = casinoService.findOrCreateCasinoByChatID(originUser.getChatId());
        if (casino.getOwner() != null && casino.getOwner().getId().equals(originUser.getId())) {
            casinoService.dropCasinoOwner(originUser.getChatId());
            bot.execute(new SendMessage(originUser.getChatId(),
                    String.format("@%s більше не властник казино", originUser.getUsername())));
        }
    }

    private void dropPoliceDepartmentIfExists(UserInfo originUser) {
        PoliceDepartmentDto pd = policeDepartmentService.findOrCreatePdByChatID(originUser.getChatId());
        if (pd.getOwner() != null && pd.getOwner().getId().equals(originUser.getId())) {
            policeDepartmentService.dropPdOwner(originUser.getChatId());
            bot.execute(new SendMessage(originUser.getChatId(),
                    String.format("@%s більше не властник поліцейської ділянки", originUser.getUsername())));
        }
    }

    private void printPersonalStats(UserInfo user) {
        StringBuilder sb = new StringBuilder().append("Показники для @")
                .append(user.getUsername())
                .append("\n****************");

        if (guardService.doesHaveGuard(user)) {
            sb.append("\nмає охорону до:\n");
            sb.append(guardService.getGuardTillLabel(user));
        }
        if (stealService.isInJail(user)) {
            sb.append("\nУ в`язниці до:\n");
            sb.append(stealService.getJailTillLabel(user));
        }

        List<DrugAction> drugs = drugsService.findActiveDrugDeals(user);
        if (!drugs.isEmpty()) {
            sb.append("\nМає наркотики до:\n");
            sb.append(drugs.stream()
                    .max(Comparator.comparing(DrugAction::getExpires))
                    .map(DrugAction::getExpires)
                    .map(DateConverter::localDateTimeToStringLabel)
                    .orElse(null));
        }

        sb.append("\nКредити: ").append(user.getSocialCredit());
        bot.execute(new SendMessage(user.getChatId(), sb.toString()));
    }
    private void printCredits(Message message) {
        CasinoDto casino = casinoService.findOrCreateCasinoByChatID(message.chat().id());
        PoliceDepartmentDto pd = policeDepartmentService.findOrCreatePdByChatID(message.chat().id());
        GuardDepartmentDto gd = guardDepartmentService.findOrCreateGdByChatID(message.chat().id());
        List<String> lines = userRepository.findByChatId(message.chat().id()).stream()
                .map(userMapper::toDto)
                .filter(UserInfo::isActive)
                .sorted(Comparator.comparingInt(UserInfo::getSocialCredit)
                        .reversed())
                .map(u -> toStringInfo(u, casino, pd, gd))
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

    private String toStringInfo(UserInfo user, CasinoDto casino, PoliceDepartmentDto pd, GuardDepartmentDto gd) {
        StringBuilder sb = new StringBuilder(String.format("%s : %s ", user.getUsername(), user.getSocialCredit()));
        if (guardService.doesHaveGuard(user)) {
            sb.append("⚔️");
        }
        if (stealService.isInJail(user)) {
            sb.append("⛓");
        }
        if (casino.getOwner() != null && casino.getOwner().getId().equals(user.getId())) {
            sb.append("\uD83C\uDFB0");
        }
        if (pd.getOwner() != null && pd.getOwner().getId().equals(user.getId())) {
            sb.append("\uD83D\uDE94");
        }
        if (gd.getOwner() != null && gd.getOwner().getId().equals(user.getId())) {
            sb.append("\uD83D\uDEE1");
        }
        if (!drugsService.findActiveDrugDeals(user).isEmpty()) {
            sb.append("\uD83D\uDC89");
        }
        return sb.toString();
    }

    private void updateChatMember(ChatMemberUpdated chat) {
        log.info("Process new chat");

        chatRepository.findByTelegramId(chat.chat().id())
                .orElseGet(() -> chatRepository.save(ChatEntity.builder()
                        .name(chat.chat().title())
                        .telegramId(chat.chat().id())
                        .build()));

        bot.execute(new SendMessage(chat.chat().id(), "Привіт хлопці"));
    }
}
