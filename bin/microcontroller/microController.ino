// Include  ////////////////////////////////////////////////////////////////////////////////

#include <EEPROM.h>

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

#define USERBUTTON1_PIN      20
#define USERBUTTON2_PIN      21

#define UPPERBOUND_X         10000 //Number of steps for full range of motion in X, need test empirically 
#define UPPERBOUND_Y         10000 //Number of steps for full range of motion in Y, need test empirically 
#define UPPERBOUND_R         10000 //Number of steps for full rotation in R, need test empirically 
#define UPPERBOUND_E         3     //Number of power settings for take shot, probably 3

#define MICRODELAY           800

#define DELIMITER            ","
#define ENDCHAR              ";"

// Global  //////////////////////////////////////////////////////////////////////////////// 

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

bool userControl = false; //Enable or disable user turn

// Setup  /////////////////////////////////////////////////////////////////////////////////

void setup()
{
  
  Serial.begin(9600);
  
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
  attachInterrupt(digitalPinToInterrupt(USERBUTTON1_PIN), UserButton1, FALLING); 
  pinMode(USERBUTTON2_PIN, INPUT);
  attachInterrupt(digitalPinToInterrupt(USERBUTTON2_PIN), UserButton2, FALLING); 

  InitializeRoutine();
  
}

// Main  //////////////////////////////////////////////////////////////////////////////////

void loop()
{
  
  PoseForPicture();
  if(userControl == false) //Only take action if not user turn
  {
    RequestPCInstruction();
  }
  if(userControl == false) 
  {   
    MoveXYR();
  }
  if(userControl == false) 
  {
    TakeShot(); 
  } 
  AllowUserTurn();
  
}

// Methods  ///////////////////////////////////////////////////////////////////////////////

void InitializeRoutine() //Move end effector such that X,Y,R are in initialized positions (0,0,0) and let user break
{

  requestedX = 0;
  requestedY = 0;
  requestedR = 0; 
  currentX = UPPERBOUND_X - 1;
  currentY = UPPERBOUND_Y - 1; 
  currentR = UPPERBOUND_R / 2;
  MoveXYR();
  AllowUserTurn(); 
  
}

void RequestPCInstruction() //Loop during PC operation
{
    
  char readline[32];
  char aChar;
  int val;
  int count = 1500;

  memset(&readline[0], 0, sizeof(readline));
  
  do {
    count++;
    if(count > 1500) //Send request every 15s
    {
      Serial.println("55");
      count = 0;
    }
    delay(10); 
  } while(Serial.available() <= 0); //Will loop forever until something entered into Serial Monitor

  count = 0;
   
  do {
    aChar = Serial.read();
    readline[count] = aChar;
    count++;
    delay(10);
  } while(count != sizeof(readline) && Serial.available() > 0);

  val = atoi(strtok(readline, DELIMITER));
  if (val == 170)
  { 
    val = atoi(strtok(NULL, DELIMITER));
    if (val < 0) requestedX = 0; 
    else if (val >= UPPERBOUND_X) rewuestedX = UPPERBOUND_X - 1;
    else requestedX = val;
    val = atoi(strtok(NULL, DELIMITER));
    if (val < 0) requestedY = 0; 
    else if (val >= UPPERBOUND_Y) rewuestedY = UPPERBOUND_Y - 1;
    else requestedY = val;
    val = atoi(strtok(NULL, DELIMITER));
    if (val < 0) requestedR = 0; 
    else if (val >= UPPERBOUND_R) requestedR = UPPERBOUND_R - 1;
    else requestedR = val;
    val = atoi(strtok(NULL, ENDCHAR));
    if (val < 0) requestedE = 0; 
    else if (val >= UPPERBOUND_E) rewuestedE = UPPERBOUND_E - 1;
    else requestedX = val;
  }
 
  memset(&readline[0], 0, sizeof(readline)); 

  Serial.println("200");
  
}

void MoveXYR() //Move X Y and R simultaneously
{
    
  if (requestedX > currentX) xDir = 1;
  else xDir = -1;

  digitalWrite(X1_DIR_PIN, xDir > 0);
  digitalWrite(X2_DIR_PIN, xDir > 0);

  if (requestedY > currentY)  yDir = 1;
  else  yDir = -1;
  
  digitalWrite(Y_DIR_PIN, yDir > 0);

  if (requestedR > currentR)
  {
    if ((requestedR - currentR) <= (UPPERBOUND_R/2))  rDir  = 1;
    else rDir = -1;
  }
  else
  {
    if ((currentR - requestedR) <= (UPPERBOUND_R/2))  rDir  = -1;
    else rDir = 1;
  }

  digitalWrite(R_DIR_PIN, rDir > 0);

  while(requestedX-currentX != 0 || requestedY-currentY != 0 || requestedR-currentR != 0)
  {
    
    if(xDir == -1 && digitalRead(XMIN_PIN) == 0) //XMIN endstop hit 
    {
      currentX = 0;
      xDir = 1;
      digitalWrite(X1_DIR_PIN, HIGH);
      digitalWrite(X2_DIR_PIN, HIGH);  
    }
    
    if(xDir == 1 && digitalRead(XMAX_PIN) == 0) //XMAX endstop hit 
    {
      currentX = UPPERBOUND_X - 1;
      xDir = -1;
      digitalWrite(X1_DIR_PIN, LOW);
      digitalWrite(X2_DIR_PIN, LOW);  
    }
    
    if(yDir == -1 && digitalRead(YMIN_PIN) == 0) //YMIN endstop hit 
    {
      currentY = 0;
      yDir = 1;
      digitalWrite(Y_DIR_PIN, HIGH);
    }
    
    if(yDir == 1 && digitalRead(YMAX_PIN) == 0) //YMAX endstop hit
    {
      currentY = UPPERBOUND_Y - 1;
      yDir = -1;
      digitalWrite(Y_DIR_PIN, LOW);
    }

    if(digitalRead(RMIN_PIN) == 0 && digitalRead(RMAX_PIN) == 0) //R reference point hit
    {
      currentR = 0;
    }
    
    if(requestedX-currentX != 0)
    {
      digitalWrite(X1_STEP_PIN, HIGH);
      digitalWrite(X2_STEP_PIN, HIGH);
    }

    if(requestedY-currentY != 0)
    {
      digitalWrite(Y_STEP_PIN, HIGH);
    }

    if(requestedR-currentR != 0) 
    {
      digitalWrite(R_STEP_PIN, HIGH);
    }

    delayMicroseconds(MICRODELAY);

    if(requestedX-currentX != 0)
    {
      digitalWrite(X1_STEP_PIN, LOW);
      digitalWrite(X2_STEP_PIN, LOW);
      currentX += xDir;
    }

    if(requestedY-currentY != 0)
    {
      digitalWrite(Y_STEP_PIN, LOW);
      currentY += yDir;
    }

    if(requestedR-currentR != 0) 
    {
      digitalWrite(R_STEP_PIN, LOW);
      currentR += rDir;
      currentR = (currentR + UPPERBOUND_R) % UPPERBOUND_R; //Since 0 == UPPERBOUND_R
    }
    
    delayMicroseconds(MICRODELAY);
    
  }
  
}

void TakeShot()
{
  /*
  Something something pnuematics
  */
}

void AllowUserTurn() //Loop during user turn
{

  userControl = true;
  while(userControl)
  {
    MoveXYR():
    delay(10); 
  }
  
}

void PoseForPicture() //Move machine so camera can see unobstructed table
{
  
  if(currentX < UPPERBOUND_X / 2) requestedX = 0;
  else requestedX = UPPERBOUND_X - 1;
  if(currentY < UPPERBOUND_Y / 2) requestedY = 0;
  else requestedY = UPPERBOUND_Y - 1;
  requestedR = 0;
  MoveXYR();  
  
}

void UserButton1() //Tell machine to move out of the way
{
  
  if (userControl)
  {
    if(requestedX == currentX && requestedX != 0) requestedX = 0; //Move to 0
    else if(requestedX == 0) requestedX =  UPPERBOUND_X - 1; //Move to upperboundX - 1
    else requestedX = currentX; //Stop moving
  }
  
}

void UserButton2() //Give/relinquish user control
{
  
  userControl = !userControl;
  requestedX = currentX;
  requestedY = currentY;
  requestedR = currentR;
  requestedE = currentE;
  
}

///////////////////////////////////////////////////////////////////////////////////////////

