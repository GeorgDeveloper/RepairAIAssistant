package ru.georgdeveloper.assistanttelegram.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.georgdeveloper.assistanttelegram.bot.RepairAssistantBot;

@Configuration
public class BotConfig {
    
    @Autowired
    private RepairAssistantBot repairAssistantBot;
    
    @Bean
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(repairAssistantBot);
        return api;
    }
}