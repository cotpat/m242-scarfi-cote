package ch.alptbz.mqtttelegramdemo;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TelegramNotificationBot
extends Thread implements UpdatesListener {

    private List<AktivListener> listenerList = new ArrayList<AktivListener>();
    private final TelegramBot bot;
    private final List<Long> users = Collections.synchronizedList(new ArrayList<Long>());

    public TelegramNotificationBot(String botToken) {
        bot = new TelegramBot(botToken);

        bot.setUpdatesListener(this);
    }

    public void sendAlarmNotificationToAllUsers() {
        for(Long user: users) {
                if(Main.pir) {
                    SendMessage reply = new SendMessage(user, "ACHTUNG: ALARM ALARM ALARM!");
                    bot.execute(reply);
                }
        }
    }

    public void addEventListener(AktivListener listener) {
        listenerList.add(listener);
    }
    public void removeEventListener(AktivListener listener) {
        listenerList.remove(listener);
    }

    private void fireAktivStatusEvent(final AktivStatusEvent aEvent){
        for (final AktivListener listener : this.listenerList){
            listener.aktivGeaendert(aEvent);
        }
    }

    @Override
    public int process(List<Update> updates) {
        for(Update update: updates) {
            if(update.message() == null) continue;
            String message = update.message().text();
            if(message == null) continue;
            if(message.startsWith("/help")) {
                SendMessage reply = new SendMessage(update.message().chat().id(), "Use /subscribe to subscribe to your alarm updates. Use /unsubscribe to leave");
                bot.execute(reply);
            }
            if(message.startsWith("/subscribe")) {
                if(!users.contains(update.message().chat().id())) {
                    users.add(update.message().chat().id());
                    SendMessage reply = new SendMessage(update.message().chat().id(),
                            "Welcome! Use /unsubscribe to stop getting notifications.");
                    bot.execute(reply);
                }else{
                    SendMessage reply = new SendMessage(update.message().chat().id(),
                            "You are already subscribed to the alarm notifications!");
                    bot.execute(reply);
                }
            }
            if(message.startsWith("/unsubscribe")) {
                if(users.contains(update.message().chat().id())) {
                    users.remove(update.message().chat().id());
                    SendMessage reply = new SendMessage(update.message().chat().id(),
                            "Byebye!");
                    bot.execute(reply);
                }else{
                    SendMessage reply = new SendMessage(update.message().chat().id(),
                            "You cannot unsubscribe something you've never subscribed to.");
                    bot.execute(reply);
                }
            }
            if(message.startsWith("/turnoff")) {
                Main.aktiv = false;
                fireAktivStatusEvent(new AktivStatusEvent(this, true, false));
                SendMessage reply = new SendMessage(update.message().chat().id(),
                        "Die Alarmanlage ist deaktiviert");
                bot.execute(reply);
            }
            if(message.startsWith("/turnon")) {
                if (!Main.pir){
                    Main.aktiv = true;
                    fireAktivStatusEvent(new AktivStatusEvent(this, false, true));
                    SendMessage reply = new SendMessage(update.message().chat().id(),
                            "Die Alarmanlage wurde aktiviert");
                    bot.execute(reply);
                }
                else {
                    SendMessage reply = new SendMessage(update.message().chat().id(),
                            "ERROR: Eine Bewegung wurde wahrgenommen");
                    bot.execute(reply);
                }
            }
            if(message.startsWith("/aktivstatus")) {
                if(Main.aktiv) {
                    SendMessage reply = new SendMessage(update.message().chat().id(),
                            "Die Alarmanlage ist an");
                    bot.execute(reply);
                }
                else{
                    SendMessage reply = new SendMessage(update.message().chat().id(),
                            "Die Alarmanlage ist aus");
                    bot.execute(reply);
                }
            }
            if(message.startsWith("/pirstatus")) {
                if(Main.pir) {
                    SendMessage reply = new SendMessage(update.message().chat().id(),
                            "Bewegung");
                    bot.execute(reply);
                }
                else{
                    SendMessage reply = new SendMessage(update.message().chat().id(),
                            "Keine Bewegung");
                    bot.execute(reply);
                }
            }
        }

        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }
}
