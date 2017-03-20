#include <string.h>

#define MICRODELAY        800

#define DELIMITER         ","
#define ENDCHAR           ";"

void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
}

void loop() {
  // put your main code here, to run repeatedly:
  input();
}

void input() {
  char readline[32];
  char aChar;
  int val1;
  int val2;
  int val3;
  int val4;
  int count = 0;

  memset(&readline[0], 0, sizeof(readline));
  
  do {
    delay(10);
  } while(Serial.available() <= 0); //Wait for input, will loop forever until something entered into Serial Monitor
   
  do {
    aChar = Serial.read();
    readline[count] = aChar;
    count++;
    delay(10);
  } while(count != sizeof(readline) && Serial.available() > 0);
  
  val1 = atoi(strtok(readline, DELIMITER));
  val2 = atoi(strtok(NULL, DELIMITER));
  val3 = atoi(strtok(NULL, DELIMITER));
  val4 = atoi(strtok(NULL, ENDCHAR));
  
  Serial.println(val1);
  Serial.println(val2);
  Serial.println(val3);
  Serial.println(val4);
  memset(&readline[0], 0, sizeof(readline));
  
}
