package com.coolguys.bot.scheduled;

import com.coolguys.bot.dto.ChatAccount;
import com.coolguys.bot.dto.Poll;
import com.coolguys.bot.dto.PollStatus;
import com.coolguys.bot.dto.TelegramChat;
import com.coolguys.bot.dto.TelegramMessage;
import com.coolguys.bot.dto.TelegramUser;
import com.coolguys.bot.mapper.ChatAccountMapper;
import com.coolguys.bot.mapper.PollMapper;
import com.coolguys.bot.mapper.TelegramChatMapper;
import com.coolguys.bot.mapper.TelegramMessageMapper;
import com.coolguys.bot.repository.ChatAccountRepository;
import com.coolguys.bot.repository.PollRepository;
import com.coolguys.bot.repository.TelegramChatRepository;
import com.coolguys.bot.repository.TelegramMessageRepository;
import com.coolguys.bot.service.UserService;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.PollOption;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPoll;
import com.pengrad.telegrambot.request.StopPoll;
import com.pengrad.telegrambot.response.SendResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.pengrad.telegrambot.model.Poll.Type.quiz;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledPollService {

    private final TelegramChatRepository telegramChatRepository;
    private final UserService userService;
    private final TelegramMessageRepository telegramMessageRepository;
    private final TelegramMessageMapper telegramMessageMapper;
    private final TelegramChatMapper telegramChatMapper;
    private final PollRepository pollRepository;
    private final PollMapper pollMapper;
    private final ChatAccountRepository chatAccountRepository;
    private final ChatAccountMapper chatAccountMapper;
    private final TelegramBot bot;

    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(5);

    @Scheduled(cron = "00 00 14,15,17,20,22 * * *", zone = "Europe/Kiev")
    @Async
    public void processPolls() {
        telegramChatRepository.findAllActive().stream()
                .peek(ch -> log.info("Process poll in {}", ch.getName()))
                .map(telegramChatMapper::toDto)
                .forEach(this::processPollChat);
    }

    private void processPollChat(TelegramChat chat) {
        List<ChatAccount> users = userService.findActiveAccByChatId(chat.getId());

        if (users.isEmpty()) {
            log.info("no users in chat {}", chat.getName());
            return;
        }

        List<TelegramMessage> messages = telegramMessageRepository.findByChatIdAndUserIdsAndDate(chat.getId(), users.stream()
                        .map(ChatAccount::getUser)
                        .map(TelegramUser::getId)
                        .collect(Collectors.toList()),
                        LocalDateTime.now().minusDays(30)).stream()
                .map(telegramMessageMapper::toDto)
                .collect(Collectors.toList());

        if (messages.size() == 0) {
            log.info("No messages");
            return;
        }

        Random random = new Random();
        int index = random.nextInt(messages.size());

        TelegramMessage msg = messages.get(index);

        Poll poll = Poll.builder()
                .chat(chat)
                .status(PollStatus.IN_PROGRESS)
                .message(msg)
                .build();

        int rightIndex = 0;
        for (ChatAccount acc : users) {
            if (acc.getUser().getId().equals(msg.getUser().getId())) {
                break;
            }
            rightIndex++;
        }
        SendResponse response = bot.execute(new SendPoll(
                chat.getId(),
                String.format("Хто написав це повідомлення?\n%s", msg.getMessage()),
                users.stream()
                        .map(ChatAccount::getUser)
                        .map(TelegramUser::getUsername).toArray(String[]::new))
                .type(quiz)
                .correctOptionId(rightIndex)
                .isAnonymous(false)
                .explanation("У вас є 5 хвилин на відповідь"));

        poll.setTelegramPollId(response.message().messageId().toString());
        pollRepository.save(pollMapper.toEntity(poll));
        log.info("New Poll created in chat {}, for message id {}", chat.getName(), msg.getId());
        scheduledExecutor.schedule(this::processActivePolls, 5, TimeUnit.MINUTES);
    }

    private void processActivePolls() {
        telegramChatRepository.findAllActive().stream()
                .peek(ch -> log.info("Process active polls in {}", ch.getName()))
                .map(telegramChatMapper::toDto)
                .forEach(this::processActiveChatPolls);
    }

    private void processActiveChatPolls(TelegramChat chat) {
        List<Poll> polls = pollRepository.findActiveByChatId(chat.getId()).stream()
                .map(pollMapper::toDto)
                .collect(Collectors.toList());

        if (polls.isEmpty()) {
            log.info("No Active polls");
            return;
        }

        polls.forEach(this::processActivePoll);
    }

    private void processActivePoll(Poll poll) {
        log.info("poll updated: {}", poll.getId());
        var resp = bot.execute(new StopPoll(poll.getChat().getId(), Integer.parseInt(poll.getTelegramPollId())));
        if (resp.isOk()) {
            log.info("poll: {} - is closed", poll.getId());
            poll.setStatus(PollStatus.DONE);
            pollRepository.save(pollMapper.toEntity(poll));

            if (resp.poll().totalVoterCount().equals(0)) {
                bot.execute(new SendMessage(poll.getChat().getId(), "Ніхто не проголосував \uD83D\uDE14"));
                return;
            }

            PollOption rightOption = resp.poll().options()[resp.poll().correctOptionId()];

            boolean isWin = true;
            for (PollOption option : resp.poll().options()) {
                if (rightOption.equals(option)) {
                    continue;
                }
                if (rightOption.voterCount() <= option.voterCount()) {
                    isWin = false;
                    break;
                }
            }

            if (isWin) {
                bot.execute(new SendMessage(poll.getChat().getId(), "Більшість відповіла вірно!\nВсім по 50 кредитів!"));
                List<ChatAccount> users = userService.findActiveAccByChatId(poll.getChat().getId());
                users.stream()
                        .peek(u -> u.plusCredit(50))
                        .map(chatAccountMapper::toEntity)
                        .forEach(chatAccountRepository::save);
            } else {
                bot.execute(new SendMessage(poll.getChat().getId(), "Більшість не відповіла вірно!\nВсім по мінус 50 кредитів!"));
                List<ChatAccount> users = userService.findActiveAccByChatId(poll.getChat().getId());
                users.stream()
                        .peek(u -> u.minusCredit(50))
                        .map(chatAccountMapper::toEntity)
                        .forEach(chatAccountRepository::save);
            }
        }
    }
}
