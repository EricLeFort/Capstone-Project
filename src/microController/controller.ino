#include <EEPROM.h>   //TODO needed?

const long RESPONSE_WAIT_TIME = 10, CHECK_SUM = 170;
const float X_LENGTH = 1.848, Y_LENGTH = 0.921, pi = 3.1415927;
const int CANCEL_PIN = 20, MOVE_PIN = 21, SHOT_PIN = 2, //TODO actual pin value for third button
X1_STEP_PIN = 54, X2_STEP_PIN = 46, Y_STEP_PIN = 60, ANGLE_STEP_PIN = 36, P_STEP_PIN = 26,
X1_DIR_PIN = 55, X2_DIR_PIN = 48, Y_DIR_PIN = 61, ANGLE_DIR_PIN = 34, P_STEP_PIN = 28,
X1_ENABLE_PIN = 38, X2_ENABLE_PIN = 62, Y_ENABLE_PIN = 56, ANGLE_ENABLE_PIN = 30, P_ENABLE_PIN = 24,
X_MIN_PIN = 16, Y_MIN_PIN = 23, ANGLE_SENSOR_PIN = 27,
X_MAX_PIN = 17, Y_MAX_PIN = 25,
MAX_X_STEPS = 10000, MAX_Y_STEPS = 10000, MAX_ANGLE_STEPS = 10000,  //Must be tested empirically
STEP_DELAY = 800,
REQUEST_CODE = 55, RECEIPT_CODE = 200;

int currentXSteps, currentYSteps, currentAngleSteps;
boolean cancelOp, moveOp, shotOp;

/**
 * Prepares the system for operation. This includes calibrating the machine,
 * preparing the cancel interrupt, andinitializing the serial communication
 * channel.
 */
void setup(){
  cancelOp = false;
  moveOp = false;
  shotOp = false;
                                              //Prepare button interrupts
  attachInterrupt(digitalPinToInterrupt(CANCEL_PIN), cancelBtnPress, FALLING);
  attachInterrupt(digitalPinToInterrupt(MOVE_PIN), moveBtnPress, FALLING);
  attachInterrupt(digitalPinToInterrupt(SHOT_PIN), shotBtnPress, FALLING);

  pinMode(X1_STEP_PIN, OUTPUT);               //Declare all output pins
  pinMode(X1_DIR_PIN, OUTPUT);
  pinMode(X1_ENABLE_PIN, OUTPUT);
  pinMode(X2_STEP_PIN, OUTPUT);
  pinMode(X2_DIR_PIN, OUTPUT);
  pinMode(X2_ENABLE_PIN, OUTPUT);
  pinMode(Y_STEP_PIN, OUTPUT);
  pinMode(Y_DIR_PIN, OUTPUT);
  pinMode(Y_ENABLE_PIN, OUTPUT);
  pinMode(ANGLE_STEP_PIN, OUTPUT);
  pinMode(ANGLE_DIR_PIN, OUTPUT);
  pinMode(ANGLE_ENABLE_PIN, OUTPUT);
  pinMode(P_STEP_PIN, OUTPUT);
  pinMode(P_DIR_PIN, OUTPUT);
  pinMode(P_ENABLE_PIN, OUTPUT);

  //TODO do we need to set the output of the ENABLE pins?

  while(!moveOp){
    ; //Wait to start system to avoid unexpected movement
  }

  calibrate(true)
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

  while(!(moveOp || shotOp)){                 //Monitor button sensors
    ; //Wait for command
  }

  if(moveOp){
    move();
  }else if(shotOp){
    receiveShot(shot);
    takeShot(shot);
  }

  moveOp = false;
  shotOp = false;
}//loop()

/**
 * Utilizes serial communication to receive a shot specification.
 * 
 * @param - *shot - An array of floats of length 4 in which to store the
 * received specification.
 */
boolean receiveShot(float *shot){
  if(currentXSteps > MAX_X_STEPS / 2){        //Move out of way
    calibrate(false);
  }else{
    calibrate(true);
  }

  if(!cancelOp){                              //Request shot
    Serial.print(REQUEST_CODE, BIN);
  }
  
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
  Serial.flush();
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

  //TODO do all together instead of one at a time?
  positive = dx > 0;                      //Move in x
  steps = abs(dx);
  digitalWrite(X_DIR_PIN, positive);      //Set direction
  for(int i = 0; i < steps && !cancelOp; i++){
    stepXMotors(positive);
    delayMicroseconds(STEP_DELAY);
  }

  positive = dy > 0;                      //Move in y
  steps = abs(dy);
  digitalWrite(Y_DIR_PIN, positive);      //Set direction
  for(int i = 0; i < steps && !cancelOp; i++){
    stepYMotor(positive);
    delayMicroseconds(STEP_DELAY);
  }

  positive = dTheta > 0;                  //Rotate end-effector
  steps = abs(dTheta);
  digitalWrite(ANGLE_DIR_PIN, positive);  //Set direction
  for(int i = 0; i < steps && !cancelOp; i++){
    stepRotationalMotor(positive);
    delayMicroseconds(STEP_DELAY);
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
  if(zero){                                     //Move to appropriate x side
    digitalWrite(X_DIR_PIN, false);             //Set direction
    while(!digitalRead(X_MAX_PIN) && !cancelOp){
      stepXMotors(false);
    }
  }else{
    digitalWrite(X_DIR_PIN, true);              //Set direction
    while(!digitalRead(X_MAX_PIN) && !cancelOp){
      stepXMotors(true);
    }
  }
  
  if(currentYSteps < MAX_Y_STEPS / 2){          //Move to appropriate y side
    digitalWrite(Y_DIR_PIN, false);             //Set direction
    while(!digitalRead(Y_MIN_PIN) && !cancelOp){
      stepYMotors(false);
    }
  }else{
    digitalWrite(Y_DIR_PIN, true);             //Set direction
    while(!digitalRead(Y_MAX_PIN) && !cancelOp){
      stepYMotors(true);
    }
  }

  if(currentAngleSteps < MAX_ANGLE_STEPS / 2){  //Move to 0 angle
    digitalWrite(Angle_DIR_PIN, false);         //Set direction
    while(!digitalRead(ANGLE_SENSOR_PIN) && !cancelOp){
      stepAngleMotors(false);
    }
  }else{
    digitalWrite(Angle_DIR_PIN, true);         //Set direction
    while(!digitalRead(ANGLE_SENSOR_PIN) && !cancelOp){
      stepAngleMotors(true);
    }
  }
  
  cancelOp = false;
}//calibrate

/**
 * Performs one step of both of the X-motors in the direction specified
 * and updates the current step count appropriately.
 * 
 * @param positive - True means step in the positive direction, 
 * false means step in the negative direction.
 */
void stepXMotors(boolean positive){
  if((positive && digitalRead(X_MAX_PIN))       //Prevent over moving
    || (!positive && digitalRead(X_MIN_PIN)
    || cancelOp){
    return;
  }

  digitalWrite(X_STEP_PIN, HIGH);
  delayMicroseconds(STEP_DELAY);
  digitalWrite(X_STEP_PIN, LOW);
  
  if(positive){
    currentXSteps++;
  }else{
    currentXSteps--;
  }

  if(digitalRead(X_MIN_PIN)){                   //Check sensors (can be removed if slow)
    currentXSteps = 0;
  }else if(digitalRead(X_MAX_PIN)){
    currentXSteps = MAX_X_STEPS;
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
  if((positive && digitalRead(Y_MAX_PIN))       //Prevent over moving
    || (!positive && digitalRead(Y_MIN_PIN)
    || cancelOp){
    return;
  }

  digitalWrite(Y_STEP_PIN, HIGH);
  delayMicroseconds(STEP_DELAY);
  digitalWrite(Y_STEP_PIN, LOW);
  
	if(positive){
    currentYSteps++;
  }else{
    currentYSteps--;
  }
  
  if(digitalRead(Y_MIN_PIN)){                   //Check sensors
    currentYSteps = 0;
  }else if(digitalRead(Y_MAX_PIN)){
    currentYSteps = MAX_Y_STEPS;
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
  if(cancelOp){
    return;
  }

  digitalWrite(ANGLE_STEP_PIN, HIGH);
  delayMicroseconds(STEP_DELAY);
  digitalWrite(ANGLE_STEP_PIN, LOW);
  
  if(positive){
    currentAngleSteps++;
    if(currentAngleSteps > MAX_ANGLE_STEPS){
      currentAngleSteps = 0;
    }
  }else{
    currentAngleSteps--;
    if(currentAngleSteps < 0){
      currentAngleSteps = MAX_ANGLE_STEPS;
    }
  }

  if(digitalRead(ANGLE_SENSOR_PIN)){          //Check sensors
    currentAngleSteps = 0;
  }
}//stepRotationalMotor()

/**
 * This method signals the rest of the system that the cancel button
 * has been pressed.
 */
void cancelBtnPress(){
  cancelOp = true;
}//cancelBtnPress()

/**
 * This method signals the rest of the system that the move button
 * has been pressed.
 */
void moveBtnPress(){
  moveOp = true;
}//moveBtnPress()

/**
 * This method signals the rest of the system that the take shot button
 * has been pressed.
 */
void shotBtnPress(){
  shotOp = true;
}//shotBtnPress()
