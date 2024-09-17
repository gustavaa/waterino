#include <Arduino_JSON.h>
#include <millisDelay.h>
#include "Firebase_Arduino_WiFiNINA.h"
#include "config.h"
#include <SI7021.h>
#include "WaterinoSettings.h"
#include <Arduino_JSON.h>

#define MILLISECONDS_HOUR 3600000
#define MILLISECONDS_MINUTE 60000
#define WATERING_WAIT_TIME MILLISECONDS_MINUTE * 2
#define REBOOT_DELAY_MS MILLISECONDS_HOUR * 12
#define SETTINGS_FETCH_FREQUENCY MILLISECONDS_MINUTE * 2

millisDelay rebootTimer;
millisDelay measuringTimer;
millisDelay settingsFetchTimer;
millisDelay fixedWateringTimer;

FirebaseData firebaseData;

WaterinoSettings currentConfig = WaterinoSettings();

// Json-data values
float latestTemperature = 0.0;
int latestSoilMoisture = 0;
int latestHumidity = 0;
bool didWater = false;


// Sensors
int waterPump = 6;
SI7021 temperatureSensor;

void setup() {
    rebootTimer.start(REBOOT_DELAY_MS);                  // Start reboot timer
    settingsFetchTimer.start(SETTINGS_FETCH_FREQUENCY);  // Start settings update timer

    Serial.begin(9600);
    Serial1.begin(4800);

    digitalWrite(waterPump, HIGH);
    pinMode(waterPump, OUTPUT);

    while (!temperatureSensor.begin()) {
        Serial.println("HUMIDITY SENSOR NOT FOUND");
        delay(1000);
    }
    delay(1000);  // give some  time to boot up

    Firebase.begin(FIREBASE_URL, FIREBASE_SECRET, WIFI_SSID, WIFI_PASSWORD);
    Firebase.reconnectWiFi(true);
    updateSettings();
    initialReading();
    performMeasuringIteration();
    performWateringIteration();
}

void loop() {
    if (rebootTimer.justFinished()) {
        Serial.println("Reboot timer finished, rebooting.");
        NVIC_SystemReset();  // force watch dog timer reboot
    }

    if (settingsFetchTimer.justFinished()) {
        Serial.println("Settings fetch timer finished, updating settings from firebase.");
        int oldMeasureFreq = currentConfig.getMeasuringFrequency();
        int oldWateringMode = currentConfig.getCurrentWateringMode();
        updateSettings();
        if (oldMeasureFreq != currentConfig.getMeasuringFrequency() ||
            oldWateringMode != currentConfig.getCurrentWateringMode()) {
            Serial.println("New settings received, cancelling measuring loop and restarting");
            measuringTimer.stop();
            fixedWateringTimer.stop();
            performMeasuringIteration();
            performWateringIteration();
        }
        settingsFetchTimer.start(SETTINGS_FETCH_FREQUENCY);
    }

    if (measuringTimer.justFinished()) {
        Serial.println("Update timer finished, updating input and pushing to Firebase.");
        updateSettings();
        performMeasuringIteration();
    }

    if (fixedWateringTimer.justFinished()) {
        Serial.println("Fixed watering timer finished, watering.");
        updateSettings();
        performWateringIteration();
    }
}

void performMeasuringIteration() {
    bool shouldSendDataToFirebase = false;
    if (currentConfig.getIsEnabled()) {
        readSensors();
        shouldSendDataToFirebase = true;
        if (currentConfig.getCurrentWateringMode() == AUTOMATIC) {
            Serial.println("Current watering mode is Automatic. Checking if should water");
            if (latestSoilMoisture <= currentConfig.getSoilMoistureThreshold()) {
                Serial.println("Soil moisture low, watering.");
                waterPlant();
            }
        }
    }
    if (didWater) {
        measuringTimer.start(WATERING_WAIT_TIME);
        Serial.print("Did water plant. Next measuring update in ");
        Serial.println(WATERING_WAIT_TIME);
    } else {
        Serial.print("Did not water plant. Next measuring update ");
        Serial.println(currentConfig.getMeasuringFrequency());
        measuringTimer.start(currentConfig.getMeasuringFrequency());
    }
    if (shouldSendDataToFirebase == true) {
        sendMeasurementsToFirebase();
    }
}

void performWateringIteration() {
    bool shouldSendDataToFirebase = false;
    if (currentConfig.getCurrentWateringMode() == FIXED_FREQ) {
        if (currentConfig.getIsEnabled()) {
            shouldSendDataToFirebase = true;
            waterPlant();
        }
        fixedWateringTimer.start(currentConfig.getWateringFrequency());
    }
    if (shouldSendDataToFirebase == true) {
        sendMeasurementsToFirebase();
    }
}

const byte hum_temp_ec[8] = {0x01, 0x03, 0x00, 0x00, 0x00, 0x03, 0x05, 0xCB};
byte sensorResponse[11] = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
byte sensor_values[11];

void readSensors() {
    measureSoilMoisture();
    measureTemperatureAndHumidifty();
}

void waterPlant() {
    didWater = true;
    digitalWrite(waterPump, LOW);
    delay(currentConfig.getWateringTime());
    digitalWrite(waterPump, HIGH);
}


void initialReading() {
    Serial.println("Discarding initital sensor data");
    Serial1.flush();
    Serial1.write(hum_temp_ec, 8);
    while (Serial1.available() > 0) {
        Serial.println("Data still available, discarding");
        Serial1.read();
    }
}

void measureSoilMoisture() {
    Serial.println("Taking measurements");
    Serial1.flush();
    while (!Serial1.availableForWrite()) {}
    Serial.println("Writing");
    Serial1.write(hum_temp_ec, 8);

    Serial.println("Reading response");
    Serial1.readBytes(sensorResponse, 11);
    Serial.println("Read END");

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

void measureTemperatureAndHumidifty() {
    si7021_env data = temperatureSensor.getHumidityAndTemperature();
    latestHumidity = data.humidityPercent;
}

void resetShouldForce() {
    Firebase.setBool(firebaseData, "/settings/forceNextWatering", false);
    currentConfig.setShouldForceNext(false);
}

void updateSettings() {
    updateEnabled();
    updateMeasuringFrequency();
    updateWateringFrequency();
    updateWateringMode();
    updateMoistureWateringThreshold();
    updateWateringTime();
    updateMaxTemp();
    updateShouldForce();
}

void updateEnabled() {
    bool success = Firebase.getBool(firebaseData, "/settings/enableWatering");
    if (success) {
        if (firebaseData.dataType() == "boolean") {
            currentConfig.setIsEnabled(firebaseData.boolData());
        }
    }
}

void updateMoistureWateringThreshold() {
    bool success = Firebase.getInt(firebaseData, "/settings/wateringThreshold");
    if (success) {
        if (firebaseData.dataType() == "int") {
            currentConfig.setSoilMoistureThreshold(firebaseData.intData());
            Serial.println("Moisture threshold:");
            Serial.println(currentConfig.getSoilMoistureThreshold());
        }
    }
}

void updateMaxTemp() {
    bool success = Firebase.getInt(firebaseData, "/settings/maxWateringTemperature");
    if (success) {
        if (firebaseData.dataType() == "int") {
            currentConfig.setMaxWateringTemperature(firebaseData.intData());
            Serial.println("Max temperature:");
            Serial.println(currentConfig.getMaxWateringTemperature());
        }
    }
}

void updateWateringTime() {
    bool success = Firebase.getInt(firebaseData, "/settings/wateringTimeMillis");
    if (success) {
        if (firebaseData.dataType() == "int") {
            currentConfig.setWateringTime(firebaseData.intData());
            Serial.println("Watering time:");
            Serial.println(currentConfig.getWateringTime());
        }
    }
}

void updateWateringFrequency() {
    bool success = Firebase.getFloat(firebaseData, "/settings/fixedWateringFrequencyHours");
    if (success) {
        currentConfig.setWateringFrequency(round(firebaseData.floatData() * MILLISECONDS_HOUR));
        Serial.println("Watering frequency:");
        Serial.println(currentConfig.getWateringFrequency());
    }
}

void updateMeasuringFrequency() {
    bool success = Firebase.getFloat(firebaseData, "/settings/updateFrequencyHours");
    if (success) {
        currentConfig.setMeasuringFrequency(round(firebaseData.floatData() * MILLISECONDS_HOUR));
        Serial.println("Update frequency:");
        Serial.println(currentConfig.getMeasuringFrequency());
    }
}

void updateShouldForce() {
    bool success = Firebase.getBool(firebaseData, "/settings/forceNextWatering");
    if (success) {
        if (firebaseData.dataType() == "boolean") {
            currentConfig.setShouldForceNext(firebaseData.boolData());
            Serial.println("Force next:");
            Serial.println(currentConfig.getShouldForceNext());
        }
    }
}

void updateWateringMode() {
    bool success = Firebase.getString(firebaseData, "/settings/wateringMode");
    if (success) {
        String wateringMode = firebaseData.stringData();
        if (wateringMode == "AUTOMATIC") {
            currentConfig.setCurrentWateringMode(AUTOMATIC);
            fixedWateringTimer.stop();
        } else {
            currentConfig.setCurrentWateringMode(FIXED_FREQ);
            if (!fixedWateringTimer.isRunning()) {
                fixedWateringTimer.start(currentConfig.getWateringFrequency());
            }
        }
        Serial.println("Watering mode:");
        Serial.println(currentConfig.getCurrentWateringMode());
    }
}

void sendMeasurementsToFirebase() {
    Serial.println("Sending data");
    JSONVar sensorData;
    JSONVar timeStamp;
    timeStamp[".sv"] = "timestamp";

    sensorData["time"] = timeStamp;
    sensorData["humidity"] = latestHumidity;
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
    } else {
        int minUpdateTime = min(fixedWateringTimer.remaining(), measuringTimer.remaining());
        sensorData["nextUpdate"] = minUpdateTime;
    }

    String jsonString = JSON.stringify(sensorData);
    Serial.println("Pushing data");
    Serial.println(jsonString);
    if (Firebase.pushJSON(firebaseData, "/wateringdata", jsonString)) {
        // Serial.println(firebaseData.dataPath());
        // Serial.println(firebaseData.pushName());
        Serial.println(firebaseData.dataPath() + "/" + firebaseData.pushName());
    } else {
        //Failed, then print out the error detail
        Serial.println(firebaseData.errorReason());
    }
    delete sensorData;
    delete timeStamp;
    didWater = false;
}
