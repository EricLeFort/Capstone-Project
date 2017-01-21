package pcController;

/*
 * Notes:
 * soft shots last < 3s
 * medium shots last < 6s
 * hard shots last < 9s
 * 
 * If a continuous set of shots result in the same score, it might be wise to choose the shot in the middle of that
 * set. This will minimize the effect of precision errors on the shot.
 */

/*
 * TODO Possible errors:
 * Physical constants might be inaccurate or unusable, certain ones should be measured:
 /* 	- pocket openings,
 * 		- initial speeds/
 * Equations may not be correct, especially:
 * 		- slowdown of balls,
 * 		- friction slowdown from hitting wall/ball at an angle
 * 		- Ball-Ball collisions -- No consideration of angle of contact.
 * 		- Whether a ball is sunk -- might bounce out from angled side pockets
 */

public class SimulationInstance extends TableState{
	private final static double TIME_STEP = 0.01,									//Simulation time step in s
			BUMPER_COEFFICIENT = 0.866, BALL_BALL_COEFFICIENT = 0.96,				//Elastic coefficients
			BALL_TABLE_FRICTION = 0.01,
			GRAVITATIONAL_CONSTANT = 9.807,											//in m/s/s
			INITIAL_LOW_SPEED = 1.2, INITIAL_MED_SPEED = 2.25, INITIAL_HI_SPEED = 3,//Initial speeds in m/s
			MIN_MOTION = 0.015,														//Motion below this is considered stopped
			CORNER_MOUTH_WIDTH = 0.1, SIDE_MOUTH_WIDTH = 0.114,						//Pocket openings
			CORNER_PLAY = CORNER_MOUTH_WIDTH - Ball.RADIUS, SIDE_PLAY = SIDE_MOUTH_WIDTH - Ball.RADIUS,
			SINK_PROXIMITY = 0.01,													//Distance at which a ball is sunk in m
			MAX_SINK_SPEED = 2;														//Speed when balls bounce out of hole in m/s
	private final static int CUE_SCORE = -20,										//Scoring for sinking certain balls
			RIGHT_BALLTYPE_SUNK = 2, WRONG_BALLTYPE_SUNK = -3;
	Ball[] balls;
	private double[][] velocities = new double[16][2];
	private boolean inMotion, shooting8;
	
	/**
	 * Constructs a new <code>SimulationInstance</code> according to the parameters passed in.<br>
	 * @param positions - A 16-by-2 array containing the coordinates of each ball.
	 * @param shotAngle - The angle relative to the cue ball with which to take this shot (from 0 to 2*pi).
	 * @param shotPower - The power with which to take this shot (from 0 to 1).
	 */
	public SimulationInstance(double[][] positions, double shotAngle, double shotPower){
		super(positions);
		
		double radAngle = shotAngle, speed;
		int start, end;
		
		balls = super.deepCopy();
		
		if(InferenceEngine.myBallType == BallType.SOLID){
			start = 1;
			end = 7;
		}else{
			start = 9;
			end = 15;
		}
		
		shooting8 = true;
		for(int i = start; i <= end; i++){			//If all balls sunk, shooting 8
			if(positions[i][0] >= 0){
				shooting8 = false;
				break;
			}
		}
		
		inMotion = true;
		if(shotPower <= 0 || shotPower > 1){
			throw new IllegalArgumentException("Shot power out of range.");
		}else if(shotPower <= 0.4){
			speed = INITIAL_LOW_SPEED;
		}else if(shotPower <= 0.75){
			speed = INITIAL_MED_SPEED;
		}else{
			speed = INITIAL_HI_SPEED;
		}
		
		velocities[0][0] = speed * Math.cos(radAngle);
		velocities[0][1] = speed * Math.sin(radAngle);
	}//Constructor
	
	/**
	 * Updates the state of the simulation by one time step according to the appropriate laws of physics.
	 * @return The score earned or lost in this update.
	 */
	public int update(){
		double distance, nextDistance, fullVelocity;
		int updateScore = 0;
		if(Math.random() < 0.05){
//			System.out.println();
//			System.out.println("(" + balls[0].getXPosition() + ", " + balls[0].getYPosition() + ")");
//			System.out.println("<" + velocities[0][0] + ", " + velocities[0][1] + ">");
//			System.out.println("(" + balls[1].getXPosition() + ", " + balls[1].getYPosition() + ")");
//			System.out.println("<" + velocities[1][0] + ", " + velocities[1][1] + ">");
		}
		
		for(int i = 0; i < velocities.length; i++){		//Move balls appropriately
			balls[i].alterX(velocities[i][0] * TIME_STEP);
			balls[i].alterY(velocities[i][1] * TIME_STEP);
			fullVelocity = Math.sqrt(Math.pow(velocities[i][0], 2) + Math.pow(velocities[i][1], 2));
			
			if(velocities[i][0] > 0){					//Slow down from friction in the x-direction
				velocities[i][0] -= BALL_TABLE_FRICTION*GRAVITATIONAL_CONSTANT*TIME_STEP*fullVelocity;
			}else if(velocities[i][0] < 0){
				velocities[i][0] += BALL_TABLE_FRICTION*GRAVITATIONAL_CONSTANT*TIME_STEP*fullVelocity;
			}
			
			if(velocities[i][1] > 0){					//Slow down from friction in the y-direction
				velocities[i][1] -= BALL_TABLE_FRICTION*GRAVITATIONAL_CONSTANT*TIME_STEP*fullVelocity;
			}else if(velocities[i][1] < 0){
				velocities[i][1]  += BALL_TABLE_FRICTION*GRAVITATIONAL_CONSTANT*TIME_STEP*fullVelocity;
			}
			if(Math.sqrt(Math.pow(velocities[i][0], 2) + Math.pow(velocities[i][1], 2)) < MIN_MOTION){
				velocities[i][0] = velocities[i][1] = 0;
			}
		}
		
		outerloop:
			for(int i = 0; i < balls.length; i++){
				while(velocities[i][0] + velocities[i][1] == 0){		//Ignore balls not in motion
					i++;
					if(i == balls.length){
						break outerloop;
					}
				}
				if(inPocket(balls[i], velocities[i])){					//Ball sunk
					if(shooting8){
						System.out.println("Oh..");
						if(balls[i].getValue() == 0){					//Sunk cue ball
							System.out.println("Cue ball sunk! (shooting 8)");
							updateScore = Integer.MIN_VALUE;
							break outerloop;
						}else if(balls[i].getValue() == 8){
							System.out.println("Eight ball sunk! (shooting 8)");
							updateScore = Integer.MAX_VALUE;
						}else{
							System.out.println("Wrong ball type sunk! (shooting 8)");
							updateScore -= WRONG_BALLTYPE_SUNK;
						}
					}else{
						if(balls[i].getValue() == 0){					//Sunk cue ball
							System.out.println("Cue ball sunk!");
							updateScore += CUE_SCORE;
						}else if(balls[i].getValue() == 8){				//Sunk eight ball
							System.out.println("Eight ball sunk!");
							updateScore = Integer.MIN_VALUE;
							break outerloop;							//Sunk right type of ball
						}else if(balls[i].getValue() > 8 && InferenceEngine.myBallType == BallType.STRIPE
								|| balls[i].getValue() < 8 && InferenceEngine.myBallType == BallType.SOLID){
							System.out.println("Right ball type sunk!");
							updateScore += RIGHT_BALLTYPE_SUNK;
						}else{											//Sunk wrong type of ball
							System.out.println("Wrong ball type sunk!");
							updateScore += WRONG_BALLTYPE_SUNK;
						}
					}
					balls[i].alterX(-1*balls[i].getXPosition() - 1);	//Sinks the ball
					balls[i].alterY(-1*balls[i].getYPosition() - 1);
					velocities[i][0] = 0;
					velocities[i][1] = 0;
				}else{
					for(int j = i; j < balls.length; j++){
						distance = Math.sqrt(
								Math.pow(Math.abs(balls[i].getXPosition() - balls[j].getXPosition()),2) +
								Math.pow(Math.abs(balls[i].getYPosition() - balls[j].getYPosition()), 2));
						nextDistance = Math.sqrt(
								Math.pow(Math.abs(balls[i].getXPosition() + velocities[i][0]*TIME_STEP -
										balls[j].getXPosition() - velocities[j][0]*TIME_STEP),2) +
								Math.pow(Math.abs(balls[i].getYPosition() + velocities[i][1]*TIME_STEP  -
										balls[j].getYPosition() - velocities[j][1]*TIME_STEP), 2));
						if(i != j && distance < Ball.RADIUS &&				//BALL-BALL collision
							nextDistance < distance){
							velocities[i][0] = ballToBallCollision(velocities[i][0], velocities[j][0]);
							velocities[i][1] = ballToBallCollision(velocities[i][1], velocities[j][1]);
							velocities[j][0] = ballToBallCollision(velocities[j][0], velocities[i][0]);
							velocities[j][1] = ballToBallCollision(velocities[j][1], velocities[i][1]);
						}
						if((balls[j].getXPosition() - Ball.RADIUS <= 0 &&	//BALL-BUMPER collision (x)
								velocities[j][0] < 0) ||
								(balls[j].getXPosition() + Ball.RADIUS >= InferenceEngine.MAX_X_COORDINATE &&
								velocities[j][0] > 0)){
							velocities[j][0] = ballToWallCollision(velocities[j][0], true);
							velocities[j][1] = ballToWallCollision(velocities[j][1], false);
							System.out.println(j+ ": Y-Wall collision");
						}
						
						if((balls[j].getYPosition() - Ball.RADIUS <= 0 &&	//BALL-BUMPER collision (y)
								velocities[j][1] < 0) ||
								(balls[j].getYPosition() + Ball.RADIUS >= InferenceEngine.MAX_Y_COORDINATE &&
								velocities[j][1] > 0)){
							velocities[j][0] = ballToWallCollision(velocities[j][0], false);
							velocities[j][1] = ballToWallCollision(velocities[j][1], true);
							System.out.println(j+ ": X-Wall collision");
						}
					}
				}
			}
		
		inMotion = false;
		for(int i = 0; i < velocities.length; i++){
			if(Math.abs(velocities[i][0]) + Math.abs(velocities[i][1]) > 0){
				inMotion = true;
				break;
			}
		}
		
		return updateScore;
	}//update()
	
	/**
	 * Returns the resulting one-dimensional velocity of the first ball according to the initial
	 * one-dimensional velocity in a situation where two balls are colliding.
	 * @param v1 - The first ball's one-dimensional velocity.
	 * @param v2 - The second ball's one-dimensional velocity.
	 * @return The resulting one-dimensional velocity of the first ball.
	 */
	private double ballToBallCollision(double v1, double v2){
		System.out.println("Ball collision");
		return (0.5*(BALL_BALL_COEFFICIENT*(v2-v1) + v1 + v2));
	}//ballToBallCollision()
	
	/**
	 * Returns the resulting one-dimensional velocity of a ball colliding with a wall.
	 * @param v - The ball's one-dimensional velocity.
	 * @param perpendicular - True if the velocity is perpendicular to the wall, false otherwise.
	 * @return The resulting one-dimensional velocity of the ball.
	 */
	private double ballToWallCollision(double v, boolean perpendicular){
		if(perpendicular){
			return -v * BUMPER_COEFFICIENT;
		}else{
			return v;	//TODO do we need to handle this slowdown due to friction?
		}
	}//ballToWallCollision()
	
	/**
	 * Checks if the ball will be sunk into any pocket. Returns true if it will be sunk.
	 * @param ball - The ball to check for whether it has been sunk.
	 * @param v - An array containing the x- and y-components of the velocity (should be of length 2).
	 * @return True if the ball will be sunk or false otherwise.
	 */
	private boolean inPocket(Ball ball, double[] v){
		double midpoint = InferenceEngine.MAX_X_COORDINATE / 2,
				lowMidpoint = midpoint - SIDE_PLAY/2, hiMidpoint = midpoint + SIDE_PLAY/2;
		
		
		return (ball.getXPosition() < SINK_PROXIMITY ||
				ball.getXPosition() > lowMidpoint && ball.getXPosition() < hiMidpoint ||
				ball.getXPosition() > InferenceEngine.MAX_X_COORDINATE - SINK_PROXIMITY) &&
				(ball.getYPosition() < SINK_PROXIMITY ||
						ball.getYPosition() > InferenceEngine.MAX_Y_COORDINATE - SINK_PROXIMITY) &&
				(Math.sqrt(Math.pow(v[0],2) + Math.pow(v[1], 2)) <= MAX_SINK_SPEED);
	}//inPocket()
	
	//    Getters & Setters    //
	public boolean inMotion(){ return inMotion; }//inMotion()
}//SimulationInstance
