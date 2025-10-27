package com.mybot; 

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import io.github.cdimascio.dotenv.Dotenv;

public class Main {

    public static void main(String[] a) {
        
        Dotenv dotenv = Dotenv.load();

        String t = dotenv.get("BOT_TOKEN"); 
        String u = dotenv.get("BOT_USERNAME"); 
        String cs = dotenv.get("DB_CONNECTION_STRING");
        String db = dotenv.get("DB_NAME");
        
        try {
            MongoService ms = new MongoService(cs, db);
            TelegramBotsApi ba = new TelegramBotsApi(DefaultBotSession.class);
            
            ba.registerBot(new PhotographerBot(t, u, ms));
            
            System.out.println("Бот успішно запущен!");

        } catch (TelegramApiException e) {
            System.err.println("Помилка запуска бота: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Помилка підключення до MongoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }
}