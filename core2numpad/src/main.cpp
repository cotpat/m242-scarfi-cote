#include <Arduino.h>
#include <M5Core2.h>
#include "MFRC522_I2C.h"
#include "view.h"
#include "networking.h"
#include "sideled.h"


void event_handler_num(struct _lv_obj_t * obj, lv_event_t event);
void event_handler_rfid(struct _lv_obj_t * obj, lv_event_t event);
void event_handler_ok(struct _lv_obj_t * obj, lv_event_t event);
void event_handler_box(struct _lv_obj_t * obj, lv_event_t event);
void init_gui_elements();
void mqtt_callback(char* topic, byte* payload, unsigned int length);
Speaker speaker;
MFRC522 mfrc522(0x28);

unsigned long next_lv_task = 0;
bool activeState;
bool pirStateMQTT;
bool pirStateLocal;

lv_obj_t * led;

void init_gui_elements() {
  int c = 1;
  for(int y = 0; y < 3; y++) {
    for(int x = 0; x < 3; x++) {
      add_button(String(c).c_str(), event_handler_num, 5 + x*80, 5 + y*80, 70, 70);
      c++;
    }
  }
  add_button("OK", event_handler_ok, 245, 5, 70, 70);
  add_button("0", event_handler_num, 245, 165, 70, 70);
  
  led = add_led(260, 100, 30, 30);
}


// ----------------------------------------------------------------------------
// MQTT callback
// ----------------------------------------------------------------------------

void mqtt_callback(char* topic, byte* payload, unsigned int length) {
  char * buf = (char *)malloc((sizeof(char)*(length+1)));
  memcpy(buf, payload, length);
  buf[length] = '\0';
  String payloadS = String(buf);
  payloadS.trim();

  if(String(topic) == "alarmalarm/screenled") {
    Serial.println(payloadS);
    if(payloadS == "on") {
      lv_led_on(led);
    }
    if(payloadS == "off") {
      lv_led_off(led);
    }
  }

  if(String(topic) == "alarmalarm/aktivstatus") {
    payloadS == "1" ? activeState = true : activeState = false;
  }

  if(String(topic) == "alarmalarm/pirAlarmbox") {
    payloadS == "1" ? pirStateMQTT = true : pirStateMQTT = false;
  }

  if(String(topic) == "alarmalarm/pirCodebox") {
    payloadS == "1" ? pirStateLocal = true : pirStateLocal = false;
  }
}


// ----------------------------------------------------------------------------
// UI event handlers
// ----------------------------------------------------------------------------

String buffer = "";

void event_handler_num(struct _lv_obj_t * obj, lv_event_t event) {
  if(event == LV_EVENT_CLICKED) {
    lv_obj_t * child = lv_obj_get_child(obj, NULL);
    String num = String(lv_label_get_text(child));
    num.trim();
    buffer += num;
  }
}

void event_handler_rfid(struct _lb_obj_t * obj, lv_event_t event) {

}

lv_obj_t * mbox;

void event_handler_box(struct _lv_obj_t * obj, lv_event_t event) {
  String textBtn = String(lv_msgbox_get_active_btn_text(obj));
  if(event == LV_EVENT_VALUE_CHANGED) {
    if(textBtn == "Send") {
      mqtt_publish("alarmalarm/passwort", buffer.c_str());
    }
    buffer = "";
    close_message_box(mbox);
  }
}

void event_handler_ok(struct _lv_obj_t * obj, lv_event_t event) {
  if(event == LV_EVENT_CLICKED) {
    Serial.println(buffer);
    mbox = show_message_box(buffer.c_str(), "Send", "Cancel", event_handler_box);
  }
}

// ----------------------------------------------------------------------------
// RFID FUNCTIONALITY
// ----------------------------------------------------------------------------

byte rfid_input[4] = "";
char rfid_output[32] = "";

unsigned long now_RFID = 0;
unsigned long last_RFID = 0;

void read_rfid() {
  if (!mfrc522.PICC_IsNewCardPresent() ||
        !mfrc522.PICC_ReadCardSerial()) {
        return;
  } else {
    now_RFID = millis();

    if(long(now_RFID - last_RFID) > 1000) {
      for(byte i = 0; i < mfrc522.uid.size; i++) {
        rfid_input[i] = mfrc522.uid.uidByte[i];
        Serial.printf("Writing byte %d as %x...\n", i, rfid_input[i]);
      }

      sprintf(rfid_output, "%x%x%x%x", rfid_input[0], rfid_input[1], rfid_input[2], rfid_input[3]);
      Serial.println(rfid_output);

      mqtt_publish("alarmalarm/badge", rfid_output);
      last_RFID = now_RFID;
    }

  }
}

// ----------------------------------------------------------------------------
// PIR FUNCTIONALITY
// ----------------------------------------------------------------------------

unsigned long now_PIR = 0;
unsigned long last_PIR = 0;

void read_PIR() {
  now_PIR = millis();
  if((now_PIR - last_PIR) > 1000) {
    if(digitalRead(36)) {
      mqtt_publish("alarmalarm/pirCodebox", "1");
    } else {
      mqtt_publish("alarmalarm/pirCodebox", "0");
    }
    
    last_PIR = now_PIR;
  }
}

unsigned long now_alarm = 0;
unsigned long target_alarm = 0;

void check_alarm() {
  now_alarm = millis();
  if(activeState) {
    Serial.println("alarm armed");

    if(pirStateLocal || pirStateMQTT) {
      target_alarm = now_alarm + 30000;
    }

    if(long((target_alarm - now_alarm)) > 0) {
      set_sideled_state(1);
    } else {
      set_sideled_state(3);
    }

  } else {
    set_sideled_state(0);
    target_alarm = now_alarm;
  }
}

// ----------------------------------------------------------------------------
// MAIN LOOP
// ----------------------------------------------------------------------------

void loop() {
  if(next_lv_task < millis()) {
    lv_task_handler();
    next_lv_task = millis() + 5;
  }
  mqtt_loop();
  read_rfid();
  read_PIR();
  check_alarm();
}

// ----------------------------------------------------------------------------
// MAIN SETUP
// ----------------------------------------------------------------------------

void setup() {
  init_m5();
  init_display();
  speaker.InitI2SSpeakOrMic(1);
  Serial.begin(115200);
  lv_obj_t * wifiConnectingBox = show_message_box_no_buttons("Connecting to WiFi...");
  lv_task_handler();
  delay(5);
  setup_wifi();
  mqtt_init(mqtt_callback);
  close_message_box(wifiConnectingBox);
  init_gui_elements();
  init_sideled();
  mfrc522.PCD_Init();
  pinMode(36, INPUT);
}