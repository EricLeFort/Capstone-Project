package pcController;

/*
 * TODO Possible errors:
 * Balls might end up in collision loop if they don't have time to escape one another's proximity.
 */
public class SimulationInstance extends TableState{
	private final static double TIME_STEP = 0,										//Simulation time step in s TODO find acceptable value
			BALL_SLOWDOWN_FACTOR = 0.7143,											//The rate at which balls slow down in m/s TODO found from website. Accurate?
			BUMPER_COEFFICIENT = 0.866, BALL_BALL_COEFFICIENT = 0.96,				//Elastic coefficients TODO found from website. Accurate?
			INITIAL_LOW_SPEED = 0, INITIAL_MED_SPEED = 0, INITIAL_HI_SPEED = 0,		//Initial speeds in m/s TODO solve for value
			MIN_MOTION = 0.01,														//Motion below this is considered stopped//TODO get value
			CORNER_MOUTH_WIDTH = 0, SIDE_MOUTH_WIDTH = 0,							//Pocket openings //TODO measure
			CORNER_PLAY = CORNER_MOUTH_WIDTH - Ball.RADIUS, SIDE_PLAY = SIDE_MOUTH_WIDTH - Ball.RADIUS,
			SINK_PROXIMITY = 0.001,													//Distance at which a ball is sunk in m //TODO verify
			MAX_SINK_SPEED = 1;														//Speed when balls bounce out of hole in m/s //TODO verify
	private final static int CUE_SCORE = -20,										//Scoring for sinking certain balls
			RIGHT_BALLTYPE_SUNK = 2, WRONG_BALLTYPE_SUNK = -3;
	private double[][] velocities = new double[16][2];
	private int score;
	private boolean inMotion, shooting8;
	
	/**
	 * Constructs a new <code>SimulationInstance</code> according to the parameters passed in.<br>
	 * @param positions - A 16-by-2 array containing the coordinates of each ball.
	 * @param shotAngle - The angle relative to the cue ball with which to take this shot (from 0 to 360).
	 * @param shotPower - The power with which to take this shot (from 0 to 1).
	 */
	public SimulationInstance(double[][] positions, double shotAngle, double shotPower){
		super(positions);
		
		double radAngle = Math.toRadians(shotAngle), speed;
		int start, end;
		
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
		Ball[] balls = super.deepCopy();
		double distance;
		int updateScore = 0;
		
		for(int i = 0; i < velocities.length; i++){		//Move balls appropriately
			balls[i].alterX(velocities[i][0] * TIME_STEP);
			balls[i].alterY(velocities[i][1] * TIME_STEP);
			
			if(velocities[i][0] > 0){					//Slow down from friction in the x-direction
				velocities[i][0] *= BALL_SLOWDOWN_FACTOR;
				if(velocities[i][0] < MIN_MOTION){		//Slowed to a full stop
					velocities[i][0] = 0;
				}
			}else if(velocities[i][0] < 0){
				velocities[i][0] *= BALL_SLOWDOWN_FACTOR;
				if(velocities[i][0] > -1*MIN_MOTION){
					velocities[i][0] = 0;
				}
			}
			
			if(velocities[i][1] > 0){					//Slow down from friction in the y-direction
				velocities[i][1] *= BALL_SLOWDOWN_FACTOR;
				if(velocities[i][1] < MIN_MOTION){
					velocities[i][1] = 0;
				}
			}else if(velocities[i][1] < 0){
				velocities[i][1]  *= BALL_SLOWDOWN_FACTOR;
				if(velocities[i][1] > -1*MIN_MOTION){
					velocities[i][1] = 0;
				}
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
						if(balls[i].getValue() == 0){					//Sunk cue ball
							score = Integer.MIN_VALUE;
							break outerloop;
						}else if(balls[i].getValue() == 8){
							score = Integer.MAX_VALUE;
						}else{
							score -= WRONG_BALLTYPE_SUNK;
						}
					}else{
						if(balls[i].getValue() == 0){					//Sunk cue ball
							score += CUE_SCORE;
						}else if(balls[i].getValue() == 8){				//Sunk eight ball
							score = Integer.MIN_VALUE;
							break outerloop;							//Sunk right type of ball
						}else if(balls[i].getValue() > 8 && InferenceEngine.myBallType == BallType.STRIPE
								|| balls[i].getValue() < 8 && InferenceEngine.myBallType == BallType.SOLID){
							score += RIGHT_BALLTYPE_SUNK;
						}else{											//Sunk wrong type of ball
							score += WRONG_BALLTYPE_SUNK;
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
						if(distance < Ball.RADIUS){						//BALL-BALL collision
							velocities[i][0] = ballToBallCollision(velocities[i][0], velocities[j][0]);
							velocities[i][1] = ballToBallCollision(velocities[i][1], velocities[j][1]);
							velocities[j][0] = ballToBallCollision(velocities[j][0], velocities[i][0]);
							velocities[j][1] = ballToBallCollision(velocities[j][1], velocities[i][1]);
						}
						if(balls[j].getXPosition() - Ball.RADIUS <= 0 ||	//BALL-BUMPER collision (x)
								balls[j].getXPosition() + Ball.RADIUS >= InferenceEngine.MAX_X_COORDINATE){
							ballToWallCollision(velocities[j][0], true);
							ballToWallCollision(velocities[j][1], false);
						}
						
						if(balls[j].getYPosition() - Ball.RADIUS <= 0 ||	//BALL-BUMPER collision (y)
								balls[j].getYPosition() + Ball.RADIUS >= InferenceEngine.MAX_Y_COORDINATE){
							ballToWallCollision(velocities[j][0], false);
							ballToWallCollision(velocities[j][1], true);
						}
					}
				}
			}
		
		inMotion = false;
		for(int i = 0; i < velocities.length; i++){
			if(velocities[i][0] + velocities[i][1] > 0){
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
		return (BALL_BALL_COEFFICIENT*(v2-v1) + v1 + v2) / Ball.MASS;
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
			
		//First check if the ball is sufficiently close to a pocket
		//Check if tangential velocity is slow enough and in correct direction.
	}//inPocket()
	
	//    Getters & Setters    //
	public int getScore(){ return score; }//getScore()
	public boolean inMotion(){ return inMotion; }//inMotion()
}//SimulationInstance
