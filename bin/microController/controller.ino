const long RESPONSE_WAIT_TIME = 10;
const float X_LENGTH = 1.848, Y_LENGTH = 0.921, pi = 3.1415927;
const int MOVE_PIN = 20, SHOT_PIN = 21,
X1_STEP_PIN = 54, X2_STEP_PIN = 46, Y_STEP_PIN = 60, ANGLE_STEP_PIN = 36, P_STEP_PIN = 26,
X1_DIR_PIN = 55, X2_DIR_PIN = 48, Y_DIR_PIN = 61, ANGLE_DIR_PIN = 34, P_DIR_PIN = 28,
X1_ENABLE_PIN = 38, X2_ENABLE_PIN = 62, Y_ENABLE_PIN = 56, ANGLE_ENABLE_PIN = 30, P_ENABLE_PIN = 24,
X_MIN_PIN = 16, Y_MIN_PIN = 23, ANGLE_MIN_PIN = 27,
X_MAX_PIN = 17, Y_MAX_PIN = 25, ANGLE_MAX_PIN = 29,
MAX_X_STEPS = 13382, MAX_Y_STEPS = 16921, MAX_ANGLE_STEPS = 1600,  //Must be tested empirically
STEP_DELAY = 1200, SLOW_STEP = 300, SLOW_RATE = 2,
REQUEST_CODE = 55, CONFIRM_CODE = 200, SHOT_CODE = 170,
SLOW_PROXIMITY = 300, DEBOUNCE = 750;

long lastPressed;
int currentXSteps, currentYSteps, currentAngleSteps;
boolean cancelOp, moveOp, shotOp;

/**
 * Prepares the system for operation. This includes calibrating the machine,
 * preparing the cancel interrupt, andinitializing the serial communication
 * channel.
 */
void setup(){
  lastPressed = millis();
  
  Serial.begin(9600);                 //Init Serial with 9600bps
  while(!Serial){
    delay(1);                         //Wait for connection
  }
                                      //Prepare button interrupts
  attachInterrupt(digitalPinToInterrupt(MOVE_PIN), moveBtnPress, FALLING);
  attachInterrupt(digitalPinToInterrupt(SHOT_PIN), shotBtnPress, FALLING);

  pinMode(X1_STEP_PIN, OUTPUT);       //Declare all output pins
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

  while(!moveOp){
    delay(1);                        //Wait to start system to avoid unexpected movement
  }

  if(!digitalRead(X_MIN_PIN)){
    Serial.println("X MIN");
  }

  calibrate(true);
  currentXSteps = 0;
  currentYSteps = 0;
  currentAngleSteps = 0;
}//setup()

/**
 * Main loop for this program. Every execution will handle either a take a shot
 * or move instruction.
 */
void loop(){
  float shot[4];

  while(!(moveOp || shotOp)){       //Monitor button sensors
    delay(1);                       //Wait for command
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
    Serial.println(REQUEST_CODE);
  }

  Serial.println("Waiting");
  while(Serial.available() == 0);
  if((int)Serial.parseFloat() != SHOT_CODE){
    Serial.println("Communication fault!");
    return false;
  }

  while(Serial.available() == 0);
  shot[0] = Serial.parseFloat();
  
  while(Serial.available() == 0);
  shot[1] = Serial.parseFloat();
  
  while(Serial.available() == 0);
  shot[2] = Serial.parseFloat();
  
  while(Serial.available() == 0);
  shot[3] = Serial.parseFloat();

  Serial.println(CONFIRM_CODE);
  
  return true;
}//receiveShot()

/**
 * Performs all operations necessary to actuate the machine to take the
 * specified shot including moving to position, actuating the end-effector,
 * and moving out of the way upon completion.
 * 
 * @param *shot - An array of floats of length 4 which contain the shot specification
 */
void takeShot(float *shot){
  int targetX, targetY, targetAngle;
  boolean positiveX, positiveY, positiveAngle, finished;

  targetX = shot[0]/X_LENGTH*MAX_X_STEPS;
  targetY = shot[1]/Y_LENGTH*MAX_Y_STEPS;
  targetAngle = shot[2]/(2*pi)*MAX_ANGLE_STEPS;

  positiveX = targetX > currentXSteps;    //Determine directions to take
  positiveY = targetY > currentYSteps;
  if(abs(targetAngle - currentAngleSteps) < MAX_ANGLE_STEPS / 2){
    positiveAngle = true;
  }else{
    positiveAngle = false;
  }

  digitalWrite(X1_DIR_PIN, !positiveX);    //Alter motor directions
  digitalWrite(X2_DIR_PIN, positiveX);
  digitalWrite(Y_DIR_PIN, positiveY);
  digitalWrite(ANGLE_DIR_PIN, positiveAngle);

  while(!finished){
    finished = true;
    if(targetX != currentXSteps){         //Step X motors
      finished = false;
      stepXMotors(positiveX, abs(targetX - currentXSteps));
    }

    if(targetY != currentYSteps){         //Step Y motor
      finished = false;
      stepYMotor(positiveY, abs(targetY - currentYSteps));
    }

    if(targetAngle != currentAngleSteps){ //Step Angle motor
      finished = false;
      stepYMotor(positiveAngle, abs(targetAngle - currentAngleSteps));
    }
    //TODO potential issue if we reach endstop earlier than expected, will skip target and loop infinitely
  }
  
  //TODO shoot pneumatic
  
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
    Serial.println("X calibrating to zero");
    digitalWrite(X1_DIR_PIN, false);            //Set direction
    digitalWrite(X2_DIR_PIN, true);
    while(digitalRead(X_MIN_PIN) && !cancelOp){
      stepXMotors(false, currentXSteps);
    }
  }else{
    Serial.println("X calibrating");
    digitalWrite(X1_DIR_PIN, true);             //Set direction
    digitalWrite(X2_DIR_PIN, false);
    while(digitalRead(X_MAX_PIN) && !cancelOp){
      stepXMotors(true, MAX_X_STEPS - currentXSteps);
    }
  }
  
  if(currentYSteps < MAX_Y_STEPS / 2){          //Move to appropriate y side
    Serial.println("Y calibrating to zero");
    digitalWrite(Y_DIR_PIN, false);             //Set direction
    while(digitalRead(Y_MIN_PIN) && !cancelOp){
      stepYMotor(false, currentYSteps);
    }
  }else{
    Serial.println("Y calibrating");
    digitalWrite(Y_DIR_PIN, true);              //Set direction
    while(digitalRead(Y_MAX_PIN) && !cancelOp){
      stepYMotor(true, MAX_Y_STEPS - currentYSteps);
    }
  }

  if(currentAngleSteps < MAX_ANGLE_STEPS / 2){  //Move to 0 angle
    Serial.println("Y calibrating negative");
    digitalWrite(ANGLE_DIR_PIN, false);         //Set direction
    while(digitalRead(ANGLE_MAX_PIN) && !cancelOp){
      stepRotationalMotor(false, currentAngleSteps);
    }
  }else{
    Serial.println("Angle calibrating positive");
    digitalWrite(ANGLE_DIR_PIN, true);         //Set direction
    while(digitalRead(ANGLE_MAX_PIN) && !cancelOp){
      stepRotationalMotor(true, MAX_ANGLE_STEPS - currentAngleSteps);
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
 * @param stepsRemaining - The number of steps expected until the destination is reached.
 */
void stepXMotors(boolean positive, int stepsRemaining){
  if((positive && !digitalRead(X_MAX_PIN))       //Prevent over moving
    || (!positive && !digitalRead(X_MIN_PIN))
    || cancelOp){
    return;
  }

  //TODO alter delay based on stepsRemaining
  //TODO speed-up procedure as well?
  digitalWrite(X1_STEP_PIN, HIGH);
  digitalWrite(X2_STEP_PIN, HIGH);
  delayMicroseconds(STEP_DELAY);
  digitalWrite(X1_STEP_PIN, LOW);
  digitalWrite(X2_STEP_PIN, LOW);
  
  if(positive){
    currentXSteps++;
  }else{
    currentXSteps--;
  }

  if(!digitalRead(X_MIN_PIN)){                   //Check sensors (can be removed if slow)
    currentXSteps = 0;
  }else if(!digitalRead(X_MAX_PIN)){
    currentXSteps = MAX_X_STEPS;
  }
}//stepXMotor()

/**
 * Performs one step of of the Y-motor in the direction specified and
 * updates the current step count appropriately.
 * 
 * @param positive - True means step in the positive direction, 
 * false means step in the negative direction.
 * @param stepsRemaining - The number of steps expected until the destination is reached.
 */
void stepYMotor(boolean positive, int stepsRemaining){
  if((positive && !digitalRead(Y_MAX_PIN))       //Prevent over moving
    || (!positive && !digitalRead(Y_MIN_PIN))
    || cancelOp){
    return;
  }

  //TODO alter delay based on stepsRemaining
  //TODO speed-up procedure as well?
  digitalWrite(Y_STEP_PIN, HIGH);
  delayMicroseconds(STEP_DELAY);
  digitalWrite(Y_STEP_PIN, LOW);
  
	if(positive){
    currentYSteps++;
  }else{
    currentYSteps--;
  }
  
  if(!digitalRead(Y_MIN_PIN)){                   //Check sensors
    currentYSteps = 0;
  }else if(!digitalRead(Y_MAX_PIN)){
    currentYSteps = MAX_Y_STEPS;
  }
}//stepYMotor()

/**
 * Performs one step of of the rotational motor in the direction specified
 * and updates the current step count appropriately.
 * 
 * @param positive - True means step in the positive direction, 
 * false means step in the negative direction.
 * @param stepsRemaining - The number of steps expected until the destination is reached.
 */
void stepRotationalMotor(boolean positive, int stepsRemaining){
  if(cancelOp){
    return;
  }

  //TODO alter delay based on stepsRemaining
  //TODO speed-up procedure as well?
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

  if(!digitalRead(ANGLE_MAX_PIN)){          //Check sensors
    currentAngleSteps = 0;
  }
}//stepRotationalMotor()

/**
 * This method signals the rest of the system that the move button
 * has been pressed.
 */
void moveBtnPress(){
  if(millis() - lastPressed > DEBOUNCE){
    if(!moveOp){
      moveOp = true;
      lastPressed = millis();
      Serial.println("Move");
    }else{
      moveOp = false;
      cancelOp = true;
      lastPressed = millis();
      Serial.println("Stop");
    }
  }
}//moveBtnPress()

/**
 * This method signals the rest of the system that the take shot button
 * has been pressed.
 */
void shotBtnPress(){
  if(millis() - lastPressed > DEBOUNCE){
    lastPressed = millis();
    shotOp = true;
  }
}//shotBtnPress()
