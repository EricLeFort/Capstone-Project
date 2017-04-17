// Define  ////////////////////////////////////////////////////////////////////////////////

#define X1_STEP_PIN          54 //X1->X
#define X1_DIR_PIN           55
#define X1_ENABLE_PIN        38

#define X2_STEP_PIN          46 //X2->Z
#define X2_DIR_PIN           48
#define X2_ENABLE_PIN        62

#define Y_STEP_PIN           60 //Y->Y
#define Y_DIR_PIN            61
#define Y_ENABLE_PIN         56

#define R_STEP_PIN           36 //R->E1
#define R_DIR_PIN            34
#define R_ENABLE_PIN         30

#define E_STEP_PIN           26 //E->E0
#define E_DIR_PIN            28
#define E_ENABLE_PIN         24

#define XMIN_PIN             16 //x sensor 1
#define XMAX_PIN             17 //x sensor 2
#define YMIN_PIN             23 //y sensor 1
#define YMAX_PIN             25 //y sensor 2
#define RMIN_PIN             27 //r sensor 1
#define RMAX_PIN             29 //r sensor 2

#define USERBUTTON1_PIN      20 //move button
#define USERBUTTON2_PIN      21 //user control button

#define UPPERBOUND_X         13382 //Number of steps for full range of motion in X
#define UPPERBOUND_Y         16921 //Number of steps for full range of motion in Y
#define UPPERBOUND_R         1600 //Number of steps for full rotation in R
#define UPPERBOUND_E         2     //Number of power settings for take shot

#define MICRODELAY           800
#define SLOWSTEP             300 //300 is normal slow, 0 is off 
#define SLOWRATE             2 //2 is normal slow, 1 is off  

#define REQUEST_CODE         55
#define CONFIRM_CODE         200
#define SHOT_CODE            170

// Globals  /////////////////////////////////////////////////////////////////////////////// 

int requestedX; //X coord for shot alignment
int requestedY; //Y coord for shot alignment
int requestedR; //Rotation angle for shot alignment
int requestedE; //Power of shot 

int currentX; //Current X position
int currentY; //Current Y position
int currentR; //Current R position

int xDir; //Direction X should move
int yDir; //Direction Y should move
int rDir; //Direction R should move

bool moveControl = false; //Used in move button funcitonality
bool userControl = false; //Enable or disable user turn
bool sensorsEnabled = false; //Enable or disable sensors

int previousTime = millis(); //Used for button de-bounce
int currentTime;  //Used for button de-bounce

// Setup  /////////////////////////////////////////////////////////////////////////////////

void setup(){
  Serial.begin(9600);
  while(!Serial){
    delay(1);
  }

  //TODO uncomment if we choose to use interrupts
                                       //Prepare button interrupts
  //attachInterrupt(digitalPinToInterrupt(USERBUTTON1_PIN), userButton1, FALLING);
  //attachInterrupt(digitalPinToInterrupt(USERBUTTON2_PIN), userButton2, FALLING);
  
  pinMode(X1_STEP_PIN, OUTPUT);  
  pinMode(X1_DIR_PIN, OUTPUT); 
  pinMode(X1_ENABLE_PIN, OUTPUT); 

  pinMode(X2_STEP_PIN, OUTPUT);  
  pinMode(X2_DIR_PIN, OUTPUT); 
  pinMode(X2_ENABLE_PIN, OUTPUT); 

  pinMode(Y_STEP_PIN, OUTPUT);  
  pinMode(Y_DIR_PIN, OUTPUT); 
  pinMode(Y_ENABLE_PIN, OUTPUT); 

  pinMode(R_STEP_PIN, OUTPUT);  
  pinMode(R_DIR_PIN, OUTPUT); 
  pinMode(R_ENABLE_PIN, OUTPUT); 

  pinMode(E_STEP_PIN, OUTPUT);  
  pinMode(E_DIR_PIN, OUTPUT); 
  pinMode(E_ENABLE_PIN, OUTPUT); 

  pinMode(XMIN_PIN, INPUT);
  pinMode(XMAX_PIN, INPUT);
  pinMode(YMIN_PIN, INPUT);
  pinMode(YMAX_PIN, INPUT);
  pinMode(RMIN_PIN, INPUT);
  pinMode(RMAX_PIN, INPUT);
  pinMode(USERBUTTON1_PIN, INPUT);
  pinMode(USERBUTTON2_PIN, INPUT);

  initializeRoutine();
}//setup()

// Main  //////////////////////////////////////////////////////////////////////////////////

void loop(){
  allowUserTurn();
  
  if(!userControl){
    poseForPicture();
  }
  
  if(!userControl){
    requestPCInstruction();  
  }
  
  if(!userControl){
    takeShot();  
  }
}//loop()

// High-Level Functionality  //////////////////////////////////////////////////////////////

void initializeRoutine(){ //Move end effector such that X,Y,R are in initialized positions (0,0,0) and let user break
  requestedX = 0;
  requestedY = 0;
  requestedR = 0;
  
  currentX = UPPERBOUND_X - 1;
  currentY = UPPERBOUND_Y - 1; 
  currentR = UPPERBOUND_R - 1;
  
  moveXYR();
}//initializeRoutine()

void allowUserTurn(){ //Loop during user turn
  userControl = true;
  
  while(userControl){
    moveXYR();
    delay(10); 
    
    if(digitalRead(USERBUTTON1_PIN) == 0){
      userButton1(); 
    }
    if(digitalRead(USERBUTTON2_PIN) == 0){
      userButton2();  
    }
  }
}//allowUserTurn()

void poseForPicture(){ //Move machine so camera can see unobstructed table
  delay(250);
  
  if(currentX < UPPERBOUND_X / 2){
    requestedX = 0;
  }else{
    requestedX = UPPERBOUND_X - 1;
  }
  
  if(currentY < UPPERBOUND_Y / 2){
    requestedY = 0;
  }else{
    requestedY = UPPERBOUND_Y - 1;
  }
  
  requestedR = 0;
  moveXYR();
}//poseForPicture()

void requestPCInstruction(){ //Loop during PC operation
  double value;
  Serial.println(REQUEST_CODE);

  while(Serial.available() == 0);
  if((int)Serial.parseFloat() != SHOT_CODE){
    Serial.println("Communication fault!");
    return;
  }

  while(Serial.available() == 0);
  value = Serial.parseFloat();
  //TODO convert to requested X here
  
  while(Serial.available() == 0);
  value = Serial.parseFloat();
  //TODO convert to requested Y here
  
  while(Serial.available() == 0);
  value = Serial.parseFloat();
  //TODO convert to requested angle here
  
  while(Serial.available() == 0);
  value = Serial.parseFloat();

  Serial.println(CONFIRM_CODE);
}//requestPCInstruction()

void takeShot(){
  moveXYR();
  /*
  Something something pneumatics
  */
}//takeShot()

// Low-Level Functionality  ///////////////////////////////////////////////////////////////

void moveXYR(){ //Move X Y and R simultaneously
  int speedUp;
  
  if(requestedX-currentX != 0 || requestedY-currentY != 0 || requestedR-currentR != 0){
    speedUp = 0;
      
    if (requestedX > currentX){
      xDir = 1;
    }else{
      xDir = -1;
    }
    digitalWrite(X1_DIR_PIN, xDir > 0);
    digitalWrite(X2_DIR_PIN, xDir < 0);

    if(requestedY > currentY){
      yDir = 1;
    }else{
      yDir = -1;
    }
    digitalWrite(Y_DIR_PIN, yDir > 0);

    if(requestedR > currentR){
      rDir = 1;
    }else{
      rDir = -1;
    }
    digitalWrite(R_DIR_PIN, rDir > 0);

    while(requestedX-currentX != 0 || requestedY-currentY != 0 || requestedR-currentR != 0){    
      if(xDir == -1 && digitalRead(XMIN_PIN) == 0){ //XMIN endstop hit
        currentX = 0;
        xDir = 1;
        digitalWrite(X1_DIR_PIN, HIGH);
        digitalWrite(X2_DIR_PIN, LOW); 
        delay(500); 
      }else if(xDir == 1 && digitalRead(XMAX_PIN) == 0){ //XMAX endstop hit
        currentX = UPPERBOUND_X - 1;
        xDir = -1;
        digitalWrite(X1_DIR_PIN, LOW);
        digitalWrite(X2_DIR_PIN, HIGH);  
        delay(500); 
      }

      if(yDir == -1 && digitalRead(YMIN_PIN) == 0){ //YMIN endstop hit
        currentY = 0;
        yDir = 1;
        digitalWrite(Y_DIR_PIN, HIGH);
      }else if(yDir == 1 && digitalRead(YMAX_PIN) == 0){ //YMAX endstop hit
        currentY = UPPERBOUND_Y - 1;
        yDir = -1;
        digitalWrite(Y_DIR_PIN, LOW);
      }
      
      if(rDir == -1 && digitalRead(RMIN_PIN) == 0 && digitalRead(RMAX_PIN) == 0){ //R reference point hit
          currentR = 0;
          rDir = 1;
          digitalWrite(R_DIR_PIN, HIGH);
      }
      
      if(requestedX-currentX != 0){
        digitalWrite(X1_STEP_PIN, HIGH);
        digitalWrite(X2_STEP_PIN, HIGH);
      }
      if(requestedY-currentY != 0){
        digitalWrite(Y_STEP_PIN, HIGH);
      }
      if(requestedR-currentR != 0){
        digitalWrite(R_STEP_PIN, HIGH);
      }

      if(abs(currentX-requestedX) < SLOWSTEP && currentX-requestedX != 0){
        delayMicroseconds(MICRODELAY*(abs(currentX-requestedX)*((1.0-2.0)/SLOWSTEP)+SLOWRATE));
      }else if(speedUp < SLOWSTEP){
        delayMicroseconds(MICRODELAY*(speedUp*((1.0-2.0)/SLOWSTEP)+SLOWRATE));
      }else{
        delayMicroseconds(MICRODELAY);
      }

      if(requestedX-currentX != 0){
        digitalWrite(X1_STEP_PIN, LOW);
        digitalWrite(X2_STEP_PIN, LOW);
        currentX += xDir;
      }
      if(requestedY-currentY != 0){ 
        digitalWrite(Y_STEP_PIN, LOW);
        currentY += yDir;
      }
      if(requestedR-currentR != 0){
        digitalWrite(R_STEP_PIN, LOW);
        currentR += rDir;
      }
    
      if(abs(currentX-requestedX) < SLOWSTEP && currentX-requestedX != 0){
        delayMicroseconds(MICRODELAY*(abs(currentX-requestedX)*((1.0-2.0)/SLOWSTEP)+SLOWRATE));
      }else if(speedUp < SLOWSTEP){
        delayMicroseconds(MICRODELAY*(speedUp*((1.0-2.0)/SLOWSTEP)+SLOWRATE));
      }else{
        delayMicroseconds(MICRODELAY);
      }

      if(digitalRead(USERBUTTON1_PIN) == 0){
        userButton1();
      }
      if(digitalRead(USERBUTTON2_PIN) == 0){
        userButton2(); 
      }

      if(speedUp < SLOWSTEP){
        speedUp++;
      }
    }
  } 
}//moveXYR()

void userButton1(){ //Tell machine to move out of way during user turn
  if(userControl){
    currentTime = millis();
    
    if(abs(currentTime - previousTime) > 250){
      previousTime = currentTime;
      
      if(requestedX != currentX){
        if(abs(requestedX-currentX) < 300){
          requestedX = currentX + (xDir*abs(requestedX-currentX));
        }else{
          requestedX = currentX + (xDir*300); //Stop Moving
        }
      }else{  
        if(!moveControl && requestedX != 0){ //Move to 0
          requestedX = 0;
          moveControl = true;
        }else if(moveControl && requestedX != UPPERBOUND_X - 1){  //Move to UPPERBOUND_X
          requestedX =  UPPERBOUND_X - 1;
          moveControl = false;  
        }else if(!moveControl && requestedX == 0){ //At 0, move to UPPERBOUND_X instead
          requestedX = UPPERBOUND_X - 1;  
        }else if(moveControl && requestedX == UPPERBOUND_X - 1){ //At UPPERBOUND_X, move to 0 instead
          requestedX =  0;  
        }   
      } 
    }
  } 
}//userButton1()

void userButton2(){ //User turn, machine turn
  currentTime = millis();
  
  if(abs(currentTime - previousTime) > 250){
    previousTime = currentTime;
    userControl = !userControl;
    
    if(abs(requestedX-currentX) < 300){
      requestedX = currentX + (xDir*abs(requestedX-currentX));
    }else{
      requestedX = currentX + (xDir*300);
    }
    
    requestedY = currentY;
    requestedR = currentR;
  } 
}//userButton2()

///////////////////////////////////////////////////////////////////////////////////////////

