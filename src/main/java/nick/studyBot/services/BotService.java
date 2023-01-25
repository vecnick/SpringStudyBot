package nick.studyBot.services;

import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import nick.studyBot.config.BotConfig;
import nick.studyBot.models.User;
import nick.studyBot.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class BotService extends TelegramLongPollingBot {


    private final UserRepository userRepository;

    private final ModelMapper modelMapper;

    private final BotConfig config;

    static final String HELP_TEXT = "This bot is created to study how to use Spring capabilities.\n\n" +
            "You can execute commands from the main menu on the left or by typing command:\n\n" +
            "Type /start to see welcome message\n\n" +
            "Type /data to see data stored about yourself\n\n" +
            "Type /daletedata to delete your data from storage\n\n" +
            "Type /setting to set your preferences\n\n" +
            "Type /help to see this message again";

    @Autowired
    public BotService(UserRepository userRepository, ModelMapper modelMapper, BotConfig config) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start","get a welcome message"));
        listOfCommands.add(new BotCommand("/data","get your data stored"));
        listOfCommands.add(new BotCommand("/deletedata","delete data stored"));
        listOfCommands.add(new BotCommand("/help","info how to use this bot"));
        listOfCommands.add(new BotCommand("/settings","set your preferences"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e){
            log.error("Error setting bot's command: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {

        if(update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start" -> {
                    registerUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                }
                case "/help" -> sendMessage(chatId, HELP_TEXT);
                default -> sendMessage(chatId, "Sorry, command was not recognized");
            }
        }
    }

    private void registerUser(Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()){
            User user = convertToUser(msg.getChat());
            user.setChatId(msg.getChatId());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));
            userRepository.save(user);
            log.info("user saved: " + user);
        }
    }

    private void startCommandReceived(long chatId, String name) {

        String answer = EmojiParser.parseToUnicode("Hi, " + name + ", nice to meet you! " +
                ":blush:");

        log.info("Replied to user " + name);

        sendMessage(chatId,answer);
    }

    private void  sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        try{
            execute(message);
        } catch (TelegramApiException e){
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private User convertToUser(Chat chat) {
        return modelMapper.map(chat,User.class);
    }
}
