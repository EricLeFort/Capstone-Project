const long RESPONSE_WAIT_TIME = 10, CHECK_SUM = 170;
const float X_LENGTH = 1.848, Y_LENGTH = 0.921, pi = 3.1415927;
const int CANCEL_PIN = 20, MOVE_PIN = 21, SHOT_PIN = 2, //TODO actual pin value for third button
X1_STEP_PIN = 54, X2_STEP_PIN = 46, Y_STEP_PIN = 60, ANGLE_STEP_PIN = 36, P_STEP_PIN = 26,
X1_DIR_PIN = 55, X2_DIR_PIN = 48, Y_DIR_PIN = 61, ANGLE_DIR_PIN = 34, P_STEP_PIN = 28,
X1_ENABLE_PIN = 38, X2_ENABLE_PIN = 62, Y_ENABLE_PIN = 56, ANGLE_ENABLE_PIN = 30, P_ENABLE_PIN = 24,
X_MIN_PIN = 16, Y_MIN_PIN = 23, ANGLE_MIN_PIN = 27,
X_MAX_PIN = 17, Y_MAX_PIN = 25, ANGLE_MAX_PIN = 29,
MAX_X_STEPS = 10000, MAX_Y_STEPS = 10000, MAX_ANGLE_STEPS = 10000,  //Must be tested empirically
MOVE_CMD = 0, SHOT_CMD = 1;

int currentXSteps, currentYSteps, currentAngleSteps;

/**
 * Prepares the system for operation. This includes calibrating the machine,
 * preparing the cancel interrupt, andinitializing the serial communication
 * channel.
 */
void setup(){
  //TODO prepare interrupt from cancel button
  calibrate(true)                             //TODO wait for command first?
  currentXSteps = 0;
  currentYSteps = 0;
  currentAngleSteps = 0;
  
  Serial.begin(9600);                         //Init Serial with 9600bps
  while(!Serial){
    ; //Wait for connection
  }
}//setup()

/**
 * Main loop for this program. Every execution will handle either a take a shot
 * or move instruction.
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
 * Utilizes serial communication to receive a shot specification.
 * 
 * @param - *shot - An array of floats of length 4 in which to store the
 * received specification.
 */
boolean receiveShot(float *shot){
  while(Serial.available() < 20){             //Wait for all bytes to be available (5*4)
    delay(10);
    //TODO timeout?
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
 * Performs all operations necessary to actuate the machine to take the
 * specified shot including moving to position, actuating the end-effector,
 * and moving out of the way upon completion.
 * 
 * @param *shot - An array of floats of length 4 which contain the shot specification
 */
void takeShot(float *shot){
  int dx, dy, dTheta, steps;
  boolean positive;

  dx = shot[0]/X_LENGTH*MAX_X_STEPS - currentXSteps;
  dy = shot[1]/Y_LENGTH*MAX_Y_STEPS - currentYSteps;
  dTheta = shot[2]/(2*pi)*MAX_ANGLE_STEPS - currentAngleSteps;

  if(dTheta > MAX_ANGLE_STEPS/2){         //Quicker to go other direction
    dTheta -= MAX_ANGLE_STEPS;
  }else if(dTheta < MAX_ANGLE_STEPS/2){
    dTheta += MAX_ANGLE_STEPS;
  }

  positive = dx > 0;                      //Move in x
  steps = abs(dx);
  for(int i = 0; i < steps; i++){
    stepXMotors(positive);
  }

  positive = dy > 0;                      //Move in y
  steps = abs(dy);
  for(int i = 0; i < steps; i++){
    stepYMotor(positive);
  }

  positive = dTheta > 0;                  //Rotate end-effector
  steps = abs(dTheta);
  for(int i = 0; i < steps; i++){
    stepRotationalMotor(positive);
  }
  
  //TODO shoot
  
  if(currentXSteps > MAX_X_STEPS / 2){    //Recalibrate to closest side
    calibrate(false);
  }else{
    calibrate(true);
  }
}//takeShot

/**
 * Handles the move instruction. Instructs the machine to go to the furthest
 * side from where it currently is.
 */
void move(){
  if(currentXSteps > MAX_X_STEPS / 2){    //Go to 0
    calibrate(true);
  }else{                                  //Go to max X
    calibrate(false);
  }
}//move()

/**
 * Calibrates the system by traversing to the x sensor at either 0 or the
 * maximum x position, the y-sensor at either 0 or the maximum y position,
 * and the end-effector to the rotational 0. The machine will move to the
 * x sensor specified, to the closest y sensor, and to the rotational sensor
 * in the closest expected rotational distance.
 * 
 * @param zero - True means the machine should go to (0, 0),
 * false means the machine should go to (max X, max Y)
 */
void calibrate(boolean zero){
  //TODO if zero, we go to (0, 0), else go to (maxX, maxY)
  //TODO rotate to 0
}//calibrate

/**
 * Performs one step of both of the X-motors in the direction specified
 * and updates the current step count appropriately.
 * 
 * @param positive - True means step in the positive direction, 
 * false means step in the negative direction.
 */
void stepXMotors(boolean positive){
  if(currentXSteps == MAX_X_STEPS){   //Prevent over moving
    return;
  }
  
  if(positive){
    //TODO positive step
    currentXSteps++;
  }else{
    //TODO negative step
    currentXSteps--;
  }
}//stepXMotor()

/**
 * Performs one step of of the Y-motor in the direction specified and
 * updates the current step count appropriately.
 * 
 * @param positive - True means step in the positive direction, 
 * false means step in the negative direction.
 */
void stepYMotor(boolean positive){
  if(currentYSteps == MAX_Y_STEPS){
    return;
  }
  
	if(positive){
    //TODO positive step
    currentYSteps++;
  }else{
    //TODO negative step
    currentYSteps--;
  }
}//stepYMotor()

/**
 * Performs one step of of the rotational motor in the direction specified
 * and updates the current step count appropriately.
 * 
 * @param positive - True means step in the positive direction, 
 * false means step in the negative direction.
 */
void stepRotationalMotor(boolean positive){
  if(positive){
    //TODO positive step
    currentAngleSteps++;
    if(currentAngleSteps > MAX_ANGLE_STEPS){
      currentAngleSteps = 0;
    }
  }else{
    //TODO negative step
    currentAngleSteps--;
    if(currentAngleSteps < 0){
      currentAngleSteps = MAX_ANGLE_STEPS;
    }
  }
}//stepRotationalMotor()
