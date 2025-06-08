#include <millisDelay.h>
#include "secrets.h"
#include "preferences.hpp"
#include "WaterinoSettings.h"
#include <WiFi.h>
#include <Arduino_JSON.h>
extern "C" {
#include "freertos/FreeRTOS.h"
#include "freertos/timers.h"
}
#include <AsyncMqttClient.h>

#define MILLISECONDS_HOUR 3600000
#define MILLISECONDS_MINUTE 60000
#define WATERING_WAIT_TIME MILLISECONDS_MINUTE * 2
#define REBOOT_DELAY_MS MILLISECONDS_HOUR * 12
#define SETTINGS_FETCH_FREQUENCY MILLISECONDS_MINUTE * 2

#define MQTT_HOST "homeassistant.local"
#define MQTT_PORT 1883
#define MQTT_CLIENT_PREFIX "waterino-"
#define MQTT_PUB_TOPIC "waterino/state"
#define MQTT_SETTINGS_TOPIC "waterino/setings/state"

#define MQTT_SOIL_MOISTURE_THRESHOLD_TOPIC "waterino/soil_moisture_threshold/set"
#define MQTT_UPDATE_FREQUENCY_TOPIC "waterino/update_frequency_hours/set"
#define MQTT_WATERING_FREQ_TOPIC "waterino/watering_frequency_hours/set"
#define MQTT_WATERING_TIME_TOPIC "waterino/watering_time_seconds/set"
#define MQTT_MAX_TEMPERATURE_TOPIC "waterino/max_temperature/set"
#define MQTT_IS_ENABLED_TOPIC "waterino/is_enabled/set"
#define MQTT_WATERING_MODE_TOPIC "waterino/watering_mode/set"
#define MQTT_WATER_NOW_TOPIC "waterino/water_now"

millisDelay rebootTimer;
millisDelay measuringTimer;
millisDelay settingsFetchTimer;
millisDelay fixedWateringTimer;
millisDelay stopWateringTimer;

WaterinoSettings currentConfig = WaterinoSettings();

AsyncMqttClient mqttClient;
TimerHandle_t mqttReconnectTimer;
TimerHandle_t wifiReconnectTimer;

// Json-data values
float latestTemperature = 0.0;
int latestSoilMoisture = 0;
bool didWater = false;

// Sensors
int waterPump = 12;

void connectToWifi() {
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  Serial.print(F("Connecting to Wi-Fi"));
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(300);
  }
  WiFi.setAutoReconnect(true);
  Serial.println();
}

void connectToMqtt() {
  Serial.println("Connecting to MQTT...");
  mqttClient.connect();
}

void WiFiEvent(WiFiEvent_t event) {
  Serial.printf("[WiFi-event] event: %d\n", event);
  switch (event) {
    case ARDUINO_EVENT_WIFI_STA_CONNECTED:
      delay(1000);
      Serial.println("WiFi connected");
      Serial.println("IP address: ");
      Serial.println(WiFi.localIP());
      connectToMqtt();
      break;
    case ARDUINO_EVENT_WIFI_STA_DISCONNECTED:
      Serial.println("WiFi lost connection");
      xTimerStop(mqttReconnectTimer, 0);  // ensure we don't reconnect to MQTT while reconnecting to Wi-Fi
      xTimerStart(wifiReconnectTimer, 0);
      break;
  }
}

void saveCurrentConfig() {
  saveIntSetting(KEY_WATERING_MODE, currentConfig.getCurrentWateringMode());
  saveIntSetting(KEY_UPDATE_FREQ, currentConfig.getMeasuringFrequency());
  saveIntSetting(KEY_WATERING_FREQ, currentConfig.getWateringFrequency());
  saveIntSetting(KEY_WATERING_TIME, currentConfig.getWateringTime());
  saveIntSetting(KEY_ENABLED, currentConfig.getIsEnabled());
  saveIntSetting(KEY_MAX_TEMP, currentConfig.getMaxWateringTemperature());
  saveIntSetting(KEY_MOISTURE_THRESHOLD, currentConfig.getSoilMoistureThreshold());
}

void loadConfigFromPreferences() {
  currentConfig.setCurrentWateringMode(static_cast<WateringMode>(loadIntSetting(KEY_WATERING_MODE, currentConfig.getCurrentWateringMode())));
  currentConfig.setMeasuringFrequency(loadIntSetting(KEY_UPDATE_FREQ, currentConfig.getMeasuringFrequency()));
  currentConfig.setWateringFrequency(loadIntSetting(KEY_WATERING_FREQ, currentConfig.getWateringFrequency()));
  currentConfig.setWateringTime(loadIntSetting(KEY_WATERING_TIME, currentConfig.getWateringTime()));
  currentConfig.setIsEnabled(loadIntSetting(KEY_ENABLED, currentConfig.getIsEnabled()));
  currentConfig.setMaxWateringTemperature(loadIntSetting(KEY_MAX_TEMP, currentConfig.getMaxWateringTemperature()));
  currentConfig.setSoilMoistureThreshold(loadIntSetting(KEY_MOISTURE_THRESHOLD, currentConfig.getSoilMoistureThreshold()));
}

void setup() {
  rebootTimer.start(REBOOT_DELAY_MS);                  // Start reboot timer
  settingsFetchTimer.start(SETTINGS_FETCH_FREQUENCY);  // Start settings update timer
  mqttReconnectTimer = xTimerCreate("mqttTimer", pdMS_TO_TICKS(2000), pdFALSE, (void*)0, reinterpret_cast<TimerCallbackFunction_t>(connectToMqtt));
  wifiReconnectTimer = xTimerCreate("wifiTimer", pdMS_TO_TICKS(2000), pdFALSE, (void*)0, reinterpret_cast<TimerCallbackFunction_t>(connectToWifi));

  WiFi.onEvent(WiFiEvent);
  mqttClient.onConnect(onMqttConnect);
  mqttClient.onDisconnect(onMqttDisconnect);
  mqttClient.onSubscribe(onMqttSubscribe);
  mqttClient.onUnsubscribe(onMqttUnsubscribe);
  mqttClient.onMessage(onMqttMessage);
  mqttClient.onPublish(onMqttPublish);
  mqttClient.setServer(MQTT_HOST, MQTT_PORT);
  mqttClient.setCredentials(MQTT_USER, MQTT_PASSWORD);
  pinMode(waterPump, OUTPUT);
  digitalWrite(waterPump, LOW);

  Serial.begin(115200);
  Serial1.begin(4800);

  loadConfigFromPreferences();
  connectToWifi();
}

String getDiscoveryTopic(String uniqueId) {
  String base = "homeassistant/sensor/";
  String config = "/config";
  return base + uniqueId + config;
}

void sendCommandDiscoveryMessages() {
  Serial.println("Sending command discovery messages");
  JSONVar deviceIdentifier;
  String clientId = mqttClient.getClientId();
  deviceIdentifier["identifiers"] = "[" + clientId + "]";

  // Soil moisture watering threshold
  JSONVar thresholdPayload;
  thresholdPayload["name"] = "Soil moisture watering threshold";
  thresholdPayload["unique_id"] = "soil_moisture_watering_threshold";
  thresholdPayload["state_topic"] = MQTT_SETTINGS_TOPIC;
  thresholdPayload["value_template"] = "{{ value_json.threshold }}";
  thresholdPayload["command_topic"] = MQTT_SOIL_MOISTURE_THRESHOLD_TOPIC;
  thresholdPayload["step"] = "1";
  thresholdPayload["min"] = "0";
  thresholdPayload["max"] = "100";
  thresholdPayload["mode"] = "box";
  thresholdPayload["device"] = deviceIdentifier;

  String jsonPayloadString1 = JSON.stringify(thresholdPayload);
  mqttClient.publish("homeassistant/number/soil_moisture_threshold/config", 2, true, jsonPayloadString1.c_str());
  mqttClient.subscribe(MQTT_SOIL_MOISTURE_THRESHOLD_TOPIC, 1);
  delete thresholdPayload;

  // Update frequency (hours)
  JSONVar updateFrequencyPayload;
  updateFrequencyPayload["name"] = "Update frequency (hours)";
  updateFrequencyPayload["unique_id"] = "update_frequency_hours";
  updateFrequencyPayload["state_topic"] = MQTT_SETTINGS_TOPIC;
  updateFrequencyPayload["value_template"] = "{{ value_json.update_freq | float / 3600000 | int }}";
  updateFrequencyPayload["command_template"] = "{{ value | float * 3600000 }}";
  updateFrequencyPayload["command_topic"] = MQTT_UPDATE_FREQUENCY_TOPIC;
  updateFrequencyPayload["step"] = "0.05";
  updateFrequencyPayload["min"] = "0.1";
  updateFrequencyPayload["max"] = "24";
  updateFrequencyPayload["mode"] = "box";
  updateFrequencyPayload["device"] = deviceIdentifier;

  String jsonPayloadString2 = JSON.stringify(updateFrequencyPayload);
  mqttClient.publish("homeassistant/number/update_frequency_hours/config", 2, true, jsonPayloadString2.c_str());
  mqttClient.subscribe(MQTT_UPDATE_FREQUENCY_TOPIC, 1);
  delete updateFrequencyPayload;

  // Watering frequency (hours)
  JSONVar wateringFrequencyPayload;
  wateringFrequencyPayload["name"] = "Watering frequency (hours)";
  wateringFrequencyPayload["unique_id"] = "watering_frequency_hours";
  wateringFrequencyPayload["state_topic"] = MQTT_SETTINGS_TOPIC;
  wateringFrequencyPayload["value_template"] = "{{ value_json.watering_freq | float / 3600000 | int }}";
  wateringFrequencyPayload["command_template"] = "{{ value | float * 3600000 | int }}";
  wateringFrequencyPayload["command_topic"] = MQTT_WATERING_FREQ_TOPIC;
  wateringFrequencyPayload["step"] = "0.05";
  wateringFrequencyPayload["min"] = "1";
  wateringFrequencyPayload["max"] = "24";
  wateringFrequencyPayload["mode"] = "box";
  wateringFrequencyPayload["device"] = deviceIdentifier;

  String jsonPayloadString3 = JSON.stringify(wateringFrequencyPayload);
  mqttClient.publish("homeassistant/number/watering_frequency_hours/config", 2, true, jsonPayloadString3.c_str());
  mqttClient.subscribe(MQTT_WATERING_FREQ_TOPIC, 1);
  delete wateringFrequencyPayload;

  // Watering time (seconds)
  JSONVar wateringTimePayload;
  wateringTimePayload["name"] = "Watering time (seconds)";
  wateringTimePayload["unique_id"] = "watering_time_seconds";
  wateringTimePayload["state_topic"] = MQTT_SETTINGS_TOPIC;
  wateringTimePayload["value_template"] = "{{ value_json.watering_time | float / 1000 }}";
  wateringTimePayload["command_topic"] = MQTT_WATERING_TIME_TOPIC;
  wateringTimePayload["command_template"] = "{{ value | float * 1000 }}";
  wateringTimePayload["min"] = "0.1";
  wateringTimePayload["max"] = "15";
  wateringTimePayload["step"] = "0.1";
  wateringTimePayload["mode"] = "box";
  wateringTimePayload["device"] = deviceIdentifier;

  String jsonPayloadString4 = JSON.stringify(wateringTimePayload);
  mqttClient.publish("homeassistant/number/watering_time_seconds/config", 2, true, jsonPayloadString4.c_str());
  mqttClient.subscribe(MQTT_WATERING_TIME_TOPIC, 1);
  delete wateringFrequencyPayload;

  // Is enabled
  JSONVar isEnabledPayload;
  isEnabledPayload["name"] = "Enable";
  isEnabledPayload["unique_id"] = "is_enabled";
  isEnabledPayload["state_topic"] = MQTT_SETTINGS_TOPIC;
  isEnabledPayload["value_template"] = "{{ 'ON' if value_json.is_enabled else 'OFF' }}";
  isEnabledPayload["command_topic"] = MQTT_IS_ENABLED_TOPIC;
  isEnabledPayload["optimistic"] = "false";
  isEnabledPayload["device"] = deviceIdentifier;

  String jsonPayloadString5 = JSON.stringify(isEnabledPayload);
  mqttClient.publish("homeassistant/switch/is_enabled/config", 2, true, jsonPayloadString5.c_str());
  mqttClient.subscribe(MQTT_IS_ENABLED_TOPIC, 1);
  delete isEnabledPayload;

  // Max temperature (number, temperature)
  JSONVar maxTemperaturePayload;
  maxTemperaturePayload["name"] = "Max temperature";
  maxTemperaturePayload["unique_id"] = "max_temperature";
  maxTemperaturePayload["state_topic"] = MQTT_SETTINGS_TOPIC;
  maxTemperaturePayload["value_template"] = "{{ value_json.max_temperature }}";
  maxTemperaturePayload["command_topic"] = MQTT_MAX_TEMPERATURE_TOPIC;
  maxTemperaturePayload["min"] = "10";
  maxTemperaturePayload["max"] = "100";
  maxTemperaturePayload["step"] = "1";
  maxTemperaturePayload["mode"] = "box";
  maxTemperaturePayload["unit_of_measurement"] = "°C";
  maxTemperaturePayload["device_class"] = "temperature";
  maxTemperaturePayload["device"] = deviceIdentifier;

  String jsonPayloadString6 = JSON.stringify(maxTemperaturePayload);
  mqttClient.publish("homeassistant/number/max_temperature/config", 2, true, jsonPayloadString6.c_str());
  mqttClient.subscribe(MQTT_MAX_TEMPERATURE_TOPIC, 1);
  delete maxTemperaturePayload;

  // Watering mode (select, AUTOMATIC or FIXED_FREQ)
  JSONVar wateringModePayload;
  JSONVar optionsArray = JSON.parse("[\"AUTOMATIC\", \"FIXED_FREQ\"]");

  wateringModePayload["name"] = "Watering mode";
  wateringModePayload["unique_id"] = "watering_mode";
  wateringModePayload["state_topic"] = MQTT_SETTINGS_TOPIC;
  wateringModePayload["value_template"] = "{{ value_json.watering_mode }}";
  wateringModePayload["command_topic"] = MQTT_WATERING_MODE_TOPIC;
  wateringModePayload["options"] = optionsArray;
  wateringModePayload["device"] = deviceIdentifier;

  String jsonPayloadString8 = JSON.stringify(wateringModePayload);
  Serial.println(jsonPayloadString8);
  mqttClient.publish("homeassistant/select/watering_mode/config", 2, true, jsonPayloadString8.c_str());
  mqttClient.subscribe(MQTT_WATERING_MODE_TOPIC, 1);
  delete wateringModePayload;
  delete optionsArray;

  JSONVar waterNowPayload;
  waterNowPayload["name"] = "Water Now";
  waterNowPayload["unique_id"] = "water_now_trigger";
  waterNowPayload["command_topic"] = MQTT_WATER_NOW_TOPIC;
  waterNowPayload["device"] = deviceIdentifier;

  String jsonPayloadStringTrigger = JSON.stringify(waterNowPayload);
  mqttClient.publish("homeassistant/button/water_now/config", 2, true, jsonPayloadStringTrigger.c_str());
  mqttClient.subscribe(MQTT_WATER_NOW_TOPIC, 1);

  publishConfigState();
}

void publishConfigState() {
  Serial.println("Publishing config state: ");
  Serial.println(currentConfig.toJsonString());
  mqttClient.publish(MQTT_SETTINGS_TOPIC, 2, true, currentConfig.toJsonString().c_str());
}

void sendSensorDiscoveryMessage() {
  String clientId = mqttClient.getClientId();
  JSONVar initialPayload;
  JSONVar soilMoisturePayload;
  JSONVar didWaterPayload;
  JSONVar nextUpdatePayload;
  JSONVar lastUpdatePayload;
  JSONVar device;
  JSONVar deviceIdentifier;

  device["identifiers"] = "[" + clientId + "]";
  device["name"] = "Waterino";
  device["manufacturer"] = "Gustav";
  device["model"] = "Waterino 4.0";
  device["serial_number"] = clientId;
  deviceIdentifier["identifiers"] = "[" + clientId + "]";

  initialPayload["device_class"] = "temperature";
  initialPayload["name"] = "Soil temperature";
  initialPayload["state_topic"] = MQTT_PUB_TOPIC;
  initialPayload["state_class"] = "measurement";
  initialPayload["unit_of_measurement"] = "°C";
  initialPayload["value_template"] = "{{ value_json.temperature }}";
  initialPayload["unique_id"] = clientId + ".temp";
  initialPayload["device"] = device;
  String jsonPayloadString1 = JSON.stringify(initialPayload);
  mqttClient.publish(getDiscoveryTopic(clientId + "-temp").c_str(), 2, true, jsonPayloadString1.c_str());

  soilMoisturePayload["device_class"] = "humidity";
  soilMoisturePayload["name"] = "Soil moisture";
  soilMoisturePayload["state_class"] = "measurement";
  soilMoisturePayload["state_topic"] = MQTT_PUB_TOPIC;
  soilMoisturePayload["unit_of_measurement"] = "%";
  soilMoisturePayload["value_template"] = "{{ value_json.moisture }}";
  soilMoisturePayload["unique_id"] = clientId + ".moist";
  soilMoisturePayload["device"] = deviceIdentifier;
  String jsonPayloadString2 = JSON.stringify(soilMoisturePayload);
  mqttClient.publish(getDiscoveryTopic(clientId + "-moist").c_str(), 2, true, jsonPayloadString2.c_str());

  didWaterPayload["device_class"] = "enum";
  didWaterPayload["name"] = "Did water";
  didWaterPayload["state_topic"] = MQTT_PUB_TOPIC;
  didWaterPayload["value_template"] = "{{ 'Yes' if bool(value_json.wateredPlant, false) else 'No' }}";
  didWaterPayload["unique_id"] = clientId + ".did_water";
  didWaterPayload["device"] = deviceIdentifier;
  String jsonPayloadString3 = JSON.stringify(didWaterPayload);
  mqttClient.publish(getDiscoveryTopic(clientId + "-did_water").c_str(), 2, true, jsonPayloadString3.c_str());

  nextUpdatePayload["device_class"] = "timestamp";
  nextUpdatePayload["name"] = "Next update";
  nextUpdatePayload["state_topic"] = MQTT_PUB_TOPIC;
  nextUpdatePayload["value_template"] = "{{ now() + timedelta( milliseconds = value_json.nextUpdate) }}";
  nextUpdatePayload["unique_id"] = clientId + "-next_update";
  nextUpdatePayload["device"] = deviceIdentifier;
  String jsonPayloadString4 = JSON.stringify(nextUpdatePayload);
  mqttClient.publish(getDiscoveryTopic(clientId + "-next_update").c_str(), 2, true, jsonPayloadString4.c_str());

  lastUpdatePayload["device_class"] = "timestamp";
  lastUpdatePayload["name"] = "Last update";
  lastUpdatePayload["state_topic"] = MQTT_PUB_TOPIC;
  lastUpdatePayload["value_template"] = "{{ now() }}";
  lastUpdatePayload["unique_id"] = clientId + "-last_update";
  lastUpdatePayload["device"] = deviceIdentifier;
  String jsonPayloadString5 = JSON.stringify(lastUpdatePayload);
  mqttClient.publish(getDiscoveryTopic(clientId + "-last_update").c_str(), 2, true, jsonPayloadString5.c_str());

  delete initialPayload;
  delete soilMoisturePayload;
  delete didWaterPayload;
  delete nextUpdatePayload;
  delete lastUpdatePayload;
  delete device;
  delete deviceIdentifier;
}

void onMqttConnect(bool sessionPresent) {
  Serial.println("Connected to MQTT.");
  Serial.print("Session present: ");
  Serial.println(sessionPresent);
  sendSensorDiscoveryMessage();
  sendCommandDiscoveryMessages();
  initialReading();
  performMeasuringIteration();
  performWateringIteration();
}

void onMqttDisconnect(AsyncMqttClientDisconnectReason reason) {
  Serial.println("Disconnected from MQTT.");

  if (WiFi.isConnected()) {
    xTimerStart(mqttReconnectTimer, 0);
  }
}

void onMqttSubscribe(uint16_t packetId, uint8_t qos) {
  Serial.println("Subscribe acknowledged.");
  Serial.print("  packetId: ");
  Serial.println(packetId);
  Serial.print("  qos: ");
  Serial.println(qos);
}

void onMqttUnsubscribe(uint16_t packetId) {
  Serial.println("Unsubscribe acknowledged.");
  Serial.print("  packetId: ");
  Serial.println(packetId);
}

void onMqttMessage(char* topic, char* payload, AsyncMqttClientMessageProperties properties, size_t len, size_t index, size_t total) {
  Serial.println("Publish received.");
  Serial.print("  topic: ");
  Serial.println(topic);
  Serial.print("  payload: ");
  String payloadStr = "";
  for (size_t i = 0; i < len; ++i) {
    payloadStr += payload[i];
  }
  Serial.println(payloadStr);

  if (String(topic) == MQTT_SOIL_MOISTURE_THRESHOLD_TOPIC) {
    int threshold = payloadStr.toInt();
    currentConfig.setSoilMoistureThreshold(threshold);
    Serial.print("Got new threshold");
    Serial.println(threshold);
  } else if (String(topic) == MQTT_UPDATE_FREQUENCY_TOPIC) {
    float updateFrequency = payloadStr.toInt();
    currentConfig.setMeasuringFrequency(updateFrequency);
    Serial.print("Got new update frequency");
    Serial.println(updateFrequency);
    performMeasuringIteration();
  } else if (String(topic) == MQTT_WATERING_FREQ_TOPIC) {
    float wateringFrequency = payloadStr.toInt();
    currentConfig.setWateringFrequency(wateringFrequency);
    Serial.print("Got new watering frequency");
    Serial.println(wateringFrequency);
  } else if (String(topic) == MQTT_WATERING_TIME_TOPIC) {
    float wateringTime = payloadStr.toInt();
    currentConfig.setWateringTime(wateringTime);
    Serial.print("Got new watering time");
    Serial.println(wateringTime);
  } else if (String(topic) == MQTT_MAX_TEMPERATURE_TOPIC) {
    int maxTemperature = payloadStr.toInt();
    currentConfig.setMaxWateringTemperature(maxTemperature);
    Serial.print("Got new max temperature");
    Serial.println(maxTemperature);
  } else if (String(topic) == MQTT_IS_ENABLED_TOPIC) {
    bool enabled = payloadStr.equalsIgnoreCase("on");
    currentConfig.setIsEnabled(enabled);
    Serial.print("Got updated enabled state: ");
    Serial.println(enabled);
  } else if (String(topic) == MQTT_WATERING_MODE_TOPIC) {
    if (payloadStr == "AUTOMATIC") {
      currentConfig.setCurrentWateringMode(AUTOMATIC);
    } else {
      currentConfig.setCurrentWateringMode(FIXED_FREQ);
    }
    Serial.print("Got updated watering mode: ");
    Serial.println(payloadStr);
  }
  if (String(topic) == MQTT_WATER_NOW_TOPIC) {
    waterPlant();
  } else {
    publishConfigState();
    saveCurrentConfig();
  }
}

void onMqttPublish(uint16_t packetId) {
  Serial.println("Publish acknowledged.");
  Serial.print("  packetId: ");
  Serial.println(packetId);
}


void loop() {
  if (rebootTimer.justFinished()) {
    Serial.println(F("Reboot timer finished, rebooting."));
    ESP.restart();
  }

  if (stopWateringTimer.justFinished()) {
      digitalWrite(waterPump, LOW);
      readSensors();
      publishStateToHomeAssistant();
  }

  if (measuringTimer.justFinished()) {
    Serial.println(F("Update timer finished, updating input and pushing to Firebase."));
    performMeasuringIteration();
  }

  if (fixedWateringTimer.justFinished()) {
    Serial.println(F("Fixed watering timer finished, watering."));
    performWateringIteration();
  }
}

void performMeasuringIteration() {
  bool shouldSendData = false;
  if (currentConfig.getIsEnabled()) {
    readSensors();
    shouldSendData = true;
    if (currentConfig.getCurrentWateringMode() == AUTOMATIC) {
      Serial.println(F("Current watering mode is Automatic. Checking if should water"));
      if (latestSoilMoisture <= currentConfig.getSoilMoistureThreshold()) {
        Serial.println(F("Soil moisture low, watering."));
        waterPlant();
      }
    }
  }
  if (didWater) {
    measuringTimer.start(WATERING_WAIT_TIME);
    Serial.print(F("Did water plant. Next measuring update in "));
    Serial.println(WATERING_WAIT_TIME);
  } else {
    Serial.print(F("Did not water plant. Next measuring update "));
    Serial.println(currentConfig.getMeasuringFrequency());
    measuringTimer.start(currentConfig.getMeasuringFrequency());
    if (shouldSendData == true) {
      publishStateToHomeAssistant();
    }
  }
}

void performWateringIteration() {
  bool shouldSendData = false;
  if (currentConfig.getCurrentWateringMode() == FIXED_FREQ) {
    if (currentConfig.getIsEnabled()) {
      shouldSendData = true;
      waterPlant();
    }
    fixedWateringTimer.start(currentConfig.getWateringFrequency());
  }
}

const byte hum_temp_ec[8] = { 0x01, 0x03, 0x00, 0x00, 0x00, 0x03, 0x05, 0xCB };
byte sensorResponse[11] = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };
byte sensor_values[11];

void readSensors() {
  measureSoilMoisture();
}

void waterPlant() {
  didWater = true;
  digitalWrite(waterPump, HIGH);
  stopWateringTimer.start(currentConfig.getWateringTime());
}


void initialReading() {
  Serial.println(F("Discarding initital sensor data"));
  Serial1.flush();
  Serial1.write(hum_temp_ec, 8);
  while (Serial1.available() > 0) {
    Serial.println(F("Data still available, discarding"));
    Serial1.read();
  }
}

void measureSoilMoisture() {
  Serial.println(F("Taking measurements"));
  Serial1.flush();
  while (!Serial1.availableForWrite()) {}
  Serial.println(F("Writing"));
  Serial1.write(hum_temp_ec, 8);

  Serial.println(F("Reading response"));
  Serial1.readBytes(sensorResponse, 11);
  Serial.println(F("Read END"));

  float soil_hum = 0.1 * int(sensorResponse[3] << 8 | sensorResponse[4]);
  float soil_temp = 0.1 * int(sensorResponse[5] << 8 | sensorResponse[6]);

  Serial.print("Humidity: ");
  Serial.print(soil_hum);
  Serial.println(" %");
  Serial.print("Temperature: ");
  Serial.print(soil_temp);
  Serial.println(" °C");

  latestTemperature = soil_temp;
  latestSoilMoisture = soil_hum;
}

void publishStateToHomeAssistant() {
  Serial.println("Sending data");
  JSONVar sensorData;

  sensorData["moisture"] = latestSoilMoisture;
  sensorData["temperature"] = latestTemperature;
  sensorData["wateredPlant"] = didWater;

  if (didWater) {
    sensorData["wateredAmount"] = currentConfig.getWateringTime();
  }

  WateringMode currentWateringMode = currentConfig.getCurrentWateringMode();

  if (currentWateringMode == AUTOMATIC) {
    if (didWater) {
      sensorData["nextUpdate"] = WATERING_WAIT_TIME;
    } else {
      sensorData["nextUpdate"] = currentConfig.getMeasuringFrequency();
    }
  } else if (fixedWateringTimer.isRunning()) {
    int minUpdateTime = min(fixedWateringTimer.remaining(), measuringTimer.remaining());
    sensorData["nextUpdate"] = minUpdateTime;
  } else {
    sensorData["nextUpdate"] = measuringTimer.remaining();
  }

  String jsonString = JSON.stringify(sensorData);
  Serial.println(F("Pushing data"));
  Serial.println(jsonString);
  mqttClient.publish(MQTT_PUB_TOPIC, 1, true, jsonString.c_str());
  delete sensorData;
  didWater = false;
}