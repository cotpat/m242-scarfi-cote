package ch.alptbz.mqtttelegramdemo;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;


import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    public static boolean pir;
    public static boolean aktiv = false;
    public static boolean alarm = false;
    private static Logger logger;
    private static Properties config;


    private static boolean loadConfig() {
        config = new Properties();
        try {
            config.load(new FileReader("config.properties"));
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading config file",e);
        }
        return false;
    }

    public final static void main(String[] args) throws InterruptedException {


        ConsoleHandler ch = new ConsoleHandler();
        ch.setLevel(Level.ALL);
        Logger.getGlobal().addHandler(ch);

        logger = Logger.getLogger("main");

        if(!loadConfig()) return;

        logger.info("Config file loaded");

        TelegramNotificationBot tnb = new TelegramNotificationBot(config.getProperty("telegram-apikey"));

        logger.info("TelegramBot started");

        Mqtt mqttClient = new Mqtt(config.getProperty("mqtt-url"), "runner-12");
        initMqtt(mqttClient);


        mqttClient.addHandler(new BiConsumer<String, MqttMessage>() {
            @Override
            public void accept(String s, MqttMessage mqttMessage) {
                if(s.equals("alarmalarm/pirCodebox")) {
                    String status = mqttMessage.toString();
                    if (status.equals("0")){
                        pir = false;
                    }
                    else{
                        pir = true;
                        if (aktiv){tnb.sendAlarmNotificationToAllUsers();}
                    }
                }
                if(s.equals("alarmalarm/pirAlarmbox")) {
                    String status = mqttMessage.toString();
                    if (status.equals("0")){
                        pir = false;
                    }
                    else{
                        pir = true;
                        if (aktiv){tnb.sendAlarmNotificationToAllUsers();}
                    }
                }

                if(s.equals("alarmalarm/passwort")) {
                    String status = mqttMessage.toString();

                    if ((status.equals("1234") || status.equals("2345") || status.equals("3456")) && aktiv) {
                        aktiv = false;
                        try {
                            mqttClient.publish("alarmalarm/passwort", "");
                            status = "";
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                        try {
                            mqttClient.publish("alarmalarm/aktivstatus", "0");
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    }
                    if ((status.equals("1234") || status.equals("2345") || status.equals("3456")) && !aktiv) {

                        aktiv = true;
                        try {
                            mqttClient.publish("alarmalarm/aktivstatus", "1");
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                        try {
                            mqttClient.publish("alarmalarm/passwort", "");
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if(s.equals("alarmalarm/badge")) {
                    String status = mqttMessage.toString();
                    if ((status.equals("f99380d3") || status.equals("1781d860") || status.equals("297347c2")) && aktiv) {
                        aktiv = false;
                        try {
                            mqttClient.publish("alarmalarm/aktivstatus", "0");
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                        try {
                            mqttClient.publish("alarmalarm/badge", "");
                            status = "";
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    }
                    if ((status.equals("f99380d3") || status.equals("1781d860") || status.equals("297347c2")) && !aktiv) {
                        aktiv = true;
                        try {
                            mqttClient.publish("alarmalarm/aktivstatus", "1");
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                        try {
                            mqttClient.publish("alarmalarm/badge", "");
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    }

                }

            }

        });


        AktivListener aktivVerlauf = new AktivListener(){
            @Override
            public void aktivGeaendert(AktivStatusEvent event){
                Boolean alt = event.getStatusalt();
                Boolean neu = event.getStatusneu();
                if (neu != alt){
                    if (neu){
                        try {
                            System.out.println("-");
                            mqttClient.publish("alarmalarm/aktivstatus", "1");
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    }
                    else {
                        try {
                            mqttClient.publish("alarmalarm/aktivstatus", "0");
                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };

        tnb.addEventListener(aktivVerlauf);

    }

    public static void pubMqtt(Mqtt mqttClient, String topic, String message){
        try {
            mqttClient.publish(topic, message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public static void initMqtt(Mqtt mqttClient){
        try {
            mqttClient.start();
            mqttClient.subscribe("alarmalarm/#");
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

}
