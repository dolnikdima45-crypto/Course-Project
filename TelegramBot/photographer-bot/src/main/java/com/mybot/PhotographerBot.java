package com.mybot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PhotographerBot extends TelegramLongPollingBot {

    private final String bu;
    private final MongoService ms;
    private Map<Long, String> us = new HashMap<>();
    private Map<Long, BC> bip = new HashMap<>();

    private static class BC {
        String s;
        String d;
        String t;
    }

    private final Map<String, String> s = Map.of(
            "portrait", "Портретна зйомка (1 година)",
            "family", "Сімейна зйомка (1.5 години)",
            "reportage", "Репортаж (від 2 годин)"
    );

    private final List<String> ah = List.of(
            "10:00", "11:00", "12:00", "14:00", "15:00", "16:00", "17:00"
    );


    public PhotographerBot(String bt, String bun, MongoService msi) {
        super(bt);
        this.bu = bun;
        this.ms = msi;
    }
    
    @Override
    public void onUpdateReceived(Update u) {
        long cid = 0; 
        try {
            if (u.hasCallbackQuery()) {
                String cbd = u.getCallbackQuery().getData();
                cid = u.getCallbackQuery().getMessage().getChatId(); 
                long mid = u.getCallbackQuery().getMessage().getMessageId();
                
                hcq(cid, mid, cbd);
            
            } else if (u.hasMessage() && u.getMessage().hasText()) {
                String mt = u.getMessage().getText();
                cid = u.getMessage().getChatId(); 
                User usr = u.getMessage().getFrom();
                
                us.remove(cid);
                bip.remove(cid);

                htm(cid, usr, mt);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (cid != 0) {
                sm(cid, "Виникла внутрішня помилка. Будь ласка, спробуйте пізніше.");
            } else {
                System.err.println("Не вдалося визначити ChatId для відправки повідомлення про помилку.");
            }
        }
    }


    private void htm(long cid, User usr, String mt) {
        switch (mt) {
            case "/start":
                ssm(cid);
                break;
            case "/book":
                sbf(cid);
                break;
            case "/my":
                sua(cid);
                break;
            case "/services":
                ssvm(cid);
                break;
            case "/info":
                sim(cid);
                break;
            default:
                if (mt.startsWith("/book ") || mt.startsWith("/delete ")) {
                    sm(cid, "Цей формат команди більше не підтримується. " +
                            "Будь ласка, використовуйте інтерактивні кнопки, почавши з команди /book або /my.");
                } else {
                    sm(cid, "Незрозуміла команда. \n" +
                            "Надішліть /start, щоб побачити список доступних команд.");
                }
                break;
        }
    }

    private void hcq(long cid, long mid, String cbd) {
        String st = us.getOrDefault(cid, "");

        if (cbd.startsWith("book_service_")) {
            String sk = cbd.split("_")[2];
            BC c = new BC();
            c.s = s.get(sk);
            bip.put(cid, c);
            us.put(cid, "WAITING_FOR_DATE");
            afd(cid, mid);
        
        } else if (cbd.startsWith("book_date_")) {
            if (!st.equals("WAITING_FOR_DATE")) return;
            String d = cbd.split("_")[2];
            bip.get(cid).d = d;
            us.put(cid, "WAITING_FOR_TIME");
            aft(cid, mid, d);

        } else if (cbd.startsWith("book_time_")) {
            if (!st.equals("WAITING_FOR_TIME")) return;
            String t = cbd.split("_")[2];
            bip.get(cid).t = t;
            us.put(cid, "WAITING_FOR_CONFIRMATION");
            afc(cid, mid);

        } else if (cbd.equals("book_confirm")) {
            if (!st.equals("WAITING_FOR_CONFIRMATION")) return;
            cbk(cid, mid);

        } else if (cbd.equals("book_cancel")) {
            us.remove(cid);
            bip.remove(cid);
            em(cid, mid, "Запис скасовано.");
        
        } else if (cbd.startsWith("delete_") && 
                   !cbd.startsWith("delete_confirm_") && 
                   !cbd.equals("delete_cancel")) {
            
            String[] p = cbd.split("_"); 
            String d = p[1];
            String t = p[2];
            
            afdc(cid, mid, d, t);
        
        } else if (cbd.startsWith("delete_confirm_")) {
            String[] p = cbd.split("_");
            String d = p[2];
            String t = p[3];
            
            boolean sc = ms.da(cid, d, t);
            if (sc) {
                em(cid, mid, "Ваш запис на " + d + " о " + t + " успішно скасовано.");
            } else {
                em(cid, mid, "Не вдалося скасувати запис. Можливо, його вже було скасовано.");
            }

        } else if (cbd.equals("delete_cancel")) {
            sua(cid);
            dm(cid, (int) mid);
        }
    }

    private void ssm(long cid) {
        String t = "Вітаю! Я бот для запису до фотографа.\n\n" +
                "Ось що я вмію:\n\n" +
                "*/book* - Інтерактивний запис на фотосесію.\n" +
                "*/my* - Переглянути або скасувати мої записи.\n" +
                "*/services* - Дізнатися про послуги та ціни.\n" +
                "*/info* - Контакти та інша корисна інформація.";
        
        SendMessage m = new SendMessage(String.valueOf(cid), t);
        m.setParseMode("Markdown");
        exm(m);
    }
    
    private void ssvm(long cid) {
        String t = "Мої послуги:\n\n" +
                      "*Портретна зйомка (1 година)*\n" +
                      "Індивідуальна фотосесія в студії або на локації.\n" +
                      "*Ціна:* 2500 грн.\n\n" +

                      "*Сімейна зйомка (1.5 години)*\n" +
                      "Зйомка для всієї родини, до 5 осіб.\n" +
                      "*Ціна:* 3500 грн.\n\n" +

                      "*Репортаж (від 2 годин)*\n" +
                      "Зйомка вашої події (день народження, хрестини тощо).\n" +
                      "*Ціна:* 1500 грн/година (мінімальне замовлення 2 години).";
        
        SendMessage m = new SendMessage(String.valueOf(cid), t);
        m.setParseMode("Markdown");
        exm(m);
    }
    
    private void sim(long cid) {
        String t = "*Контакти:*\n" +
                      "Фотограф: Дмитро Дольнік\n" + 
                      "Телефон: +380 99 930 86 94\n\n" + 

                      "*Адреса студії:*\n" +
                      "вулиця Гоголя, 38, Полтава, Полтавська область, 36000\n" +
                      "[Посилання на Google Maps](https://maps.google.com/)\n\n" +

                      "*FAQ - Часті питання:*\n" +
                      "*Як підготуватися до зйомки?*\n" +
                      "Одягніть зручний одяг нейтральних кольорів без яскравих принтів. " +
                      "Найголовніше - гарний настрій та достатньо сну.";
        
        SendMessage m = new SendMessage(String.valueOf(cid), t);
        m.setParseMode("Markdown");
        m.disableWebPagePreview();
        exm(m);
    }

    private void sua(long cid) {
        var ap = ms.gua(cid);
        if (ap.isEmpty()) {
            sm(cid, "У вас поки що немає записів.");
            return;
        }

        StringBuilder r = new StringBuilder("Ваші записи:\n");
        
        InlineKeyboardMarkup km = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rs = new ArrayList<>();

        for (var d : ap) {
            String dt = d.getString("date");
            String tm = d.getString("time");
            String srv = d.getString("service_type");

            r.append(String.format("- %s, %s (%s)\n", dt, tm, srv));
            
            String cbd = String.format("delete_%s_%s", dt, tm);
            
            InlineKeyboardButton b = new InlineKeyboardButton();
            b.setText("Скасувати: " + dt + " " + tm);
            b.setCallbackData(cbd);
            
            rs.add(Collections.singletonList(b));
        }
        
        km.setKeyboard(rs); 

        SendMessage m = new SendMessage(String.valueOf(cid), r.toString());
        m.setReplyMarkup(km);
        exm(m);
    }

    private void sbf(long cid) {
        us.put(cid, "WAITING_FOR_SERVICE");
        
        InlineKeyboardMarkup km = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rs = new ArrayList<>();
        
        for (Map.Entry<String, String> e : s.entrySet()) {
            InlineKeyboardButton b = new InlineKeyboardButton();
            b.setText(e.getValue());
            b.setCallbackData("book_service_" + e.getKey());
            rs.add(Collections.singletonList(b));
        }
        
        InlineKeyboardButton cb = new InlineKeyboardButton();
        cb.setText("Скасувати");
        cb.setCallbackData("book_cancel");
        rs.add(Collections.singletonList(cb));

        km.setKeyboard(rs);

        SendMessage m = new SendMessage(String.valueOf(cid), "Яку послугу ви б хотіли?");
        m.setReplyMarkup(km);
        exm(m);
    }

    private void afd(long cid, long mid) {
        InlineKeyboardMarkup km = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rs = new ArrayList<>();
        
        int cy = LocalDate.now().getYear();
        LocalDate sd = LocalDate.of(cy, 11, 20); 
        
        if (sd.isBefore(LocalDate.now())) {
            sd = LocalDate.now();
        }

        DateTimeFormatter df = DateTimeFormatter.ofPattern("d MMMM (E)");
        DateTimeFormatter cf = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 0; i < 7; i++) {
            LocalDate d = sd.plusDays(i); 
            
            String dd = d.format(df);
            String cd = d.format(cf);
            
            InlineKeyboardButton b = new InlineKeyboardButton();
            b.setText(dd); 
            b.setCallbackData("book_date_" + cd); 
            rs.add(Collections.singletonList(b));
        }

        InlineKeyboardButton cb = new InlineKeyboardButton();
        cb.setText("Скасувати");
        cb.setCallbackData("book_cancel");
        rs.add(Collections.singletonList(cb));

        km.setKeyboard(rs);

        EditMessageText m = new EditMessageText();
        m.setChatId(String.valueOf(cid));
        m.setMessageId((int) mid);
        m.setText("Будь ласка, оберіть дату:");
        m.setReplyMarkup(km);
        exm(m);
    }

    private void aft(long cid, long mid, String d) {
        InlineKeyboardMarkup km = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rs = new ArrayList<>();
        
        List<String> fs = ah.stream()
                .filter(time -> ms.isa(d, time))
                .collect(Collectors.toList());

        if (fs.isEmpty()) {
            em(cid, mid, "На жаль, на " + d + " вже немає вільного часу. \n" +
                    "Будь ласка, почніть заново (/book) та оберіть іншу дату.");
            us.remove(cid);
            bip.remove(cid);
            return;
        }

        for (String t : fs) {
            InlineKeyboardButton b = new InlineKeyboardButton();
            b.setText(t);
            b.setCallbackData("book_time_" + t);
            rs.add(Collections.singletonList(b));
        }
        
        InlineKeyboardButton cb = new InlineKeyboardButton();
        cb.setText("Скасувати");
        cb.setCallbackData("book_cancel");
        rs.add(Collections.singletonList(cb));
        
        km.setKeyboard(rs);

        EditMessageText m = new EditMessageText();
        m.setChatId(String.valueOf(cid));
        m.setMessageId((int) mid);
        m.setText("Оберіть вільний час на " + d + ":");
        m.setReplyMarkup(km);
        exm(m);
    }

    private void afc(long cid, long mid) {
        BC c = bip.get(cid);
        String t = "Будь ласка, підтвердіть ваш запис:\n\n" +
                      "Послуга: " + c.s + "\n" +
                      "Дата: " + c.d + "\n" +
                      "Час: " + c.t;

        InlineKeyboardMarkup km = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rs = new ArrayList<>();
        
        InlineKeyboardButton cb = new InlineKeyboardButton();
        cb.setText("Так, підтвердити");
        cb.setCallbackData("book_confirm");
        rs.add(Collections.singletonList(cb));
        
        InlineKeyboardButton clb = new InlineKeyboardButton();
        clb.setText("Скасувати");
        clb.setCallbackData("book_cancel");
        rs.add(Collections.singletonList(clb));

        km.setKeyboard(rs);
        
        EditMessageText m = new EditMessageText();
        m.setChatId(String.valueOf(cid));
        m.setMessageId((int) mid);
        m.setText(t);
        m.setReplyMarkup(km);
        exm(m);
    }

    private void cbk(long cid, long mid) {
        BC c = bip.get(cid);
        
        if (!ms.isa(c.d, c.t)) {
             em(cid, mid, "Вибачте, цей час (" + c.d + " " + c.t + ") " +
                     "щойно забронювали. Будь ласка, почніть процес запису знову (/book).");
             us.remove(cid);
             bip.remove(cid);
             return;
        }
        
        boolean sc = ms.ca(
                cid, 
                "User_" + cid,
                c.d, 
                c.t, 
                c.s
        );

        if (sc) {
            em(cid, mid, "Чудово! Вас записано на " + c.d + " о " + c.t + ".");
        } else {
            em(cid, mid, "Виникла помилка під час запису. Спробуйте пізніше.");
        }
        
        us.remove(cid);
        bip.remove(cid);
    }

    private void afdc(long cid, long mid, String d, String t) {
        String tx = "Ви впевнені, що хочете скасувати запис на " + d + " о " + t + "?";
        
        InlineKeyboardMarkup km = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rs = new ArrayList<>();

        InlineKeyboardButton cb = new InlineKeyboardButton();
        cb.setText("Так, скасувати");
        cb.setCallbackData("delete_confirm_" + d + "_" + t);
        rs.add(Collections.singletonList(cb));
        
        InlineKeyboardButton clb = new InlineKeyboardButton();
        clb.setText("Ні, я передумав");
        clb.setCallbackData("delete_cancel");
        rs.add(Collections.singletonList(clb));

        km.setKeyboard(rs);
        
        EditMessageText m = new EditMessageText();
        m.setChatId(String.valueOf(cid));
        m.setMessageId((int) mid);
        m.setText(tx);
        m.setReplyMarkup(km);
        exm(m);
    }


    private void sm(long cid, String t) {
        SendMessage m = new SendMessage(String.valueOf(cid), t);
        exm(m);
    }

    private void em(long cid, long mid, String t) {
        EditMessageText m = new EditMessageText();
        m.setChatId(String.valueOf(cid));
        m.setMessageId((int) mid);
        m.setText(t);
        exm(m);
    }

    private void dm(long cid, int mid) {
        org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage m = 
            new org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage(String.valueOf(cid), mid);
        try {
            execute(m);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void exm(SendMessage m) {
        try {
            execute(m);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    
    private void exm(EditMessageText m) {
        try {
            execute(m);
        } catch (TelegramApiException e) {
            if (!e.getMessage().contains("message is not modified")) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getBotUsername() {
        return this.bu;
    }
}