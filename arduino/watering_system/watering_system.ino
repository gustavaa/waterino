#include <SI7021.h>
#include "Firebase_Arduino_WiFiNINA.h"
#include <Arduino_JSON.h>
#include "config.h"
#include <I2CSoilMoistureSensor.h>

#define STABILIZATION_TIME 1000 // Let the sensor stabilize before reading
#define MILLISECONDS_HOUR 3600000
#define MILLISECONDS_MINUTE 60000
#define WATERING_WAIT_TIME MILLISECONDS_MINUTE*10


FirebaseData firebaseData;

SI7021 humiditySensor;
int moistureThreshold = 55;
int sensorReference = 800;
int sensorPin = A4;

bool shouldForceNext = false;
int updateFrequency = MILLISECONDS_HOUR;
int wateringTime = 2000;
int maxWateringTemperature = 35;

// Json-data values
float latestTemperature = 0.0;
int latestMoisture = 0;
int latestHumidity = 0;
int latestRaw = 0;
bool didWater = false;

int waterPump = 6;

void setup() {
  Serial.begin(9600);

  Serial.println("Setup");

  digitalWrite(waterPump, HIGH);
  pinMode(waterPump, OUTPUT);


  pinMode(sensorPin, OUTPUT);
  digitalWrite(sensorPin, LOW);

  while (!humiditySensor.begin()) {
    Serial.println("NOT FOUND");
    delay(1000);
  }
  delay(1000); // give some  time to boot up

  Serial.println("Connecting to Wi-Fi and Firebase");
  Firebase.begin(FIREBASE_URL, FIREBASE_SECRET, WIFI_SSID, WIFI_PASSWORD);
  Firebase.reconnectWiFi(true);
}

void loop() {
  Firebase.reconnectWiFi(true);
  Serial.println("Getting Enabled setting from Firebase");
  bool success = Firebase.getBool(firebaseData, "/settings/enableWatering");
  if (success) {
    if (firebaseData.dataType() == "boolean" && firebaseData.boolData()) {
      updateSettings();

      measureTemperature();
      Serial.println("MOISTURE");
      measureMoisture();
      sendMeasurementsToFirebase();

      // Wait 3 minutes after watering, measure moisture again and send data to firebase
      int maxTimes = 3;
      int count = 0;
      while (didWater && count < maxTimes) {
        delay(WATERING_WAIT_TIME);
        measureTemperature();
        measureMoisture();
        sendMeasurementsToFirebase();
        updateFrequency = updateFrequency - WATERING_WAIT_TIME - wateringTime;
        count++;
      }

    } else {
      Serial.println("Not enabled");
    }

  } else {
    Serial.println("Failed to get Bool");
    Serial.println(firebaseData.errorReason());

    measureMoisture();
    measureTemperature();
    sendMeasurementsToFirebase();
  }
  delay(updateFrequency);
}

void updateSettings() {
  updateFreqency();
  updateThreshold();
  updateReferenceValue();
  updateWateringTime();
  updateMaxTemp();
  getShouldForce();
}

void measureMoisture() {
  didWater = false;

  int rawValue = readAverageMoisture();
  int moistureLevelReal = map(rawValue, 150, sensorReference, 0, 100);
  moistureLevelReal = constrain(moistureLevelReal, 0, 100);

  Serial.println("Raw");
  Serial.println(rawValue);
  Serial.println("Real");
  Serial.println(moistureLevelReal);
  
  latestMoisture = moistureLevelReal;
  latestRaw = rawValue;

  if (shouldForceNext || (latestMoisture < moistureThreshold && latestTemperature <= maxWateringTemperature)) {
    resetShouldForce();
    didWater = true;
    water();
  }

  return;
}

int readAverageMoisture() {
  pinMode(sensorPin, INPUT_PULLUP); // Power on the sensor
  analogRead(sensorPin);// Read once to let the ADC capacitor start charging
  delay(STABILIZATION_TIME);

  int moistureTotal = 0;
  int numberOfMeasurements = 7;

  for (int i = 0; i < numberOfMeasurements; i++) {
    int newReading = analogRead(sensorPin);
    moistureTotal += newReading;
    Serial.println(newReading);
    delay(100);
  }
  pinMode(sensorPin, OUTPUT);
  digitalWrite(sensorPin, LOW);
  return round(moistureTotal / numberOfMeasurements);
}

void water() {
  Serial.println("Watering");
  digitalWrite(waterPump, LOW);
  delay(wateringTime);
  digitalWrite(waterPump, HIGH);
}

void measureTemperature() {
  si7021_env data = humiditySensor.getHumidityAndTemperature();
  latestTemperature = data.celsiusHundredths / 100.0;
  latestHumidity = data.humidityPercent;
  return;
}

void updateMaxTemp() {
  bool success = Firebase.getInt(firebaseData, "/settings/maxWateringTemperature");
  if (success) {
    if (firebaseData.dataType() == "int") {
      maxWateringTemperature = firebaseData.intData();
      Serial.println("Max temperature:");
      Serial.println(maxWateringTemperature);
    }
  }
}

void updateWateringTime() {
  bool success = Firebase.getInt(firebaseData, "/settings/wateringTimeMillis");
  if (success) {
    if (firebaseData.dataType() == "int") {
      wateringTime = firebaseData.intData();
      Serial.println("Watering time:");
      Serial.println(wateringTime);
    }
  }
}

void updateFreqency() {
  bool success = Firebase.getFloat(firebaseData, "/settings/updateFrequencyHours");
  if (success) {
    updateFrequency = round(firebaseData.floatData() * MILLISECONDS_HOUR);
    Serial.println("Update frequency:");
    Serial.println(updateFrequency);
  }
}

void updateThreshold() {
  bool success = Firebase.getInt(firebaseData, "/settings/wateringThreshold");
  if (success) {
    if (firebaseData.dataType() == "int") {
      moistureThreshold = firebaseData.intData();
      Serial.println("Moisture threshold:");
      Serial.println(moistureThreshold);
    }
  }
}

void updateReferenceValue() {
  bool success = Firebase.getInt(firebaseData, "/settings/sensorReferenceValue");
  if (success) {
    if (firebaseData.dataType() == "int") {
      sensorReference = firebaseData.intData();
      Serial.println("Reference:");
      Serial.println(sensorReference);
    }
  }
}

void getShouldForce() {
  bool success = Firebase.getBool(firebaseData, "/settings/forceNextWatering");
  if (success) {
    if (firebaseData.dataType() == "boolean") {
      shouldForceNext = firebaseData.boolData();
      Serial.println("Force next:");
      Serial.println(shouldForceNext);
    }
  }
}


void resetShouldForce() {
  Firebase.setBool(firebaseData, "/settings/forceNextWatering", false);
  shouldForceNext = false;
}


void sendMeasurementsToFirebase() {
  Serial.println("Sending data");
  JSONVar sensorData;
  JSONVar timeStamp;
  timeStamp[".sv"] = "timestamp";

  sensorData["time"] = timeStamp;
  sensorData["humidity"] = latestHumidity;
  sensorData["moisture"] = latestMoisture;
  sensorData["moistureRaw"] = latestRaw;
  sensorData["temperature"] = latestTemperature;
  sensorData["wateredPlant"] = didWater;

  if (didWater) {
    sensorData["wateredAmount"] = wateringTime;
    sensorData["nextUpdate"] = WATERING_WAIT_TIME;
  } else {
    sensorData["wateredAmount"] = 0;
    sensorData["nextUpdate"] = updateFrequency;
  }

  String jsonString = JSON.stringify(sensorData);

  if (Firebase.pushJSON(firebaseData, "/wateringdata", jsonString)) {
    Serial.println(firebaseData.dataPath());
    Serial.println(firebaseData.pushName());
    Serial.println(firebaseData.dataPath() + "/" + firebaseData.pushName());
  } else {
    //Failed, then print out the error detail
    Serial.println(firebaseData.errorReason());
  }
}
