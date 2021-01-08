#include <Arduino.h>
#include <ArduinoJson.h>
#include <ESP8266WiFi.h>
#include <PubSubClient.h>
#include "Timer.h"
#include "Task.h"
#include "MFRC522.h"
#include "SPI.h"

char ssid[50];
char password[50];
const char *mqtt_server = "broker.hivemq.com";
const int mqtt_port = 1883;
const char *assign_topic = "sownAssign";
const char *cmd_topic = "sownCmd";
const char *info_topic = "sownInfo";
const char *rsp_topic = "sownResponse";

const char *getAssign = "{\"requestAssign\" : true}";

#define NFC_RST_PIN D3

#define MSG_BUFFER_SIZE (500)
#define NUMS_PIN 18
//for assign array
#define TYPE 0 //LED : 1, AIR : 2, NFC : 3
#define SWITCH 1
#define MONITOR 2
#define NFC 3

//Switchable CMD
#define SW_OFF LOW
#define SW_ON HIGH
#define SW_CHECK_STATE 2
//Monitor CMD
#define MN_GET_VALUE 1
#define MN_CHANGE_DURATION 2
//NFC
// #define PR_GET_ACCESS_LIST 1
#define PR_ADD_CARD 1
#define PR_ADD_CARD_WITH_CODE 2
#define PR_DELETE_CARD 3

#define ID 1

#define JSON_SIZE 1000

#define NUMS_PERMISSION 10

byte assign[NUMS_PIN][2]; //assign[i][TYPE] - device type assign[i][TYPE] in pin i
                          //assign[i][ID] - device id assign[i][ID] in pin i
Task *tasks[NUMS_PIN];
// unsigned long taskDurations[NUMS_PIN];
MFRC522 nfc[NUMS_PIN];

constexpr uint8_t RST_PIN = D3; // Reset pin for RFID modules

// MFRC522::MIFARE_Key key;
String accessPermission[NUMS_PERMISSION];
byte curPermissionNumber = 0;

StaticJsonDocument<JSON_SIZE> doc;

WiFiClient espClient;
PubSubClient client(espClient);
unsigned long lastMsg = 0;
char msg[MSG_BUFFER_SIZE];
char *json = new char[JSON_SIZE];
String serialRead;

byte durationNotReadNFCCount[NUMS_PIN];
#define RESET_RFID_DURATION 1200
bool isAddingCard = false;

int storemssid = 0;

void unlockDoor()
{
  Serial.println("Door Unlock!");
}

void clearAssign()
{
  for (byte i = 0; i < NUMS_PIN; i++)
  {
    assign[i][TYPE] = 0;
    assign[i][ID] = 0;
  }
}

void clearPermission()
{
  curPermissionNumber = 0;
}

bool deletePermission(String permission)
{
  bool rs = false;
  Serial.print("Deleting ");
  Serial.println(permission);
  for (byte i = 0; i < curPermissionNumber; i++)
  {
    if (accessPermission[i] == permission)
    {
      accessPermission[i] = accessPermission[curPermissionNumber - 1];
      curPermissionNumber -= 1;
      rs = true;
      break;
    }
  }
  return rs;
}

bool checkPermission(String tag)
{
  for (byte i = 0; i < curPermissionNumber; i++)
  {
    if (accessPermission[i] == tag)
      return true;
  }
  return false;
}

bool addPermission(String tag)
{
  if (checkPermission(tag))
    return false;
  accessPermission[curPermissionNumber++] = tag;
}

void publishResponse(int mssid, const char *type, char *mss)
{
  char *mess = new char[500];
  sprintf(mess, "{\"mssid\":%d,\"resp_type\":\"%s\",\"mss\":\"%s\"}", mssid, type, mss);
  client.publish(rsp_topic, mess);
}
void publishError(int mssid, char *errorMss)
{
  publishResponse(mssid, "error", errorMss);
}
void publishSuccess(int mssid, char *successMss)
{
  publishResponse(mssid, "success", successMss);
}
char *string2char(String command)
{
  if (command.length() != 0)
  {
    char *p = const_cast<char *>(command.c_str());
    return p;
  }
}

byte getPin(byte id)
{
  //  if (assign[i] != NULL && strcmp(assign[i], device) == 0)
  //    return i;
  for (int i = 0; i < NUMS_PIN; i++)
  {
    if (assign[i][ID] == id)
      return i;
  }
  return NULL;
}

bool isPinAssigned(byte pin)
{
  return assign[pin][ID] != 0;
}

void deAssignPin(byte pin)
{
  assign[pin][ID] = 0;
  assign[pin][TYPE] = 0;
  if (tasks[pin] != nullptr)
  {
    tasks[pin]->~Task();
    tasks[pin] = nullptr;
  }
}

void deAssignId(int mssid, byte id)
{
  // memcpy(assign[pin], device, sizeof(device));
  byte pin = getPin(id);
  if (pin == NULL)
    publishError(mssid, "This ID is not assigned");
  else
    deAssignPin(pin);
}

void resetRFID(byte pin)
{
  Serial.println("Reset NFC");
  nfc[pin].PCD_Reset();
  nfc[pin].PCD_Init(pin, RST_PIN);
  durationNotReadNFCCount[pin] = 0;
}

void sendMonitorInfo(byte id, int mssid)
{
  byte pin = getPin(id);
  byte type = assign[pin][TYPE];
  unsigned long now = millis();
  if (type == MONITOR)
  {
    float h = analogRead(pin);
    if (isnan(h))
    {
      Serial.println("Failed to read from MQ-135 sensor!");
      if (mssid != NULL)
        publishError(mssid, "Read air monotor failed");
    }
    else
    {
      float gasLevel = h / 1023 * 100;
      // Serial.print("Gas Level: ");
      // Serial.println(gasLevel);
      memset(json, 0, JSON_SIZE);
      sprintf(json, "{\"id\":%d, \"value\": %f, \"duration\":%d, \"time_stamp\":%d}", id, (h / 1023 * 100), tasks[pin]->getDuration(), now);
      client.publish(info_topic, json);
      if (gasLevel > 50)
      {
        Serial.println("Fire Fire");
        Serial.println("SKRTTTTTTTTTT SKRTTTTTTTTTT");
      }
    }
  }
  else if (type == SWITCH)
  {
    int state = digitalRead(pin);
    Serial.print("LIGHT: ");
    Serial.println(state);
    memset(json, 0, JSON_SIZE);
    sprintf(json, "{\"id\":%d, \"value\": %d,\"time_stamp\":%d}", id, state, now);
    client.publish(info_topic, json);
  }
  else if (type == NFC)
  {
    if (nfc[pin].PICC_IsNewCardPresent())
    {
      if (nfc[pin].PICC_ReadCardSerial())
      {
        tasks[pin]->pauseWork();
        String tag;
        for (byte i = 0; i < 4; i++)
        {
          tag += nfc[pin].uid.uidByte[i];
        }
        Serial.println("New Card Arrive");
        Serial.println(tag);
        nfc[pin].PICC_HaltA();
        nfc[pin].PCD_StopCrypto1();
        bool permission = checkPermission(tag);
        if (!isAddingCard)
        {
          char *mss;
          if (permission)
          {
            Serial.println("Permission Granted!");
            unlockDoor();
            mss = "granted";
          }
          else
          {
            Serial.println("Permission Denied!");
            mss = "denied";
          }
          memset(json, 0, JSON_SIZE);
          sprintf(json, "{\"id\":%d, \"value\": \"%d\",\"permission\":\"%s\", \"time_stamp\":%d}", id, tag.toInt(), mss, now);
          client.publish(info_topic, json);
        }
        else
        {
          memset(json, 0, JSON_SIZE);
          sprintf(json, "{\"id\":%d, \"value\": \"%d\", \"time_stamp\":%d}", id, tag.toInt(), now);
          client.publish(info_topic, json);
          if (permission)
            publishError(storemssid, "400");
          else
          {
            accessPermission[curPermissionNumber++] = tag;
            Serial.println("Added Permission");
            publishSuccess(storemssid, string2char(tag));
          }
          isAddingCard = false;
        }
        tasks[pin]->continueWork();
        durationNotReadNFCCount[pin] = 0;
      }
    }
    else
    {
      durationNotReadNFCCount[pin] += 1;
      //28000
      if (durationNotReadNFCCount[pin] >= RESET_RFID_DURATION)
      {
        resetRFID(pin);
      }
    }
  }
}
void sendMonitorInfo(int id)
{
  sendMonitorInfo(id, NULL);
}

void assignPin(byte pin, byte type, byte id)
{
  // memcpy(assign[pin], device, sizeof(device));
  assign[pin][ID] = id;
  assign[pin][TYPE] = type;

  if (type == SWITCH)
    pinMode(pin, OUTPUT);
  else if (type == MONITOR)
  {
    pinMode(pin, INPUT);
    Task *monitorTask = new Task(id, 2000, sendMonitorInfo);
    tasks[pin] = monitorTask;
  }
  else if (type == NFC)
  {
    nfc[pin].PCD_Init(pin, RST_PIN);
    Task *nfcTask = new Task(96, 100, sendMonitorInfo);
    tasks[pin] = nfcTask;
    durationNotReadNFCCount[pin] = 0;
  }
}

void assignPin(byte pin, byte type, byte id, int mssid)
{
  int checkExist = getPin(id);
  if (checkExist != NULL)
  {
    char *err = new char[200];
    sprintf(err, "ID %d is assigned to pin %d", id, checkExist);
    publishError(mssid, err);
  }
  else if (isPinAssigned(pin))
  {
    char *err = new char[200];
    sprintf(err, "Pin %d is assigned to ID %d", pin, assign[pin][ID]);
    publishError(mssid, err);
  }
  else
  {
    assignPin(pin, type, id);
  }
}

void setup_wifi()
{
  delay(10);
  // We start by connecting to a WiFi network
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);

  WiFi.mode(WIFI_STA);
  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED)
  {
    delay(500);
    Serial.print(".");
  }

  randomSeed(micros());

  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}

char *makeDeviceListJson()
{
  doc.clear();
  JsonObject root = doc.to<JsonObject>();
  JsonArray devicesArr = root.createNestedArray("deviceList");
  for (byte i = 0; i < NUMS_PIN; i++)
  {
    if (assign[i][ID] != 0)
    {
      JsonObject device = devicesArr.createNestedObject();
      device["id"] = assign[i][ID];
      device["type"] = assign[i][TYPE];
      device["pin"] = i;
    }
  }
  JsonArray permissionsArray = root.createNestedArray("permissions");
  for (byte i = 0; i < curPermissionNumber; i++)
  {
    permissionsArray.add(accessPermission[i]);
  }
  serializeJson(doc, json, JSON_SIZE);
  return json;
}

void callback(char *topic, byte *payload, unsigned int length)
{
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("] ");

  Serial.println();
  deserializeJson(doc, payload);
  if (strcmp(topic, cmd_topic) == 0)
  {
    int id = doc["id"];
    int cmd = doc["cmd"];
    int mssid = doc["mssid"];
    double value = doc["value"];
    //
    Serial.print("ID: ");
    Serial.print(id);
    Serial.println();
    Serial.print("CMD: ");
    Serial.print(cmd);
    Serial.println();

    int pin = getPin(id);
    int type = assign[pin][TYPE];

    if (type == SWITCH)
    {
      if (cmd <= SW_ON)
      {
        Serial.println(isPinAssigned(pin));
        digitalWrite(pin, cmd);
        sendMonitorInfo(assign[pin][ID], mssid);
        publishSuccess(mssid, "Switch Light Success");
      }
      else if (cmd == SW_CHECK_STATE)
      {
        sendMonitorInfo(assign[pin][ID], mssid);
        publishSuccess(mssid, "Get Light Info Success");
      }
      else
      {
        publishError(mssid, "COMMAND NOT EXIST");
      }
    }
    else if (type == MONITOR)
    {
      if (cmd == MN_GET_VALUE)
      {
        sendMonitorInfo(assign[pin][ID], mssid);
        publishSuccess(mssid, "Get Monitor Info Success");
      }
      else if (cmd == MN_CHANGE_DURATION)
      {
        if (tasks[pin] != nullptr)
        {
          tasks[pin]->changeDuration((unsigned long)value);
        }
        else
        {
          publishError(mssid, "Something went wrong. ReAssign device");
        }
      }
    }
    else if (type == NFC)
    {
      if (cmd == PR_ADD_CARD)
      {
        isAddingCard = true;
        storemssid = mssid;
      }
      else if (cmd == PR_ADD_CARD_WITH_CODE)
      {
        if (addPermission(doc["value"].as<String>()))
        {
          publishSuccess(mssid, "Add permission success");
        }
        else
        {
          publishError(mssid, "Add permission failed");
        }
      }
      else if (cmd == PR_DELETE_CARD)
      {
        if (deletePermission(doc["value"].as<String>()))
        {
          Serial.print("Deleted ");
          Serial.println(doc["value"].as<String>());
          publishSuccess(mssid, "Delete permission success");
        }
        else
        {
          publishError(mssid, "Permission not exist");
        }
      }
      else
      {
        publishError(mssid, "COMMAND NOT EXIST");
      }
    }
    else
    {
      publishError(mssid, "Device not exist");
    }
  }
  else if (strcmp(topic, assign_topic) == 0)
  {
    // JsonObject object = doc.to<JsonObject>();
    if (doc.containsKey("assignList"))
    {
      // clearAssign();
      JsonArray deviceArray = doc["assignList"];
      for (JsonObject device : deviceArray)
      {
        Serial.println("Add to pin");
        assignPin(device["pin"], device["type"], device["id"], device["mssid"]);
      }
      if (doc.containsKey("permissions"))
      {
        Serial.print("New Permission");
        clearPermission();
        JsonArray permissionArray = doc["permissions"];
        for (JsonVariant p : permissionArray)
        {
          Serial.print("Add new permission");
          accessPermission[curPermissionNumber++] = p.as<String>();
        }
      }
    }
    else if (doc.containsKey("deassignList"))
    {
      JsonArray deviceArray = doc["deassignList"];
      for (JsonObject device : deviceArray)
      {
        deAssignId(device["mssid"], device["id"]);
      }
    }
    else if (doc.containsKey("getDevices"))
    {
      client.publish(assign_topic, makeDeviceListJson());
    }
  }
}

void reconnect()
{
  // Loop until we're reconnected
  while (!client.connected())
  {
    Serial.print("Attempting MQTT connection...");
    // Create a random client ID
    String clientId = "ESP8266Client-";
    clientId += String(random(0xffff), HEX);
    // Attempt to connect
    if (client.connect(clientId.c_str()))
    {
      Serial.println("connected");
      client.subscribe(assign_topic);
      client.subscribe(cmd_topic);
      // Publish an announcement...
      client.publish(assign_topic, getAssign);
    }
    else
    {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      // Wait 5 seconds before retrying
      delay(5000);
    }
  }
}

void setup()
{
  Serial.begin(9600);
  pinMode(BUILTIN_LED, OUTPUT); // Initialize the BUILTIN_LED pin as an output

  Serial.print("\nSSID: ");
  while (!Serial.available())
    ;
  serialRead = Serial.readString();
  int length = serialRead.length();
  serialRead.toCharArray(ssid, 50);
  ssid[length - 1] = '\0';
  Serial.print(ssid);
  Serial.print("\nPASSWORD: ");
  while (!Serial.available())
    ;
  serialRead = Serial.readString();
  length = serialRead.length();
  serialRead.toCharArray(password, 50);
  password[length - 1] = '\0';
  // Serial.print(password);

  setup_wifi();
  SPI.begin(); // Init SPI bus
  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(callback);
  // assignPin(A0, MONITOR, 69); //Built in MQ135
  // assignPin(D4, NFC, 96);

  Timer::getInstance()->initialize();
}

void loop()
{
  if (!client.connected())
  {
    reconnect();
  }
  client.loop();

  Timer::getInstance()->update();
  for (int pin = 0; pin < NUMS_PIN; pin++)
  {
    if (tasks[pin] != nullptr)
    {
      tasks[pin]->update();
    }
  }
  Timer::getInstance()->reset();
}