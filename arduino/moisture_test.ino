#define STABILIZATION_TIME 1500 // Let the sensor stabilize before reading


const int moistureSensor = A1; 


char data[10];

void setup() {
  Serial.begin(9600);
  pinMode(moistureSensor,INPUT);
  // put your setup code here, to run once:

}

void loop() {
  measureMoisture();
  delay(50);
  // put your main code here, to run repeatedly:
}


void measureMoisture() {
  int rawValue = readAverageMoisture();
  int moistureLevelReal = map(rawValue, 705, 345, 0, 100);
  moistureLevelReal = constrain(moistureLevelReal, 0, 100);

  Serial.println(rawValue);
  Serial.println(moistureLevelReal);

  // Turn off the sensor to conserve battery and minimize corrosion
}

int readAverageMoisture() {
  int moistureTotal = 0;
  int numberOfMeasurements = 7;

  for (int i = 0; i < numberOfMeasurements; i++) {
    moistureTotal += analogRead(moistureSensor);
    delay(30);
  }

  return round(moistureTotal / numberOfMeasurements);
}
