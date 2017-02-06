package pcController;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;

import javax.swing.*;


/*
 * Notes:
 * soft shots last < 3s
 * medium shots last < 6s
 * hard shots last < 9s
 * 
 * If a continuous set of shots result in the same score, it might be wise to choose the shot in the middle of that
 * set. This will minimize the effect of precision errors on the shot.
 * Should we use a different method of updating the cue ball for the first few instants due to the nature of shots
 * our machine will be forced to take?
 */

/*
 * TODO Possible errors:
 * Physical constants might be inaccurate or unusable, certain ones should be measured:
 /* 	- pocket openings,
 * 		- initial speeds/
 * 		- friction coefficient
 * Equations may not be correct, especially:
 * 		- slowdown of balls,
 * 		- friction slowdown from hitting wall/ball at an angle
 * 		- Ball-Ball collisions -- No consideration of angle of contact.
 * 		- Whether a ball is sunk -- might bounce out from angled side pockets
 * When both balls are moving, they should only both get a portion of the original speed.
 */

public class SimulationInstance extends TableState{
	private final static double TIME_STEP = 0.01,									//Simulation time step in s
			BUMPER_COEFFICIENT = 0.866, BALL_BALL_COEFFICIENT = 0.96,				//Elastic coefficients
			BALL_TABLE_FRICTION = 0.05,
			GRAVITATIONAL_CONSTANT = 9.807,											//in m/s/s
			INITIAL_LOW_SPEED = 1, INITIAL_MED_SPEED = 1.5, INITIAL_HI_SPEED = 2,	//Initial speeds in m/s
			MIN_MOTION = 0.02,														//Motion below this is considered stopped
			CORNER_MOUTH_WIDTH = 0.1, SIDE_MOUTH_WIDTH = 0.114,						//Pocket openings
			CORNER_PLAY = CORNER_MOUTH_WIDTH - Ball.RADIUS, SIDE_PLAY = SIDE_MOUTH_WIDTH - Ball.RADIUS,
			SINK_PROXIMITY = 0.01,													//Distance at which a ball is sunk in m
			MAX_SINK_SPEED = 2;														//Speed when balls bounce out of hole in m/s
	private final static int CUE_SCORE = -20,										//Scoring for sinking certain balls
			RIGHT_BALLTYPE_SUNK = 2, WRONG_BALLTYPE_SUNK = -3;
	Ball[] balls;
	private double[][] velocities = new double[16][2];
	private boolean inMotion, shooting8;
	
	/*
	 * TESTING VARIABLES
	 */
	JFrame f;
	PointPanel panel;
	
	/**
	 * Constructs a new <code>SimulationInstance</code> according to the parameters passed in.<br>
	 * @param positions - A 16-by-2 array containing the coordinates of each ball.
	 * @param shotAngle - The angle relative to the cue ball with which to take this shot (from 0 to 2*pi).
	 * @param shotPower - The power with which to take this shot (from 0 to 1).
	 */
	public SimulationInstance(double[][] positions, double shotAngle, double shotPower){
		super(positions);
		
		f = new JFrame();
		panel = new PointPanel();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.getContentPane().add(panel);
		f.setSize(1300, 648);
		f.setLocation(75,0);
		f.setVisible(true);
		
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
		
		panel.addPoint(balls[0].getXPosition(), balls[0].getYPosition(), true);
		panel.addPoint(balls[1].getXPosition(), balls[1].getYPosition(), false);
		
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
		double[] temp;
		double distance, nextDistance, fullVelocity;
		int updateScore = 0;
		
		for(int i = 0; i < velocities.length; i++){
			fullVelocity = Math.sqrt(Math.pow(velocities[i][0], 2) + Math.pow(velocities[i][1], 2));
			
			if(velocities[i][0] > 0){						//Slow down from friction in the x-direction
				velocities[i][0] -= BALL_TABLE_FRICTION*GRAVITATIONAL_CONSTANT*TIME_STEP*fullVelocity;
			}else if(velocities[i][0] < 0){
				velocities[i][0] += BALL_TABLE_FRICTION*GRAVITATIONAL_CONSTANT*TIME_STEP*fullVelocity;
			}
			
			if(velocities[i][1] > 0){						//Slow down from friction in the y-direction
				velocities[i][1] -= BALL_TABLE_FRICTION*GRAVITATIONAL_CONSTANT*TIME_STEP*fullVelocity;
			}else if(velocities[i][1] < 0){
				velocities[i][1] += BALL_TABLE_FRICTION*GRAVITATIONAL_CONSTANT*TIME_STEP*fullVelocity;
			}
			if(Math.sqrt(Math.pow(velocities[i][0], 2) + Math.pow(velocities[i][1], 2)) < MIN_MOTION){
				velocities[i][0] = velocities[i][1] = 0;
			}
			
			balls[i].alterX(velocities[i][0] * TIME_STEP);	//Move balls appropriately
			balls[i].alterY(velocities[i][1] * TIME_STEP);
			
			if(velocities[i][0] != 0 || velocities[i][1] != 0){
				panel.addPoint(balls[i].getXPosition(), balls[i].getYPosition(), i == 0);
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
								Math.pow(Math.abs(balls[i].getXPosition() - balls[j].getXPosition()), 2) +
								Math.pow(Math.abs(balls[i].getYPosition() - balls[j].getYPosition()), 2));
						nextDistance = Math.sqrt(
								Math.pow(Math.abs(balls[i].getXPosition() + velocities[i][0]*TIME_STEP -
										balls[j].getXPosition() - velocities[j][0]*TIME_STEP),2) +
								Math.pow(Math.abs(balls[i].getYPosition() + velocities[i][1]*TIME_STEP  -
										balls[j].getYPosition() - velocities[j][1]*TIME_STEP), 2));
						if(i != j && distance < Ball.RADIUS + Ball.RADIUS &&//BALL-BALL collision
								nextDistance < distance){
							System.out.println("Ball collision");
							temp = velocities[i];
							velocities[i] = ballToBallCollision(velocities[i][0], velocities[i][1], velocities[j][0], velocities[j][1]);
							velocities[j] = ballToBallCollision(velocities[j][0], velocities[j][1], temp[0], temp[1]);
						}
						if((balls[j].getXPosition() - Ball.RADIUS <= 0 &&	//BALL-BUMPER collision (x)
								velocities[j][0] < 0) ||
								(balls[j].getXPosition() + Ball.RADIUS >= InferenceEngine.MAX_X_COORDINATE &&
								velocities[j][0] > 0)){
							velocities[j][0] = ballToWallCollision(velocities[j][0], true);
							velocities[j][1] = ballToWallCollision(velocities[j][1], false);
							System.out.println(j+ ": Y-Wall collision");
							System.out.println(balls[j].getXPosition());
						}
						
						if((balls[j].getYPosition() - Ball.RADIUS <= 0 &&	//BALL-BUMPER collision (y)
								velocities[j][1] < 0) ||
								(balls[j].getYPosition() + Ball.RADIUS >= InferenceEngine.MAX_Y_COORDINATE &&
								velocities[j][1] > 0)){
							velocities[j][0] = ballToWallCollision(velocities[j][0], false);
							velocities[j][1] = ballToWallCollision(velocities[j][1], true);
							System.out.println(j+ ": X-Wall collision");
							System.out.println(balls[j].getYPosition());
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
	 * Returns the resulting velocity of the first ball according to the initial velocity in a situation where two
	 * balls are colliding.
	 * @param x1 - The first ball's velocity's x-component.
	 * @param x2 - The second ball's velocity's x-component.
	 * @param y1 - The first ball's velocity's y-component.
	 * @param y2 - The second ball's velocity's y-component.
	 * @return The resulting velocity of the first ball.
	 */
	private double[] ballToBallCollision(double x1, double y1, double x2, double y2){
		//TODO account for angle of collision
		double x = x2 - x1, y = y2 - y1,				//relative velocities
				magnitude, theta = Math.atan2(-y, x);	//angle of the perpendicular vector
		x = Math.abs(x);
		y = Math.abs(y);
		
		magnitude =					//magnitude of the resulting vector
				(BALL_BALL_COEFFICIENT)*Math.sqrt(Math.pow(Math.max(x, y), 2) - Math.pow(Math.min(x, y), 2));
		
		return new double[]{
				magnitude*Math.cos(theta),
				-1*magnitude*Math.sin(theta)
		};
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

//TODO remove this whole class
class PointPanel extends JPanel{
	ArrayList<Ellipse2D> pointList, cuePointList;
	
	public PointPanel(){
		pointList = new ArrayList<Ellipse2D>();
		cuePointList = new ArrayList<Ellipse2D>();
		setBackground(Color.white);
	}
	
	protected void paintComponent(Graphics g){
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		
		for(int j = 0; j < pointList.size(); j++){
			g2.setPaint(Color.blue);
			g2.fill(pointList.get(j));
		}
		for(int j = 0; j < cuePointList.size(); j++){
			g2.setPaint(Color.black);
			g2.draw(cuePointList.get(j));
		}
	}
	
	public void addPoint(double x, double y, boolean cue){
		x -= Ball.RADIUS;
		y += Ball.RADIUS;
		y = 0.921 - y;
		x *= 1300/1.848;
		y *= 648/0.921;
		
		try{Thread.sleep(10);}catch(Exception e){}
		
		if(cue){
			cuePointList = new ArrayList<Ellipse2D>();
			cuePointList.add(new Ellipse2D.Double(x, y, 40, 40));
		}else{
			pointList = new ArrayList<Ellipse2D>();
			pointList.add(new Ellipse2D.Double(x, y, 40, 40));
		}
		repaint();
	}
}
