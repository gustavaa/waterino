#include <iostream>

enum WateringMode {
    AUTOMATIC,
    FIXED_FREQ
};

// Define constants
const int MILLISECONDS_MINUTE = 60000;
const int MILLISECONDS_HOUR = 3600000;

class WaterinoSettings {
private:
    WateringMode currentWateringMode;
    bool shouldForceNext;
    bool isEnabled;
    int measuringFrequency;
    int wateringFrequency;
    int wateringTime;  // in milliseconds
    int maxWateringTemperature;
    int soilMoistureThreshold;

public:
    // Constructor with default values
    WaterinoSettings()
            : currentWateringMode(FIXED_FREQ),
              shouldForceNext(false),
              isEnabled(true),
              measuringFrequency(MILLISECONDS_MINUTE * 15),
              wateringFrequency(MILLISECONDS_HOUR * 6),
              wateringTime(3500),  // 350ml
              maxWateringTemperature(35),
              soilMoistureThreshold(1000) {
    }

    WateringMode getCurrentWateringMode() const { return currentWateringMode; }

    void setCurrentWateringMode(WateringMode mode) { currentWateringMode = mode; }

    bool getShouldForceNext() const { return shouldForceNext; }

    void setShouldForceNext(bool force) { shouldForceNext = force; }

    bool getIsEnabled() const { return isEnabled; }

    void setIsEnabled(bool enabled) { isEnabled = enabled; }

    int getMeasuringFrequency() const { return measuringFrequency; }

    void setMeasuringFrequency(int frequency) { measuringFrequency = frequency; }

    int getWateringFrequency() const { return wateringFrequency; }

    void setWateringFrequency(int frequency) { wateringFrequency = frequency; }

    int getWateringTime() const { return wateringTime; }

    void setWateringTime(int time) { wateringTime = time; }

    int getMaxWateringTemperature() const { return maxWateringTemperature; }

    void setMaxWateringTemperature(int temperature) { maxWateringTemperature = temperature; }

    int getSoilMoistureThreshold() const { return soilMoistureThreshold; }

    void setSoilMoistureThreshold(int threshold) { soilMoistureThreshold = threshold; }
};