// Define  ////////////////////////////////////////////////////////////////////////////////

#define X1_STEP_PIN         54 //X1->X
#define X1_DIR_PIN          55
#define X1_ENABLE_PIN       38
//#define X1_STOP_PIN       37

#define X2_STEP_PIN         60 //X2->Y
#define X2_DIR_PIN          61
#define X2_ENABLE_PIN       56
//#define X2_STOP_PIN       36

#define Y_STEP_PIN          46 //Y->Z
#define Y_DIR_PIN           48
#define Y_ENABLE_PIN        62
//#define Y_STOP_PIN        39

#define R_STEP_PIN          26 //R->E0
#define R_DIR_PIN           28
#define R_ENABLE_PIN        24
//#define R_STOP_PIN        17

#define E_STEP_PIN          36 //E->E1
#define E_DIR_PIN           34
#define E_ENABLE_PIN        30
//#define E_STOP_PIN        20

#define UPPERBOUND_X        10000 //Number of steps for full range of motion in X, need test empirically 
#define UPPERBOUND_Y        10000 //Number of steps for full range of motion in Y, need test empirically 
#define UPPERBOUND_R        10000 //Number of steps for full rotation in R, need test empirically 
#define UPPERBOUND_E        5 //Probably 5?

#define MICRODELAY          800

#define DELIMITER           ','
#define ENDCHAR             ';'

// Global  //////////////////////////////////////////////////////////////////////////////// 

int requestedX = 0; //X coord for shot alignment
int requestedY= 0; //Y coord for shot alignment
int requestedR = 0; //Rotation angle for shot alignment
int requestedE = 0; //Power of shot 
int currentX = 0;
int currentY = 0;
int currentR = 0;
int inputError = 0;
            
// Main  //////////////////////////////////////////////////////////////////////////////////

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

  //InitializeRoutine(); //Move end effector such that X,Y,R are in initialized positions (0,0,0)
  
}

void loop()
{
  WaitForInput(); 
  if(inputError == 0) //Only take action if all inputs are valid
  {   
    MoveX();    
    if (requestedE != 0) //Are we taking a shot or just moving X to take a picture?
    {  
      MoveY(); 
      MoveR();
      TakeShot();  
    }  
  }
  SignalDoneAction();
}

// Methods  ///////////////////////////////////////////////////////////////////////////////

void WaitForInput() //Expect input X,Y,R,E if only want move X then send X,DC,DC,0
{
  
  char readline[32] = {NULL};
  const char delim[2] = ",";
  char *var;
  char c;
  int count = 0;
  int varcount = 0;
  
  do {
    delay(10);
  } while(Serial.available() <= 0); //Wait for input, will loop forever until something entered into Serial Monitor
   
  do {
    readline[count++] = Serial.read();
    delay(10);
  } while(readline[count-1] != ENDCHAR && count != sizeof(readline)); //Fill buffer with line of text entered in Serial Monitor

  readline[count-1] = NULL; //Remove ENDCHAR from buffer

   var = strtok(readline, delim); //Separate buffer into array elements using comma delimiter 
   count = 0;
   while( var != NULL && count < 4) //Iterate through array elements
   {  
      if(count == 0) 
      {
        if (atoi(var) >= 0 && atoi(var) < UPPERBOUND_X) requestedX = atoi(var); //atoi -> char to int
        else inputError += 1; //inputError is four digit binary number represented as integer
      }
      else if (count == 1) 
      {
        if (atoi(var) >= 0 && atoi(var) < UPPERBOUND_Y) requestedY = atoi(var);
        else inputError += 2; 
      }
      else if (count == 2) 
      {
        if (atoi(var) >= 0 && atoi(var) < UPPERBOUND_R) requestedR = atoi(var);
        else inputError += 4;
      }
      else
      {
        if (atoi(var) >= 0 && atoi(var) < UPPERBOUND_E) requestedE = atoi(var);
        else inputError += 8;  
      }
      var = strtok(NULL, delim);
      count++;
   }
}

void MoveX(){

  int dir;
  if (requestedX > currentX) dir = 1;
  else dir = -1;
 
  digitalWrite(X1_DIR_PIN, dir > 0);
  digitalWrite(X2_DIR_PIN, dir > 0);
  
  while(requestedX-currentX != 0)
  {
    digitalWrite(X1_STEP_PIN, HIGH);
    digitalWrite(X2_STEP_PIN, HIGH);
    delayMicroseconds(MICRODELAY);
    digitalWrite(X1_STEP_PIN, LOW);
    digitalWrite(X2_STEP_PIN, LOW);
    delayMicroseconds(MICRODELAY);
    currentX += dir;
  }
  
}

void MoveY(){

  int dir;
  if (requestedY > currentY)  dir = 1;
  else  dir = -1;
  
  digitalWrite(Y_DIR_PIN, dir > 0);
  
  while(requestedY-currentY != 0)
  {
    digitalWrite(Y_STEP_PIN, HIGH);
    delayMicroseconds(MICRODELAY);
    digitalWrite(Y_STEP_PIN, LOW);
    delayMicroseconds(MICRODELAY);
    currentY += dir;
  }
  
}

void MoveR(){

  int dir;
  if (requestedR > currentR)
  {
    if ((requestedR - currentR) <= (UPPERBOUND_R/2))  dir  = 1;
    else dir = -1;
  }
  else
  {
    if ((currentR - requestedR) <= (UPPERBOUND_R/2))  dir  = -1;
    else dir = 1;
  }

  digitalWrite(R_DIR_PIN, dir > 0);
  
  while(requestedR-currentR != 0)
  {
    digitalWrite(R_STEP_PIN, HIGH);
    delayMicroseconds(MICRODELAY);
    digitalWrite(R_STEP_PIN, LOW);
    delayMicroseconds(MICRODELAY);
    currentR += dir;
    currentR = (currentR + UPPERBOUND_R) % UPPERBOUND_R; //To wrap around since 0 == UPPERBOUND_R
  }
  
}

void TakeShot()
{
  if(requestedE != 0) //Redundancy 
  {
  
    int effectorLength = 10000; //Number of steps for full range of motion in E, need test empirically ;
  
    digitalWrite(E_DIR_PIN, HIGH);
  
    for(int i=0;i<effectorLength;i++)
    {
      digitalWrite(E_STEP_PIN, HIGH);
      delayMicroseconds(MICRODELAY*requestedE); //requestedE affects delay between steps -> speed of shot -> power of shot
      digitalWrite(E_STEP_PIN, LOW);
      delayMicroseconds(MICRODELAY*requestedE); //requestedE affects delay between steps -> speed of shot -> power of shot
    }
  
    digitalWrite(E_DIR_PIN, LOW);
  
    for(int i=0;i<effectorLength;i++)
    {
      digitalWrite(E_STEP_PIN, HIGH);
      delayMicroseconds(MICRODELAY);
      digitalWrite(E_STEP_PIN, LOW);
      delayMicroseconds(MICRODELAY);
    }
    
  }
  
}


void SignalDoneAction()
{
  
  Serial.println(inputError); //Inform PC if any inputs are invlaid
  inputError = 0;
  
}

///////////////////////////////////////////////////////////////////////////////////////////


