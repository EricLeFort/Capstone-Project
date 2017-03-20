const long RESPONSE_WAIT_TIME = 10, CHECK_SUM = 170;
const int CANCEL_PIN = 20, MOVE_PIN = 21, SHOT_PIN = 2, //TODO actual pin value for third button
X1_STEP_PIN = 54, X2_STEP_PIN = 46, Y_STEP_PIN = 60, R_STEP_PIN = 36, P_STEP_PIN = 26,
X1_DIR_PIN = 55, X2_DIR_PIN = 48, Y_DIR_PIN = 61, R_DIR_PIN = 34, P_STEP_PIN = 28,
X1_ENABLE_PIN = 38, X2_ENABLE_PIN = 62, Y_ENABLE_PIN = 56, R_ENABLE_PIN = 30, P_ENABLE_PIN = 24,
X_MIN_PIN = 16, Y_MIN_PIN = 23, R_MIN_PIN = 27,
X_MAX_PIN = 17, Y_MAX_PIN = 25, R_MAX_PIN = 29,
MAX_X_STEPS = 10000, MAX_Y_STEPS = 10000, MAX_R_STEPS = 10000,  //Must be tested empirically
MOVE_CMD = 0, SHOT_CMD = 1,
X_LENGTH, Y_LENGTH;

int currentXSteps, currentYSteps, currentAngleSteps;

void setup(){
  //TODO prepare interrupt from cancel button
  calibrate(true)                             //TODO wait for command first?
  currentXSteps = 0;
  currentYSteps = 0;
  currentAngleSteps = 0;
  
  Serial.begin(9600);                         //Init Serial with 9600bps
  while(!Serial){
    //Wait for connection
  }
}//setup()

/**
 * TODO doc
 */
void loop(){
  float shot[4];
  int command = -1;
  char readInt[2];

  while(command == -1){                       //Monitor button sensors
    if(digitalRead(MOVE_PIN)){
      command = MOVE_CMD;
    }else if(digitalRead(SHOT_PIN)){
      command = SHOT_CMD;
    }
    delay(1);
  }

  if(command == MOVE_CMD){
    move();
  }else if(command == SHOT_CMD){
    receiveShot(shot);
    takeShot(shot);
  }
	//TODO monitor sensors, initialize requests
}//loop()

/**
 * TODO doc
 */
boolean receiveShot(float *shot){
  while(Serial.available() < 20){             //Wait for all bytes to be available (5*4)
    delay(10);
  }

  if(sizeof(shot) < 16){
    Serial.println("Incorrect array size.");
    return false;
  }else if(Serial.parseInt() != CHECK_SUM){
    Serial.println("Communication fault!")
    return false;
  }
  
  shot[0] = Serial.parseFloat();
  shot[1] = Serial.parseFloat();
  shot[2] = Serial.parseFloat();
  shot[3] = Serial.parseFloat();
}//receiveShot()

/**
 * TODO doc
 */
void takeShot(float *shot){
  float dx, dy, dTheta;

  dx = shot[0] - X_LENGTH * currentXSteps / MAX_X_STEPS;
  dy = shot[1] - currentYSteps;
  dTheta = shot[2] - currentAngleSteps;
  
  //TODO move to position
  //TODO shoot
  
  if(currentXSteps > MAX_X_STEPS / 2){    //Recalibrate to closest side
    calibrate(false);
  }else{
    calibrate(true);
  }
}//takeShot

/**
 * TODO doc
 */
void move(){
  if(currentXSteps > MAX_X_STEPS / 2){    //Go to 0
    calibrate(true);
  }else{                                  //Go to max X
    calibrate(false);
  }
}//move()

/**
 * TODO doc
 */
void calibrate(boolean zero){
  //TODO if zero, we go to (0, 0), else go to (maxX, maxY)
  //TODO rotate to 0
}//calibrate

/**
 * TODO doc
 */
void stepXMotor(boolean positive){
	//TODO send signal for one step in direction specified
  currentXSteps++;
}//stepXMotor()

/**
 * TODO doc
 */
void stepYMotor(boolean positive){
	//TODO send signal for one step in direction specified
  currentYSteps++;
}//stepYMotor()

/**
 * TODO doc
 */
void stepRotationalMotor(boolean positive){
	//TODO send signal for one step in direction specified
  currentAngleSteps++;
  if(currentAngleSteps > MAX_R_STEPS){
    currentAngleSteps = 0;
  }
}//stepRotationalMotor()
