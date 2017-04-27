package pcController;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;

import javax.swing.*;


/*
 * Notes:
 * 
 * If a continuous set of shots result in the same score, it might be wise to choose the shot in the middle of that
 * set. This will minimize the effect of precision errors on the shot.
 * Should we use a different method of updating the cue ball for the first few instants due to the nature of shots
 * our machine will be forced to take?
 */

/*
 * TODO Possible errors:
 * Physical constants might be inaccurate or unusable, certain ones should be measured:
 * 		- friction coefficient
 * Equations may not be correct, especially:
 * 		- friction slowdown from hitting wall/ball at an angle
 * 		- Whether a ball is sunk -- might bounce out from angled side pockets
 */

public class SimulationInstance extends TableState{
	private final static double TIME_STEP = 0.001,										//Simulation time step in s
			BUMPER_COEFFICIENT = 0.866, BALL_BALL_COEFFICIENT = 0.96,					//Elastic coefficients
			BALL_TABLE_FRICTION = 0.49035,												//in m/s/s
			INITIAL_LOW_SPEED = 1, INITIAL_MED_SPEED = 1.5, INITIAL_HI_SPEED = 1.34,	//Initial speeds in m/s
			MIN_MOTION = 0.1,															//Slower motion considered 0
			CORNER_MOUTH_WIDTH = 0.1, SIDE_MOUTH_WIDTH = 0.114,							//Pocket openings
			CORNER_SIDE_LENGTH = Math.sqrt(CORNER_MOUTH_WIDTH*CORNER_MOUTH_WIDTH/2),	//Corner side opening length
			CORNER_RADIUS = 0.0625, CORNER_CENTER_X = 0.019, CORNER_CENTER_Y = 0.017,	//Distance to center of pocket
			SIDE_PLAY = SIDE_MOUTH_WIDTH - Ball.RADIUS,
			SINK_PROXIMITY = 0.001;														//Distance at which a ball is sunk in m
	private final static int CUE_SCORE = -20,											//Scoring for sinking certain balls
			RIGHT_BALLTYPE_SUNK = 2, WRONG_BALLTYPE_SUNK = -3, MIN_SCORE = -5000;
	Ball[] balls;
	private double[][] velocities = new double[16][2];
	private int time;
	private boolean inMotion, shooting8;
	
	/*
	 * TESTING VARIABLES
	 */
	private static boolean visual = false;
	private JFrame f;
	private PointPanel panel;
	public static void setVisible(boolean visible){ visual = visible; }
	
	/**
	 * Constructs a new <code>SimulationInstance</code> according to the parameters passed in.<br>
	 * @param positions - A 16-by-2 array containing the coordinates of each ball.
	 * @param shotAngle - The angle relative to the cue ball with which to take this shot (from 0 to 2*pi).
	 * @param shotPower - The power with which to take this shot (from 0 to 1).
	 */
	public SimulationInstance(double[][] positions, double shotAngle, double shotPower){
		super(positions);
		
		int start, end;
		
		time = 0;
		balls = super.deepCopy();
		
		if(visual){
			f = new JFrame();
			panel = new PointPanel(balls);
			f.getContentPane().add(panel);
			f.setSize(1300, 671);
			f.setLocation(75,0);
			f.setVisible(true);
		}
		
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
			velocities[0][0] = INITIAL_LOW_SPEED * Math.cos(shotAngle);
			velocities[0][1] = INITIAL_LOW_SPEED * Math.sin(shotAngle);
		}else if(shotPower <= 0.75){
			velocities[0][0] = INITIAL_MED_SPEED * Math.cos(shotAngle);
			velocities[0][1] = INITIAL_MED_SPEED * Math.sin(shotAngle);
		}else{
			velocities[0][0] = INITIAL_HI_SPEED * Math.cos(shotAngle);
			velocities[0][1] = INITIAL_HI_SPEED * Math.sin(shotAngle);
		}
	}//Constructor
	
	/**
	 * Updates the state of the simulation by one time step according to the appropriate laws of physics.
	 * @return The score earned or lost in this update.
	 */
	public int update(){
		double[][] newVelocities = new double[16][2];
		double distance, nextDistance, fullVelocity;
		int updateScore = 0;
		
		for(int i = 0; i < balls.length; i++){
			newVelocities[i] = velocities[i].clone();
		}
		
		for(int i = 0; i < velocities.length; i++){
			fullVelocity = Math.sqrt(Math.pow(newVelocities[i][0], 2) + Math.pow(newVelocities[i][1], 2));
			
			balls[i].alterX(velocities[i][0] * TIME_STEP);						//Move balls appropriately
			balls[i].alterY(velocities[i][1] * TIME_STEP);
			//Friction slowdown
			newVelocities[i][0] -= BALL_TABLE_FRICTION*TIME_STEP*velocities[i][0];
			newVelocities[i][1] -= BALL_TABLE_FRICTION*TIME_STEP*velocities[i][1];
			
			if(fullVelocity < MIN_MOTION){
				newVelocities[i][0] = newVelocities[i][1] = 0;
			}
			
			if(newVelocities[i][0] != 0 || newVelocities[i][1] != 0){
				if(visual){
					panel.addPoint(balls[i].getXPosition(), balls[i].getYPosition(), i);
				}
			}
		}
		
		outerloop:
			for(int i = 0; i < balls.length; i++){
				while(balls[i].getXPosition() < 0){										//Ignore balls not in motion
					i++;
					if(i == balls.length){
						break outerloop;
					}
				}
				if(!(isWallHere(balls[i].getYPosition(), false) || isWallHere(balls[i].getXPosition(), true))
						& inPocket(balls[i].getXPosition(), balls[i].getYPosition())){	//Ball sunk
					if(shooting8){
						if(balls[i].getValue() == 0){									//Sunk cue ball
							updateScore = MIN_SCORE;
							inMotion = false;
							return updateScore;
						}else if(balls[i].getValue() == 8){
							updateScore += 12;
						}else{
							updateScore -= WRONG_BALLTYPE_SUNK;
						}
					}else{
						if(balls[i].getValue() == 0){								//Sunk cue ball
							updateScore += CUE_SCORE;
						}else if(balls[i].getValue() == 8){							//Sunk eight ball
							updateScore = MIN_SCORE;
							inMotion = false;
							return updateScore;										//Sunk right type of ball
						}else if(balls[i].getValue() > 8 && InferenceEngine.myBallType == BallType.STRIPE
								|| balls[i].getValue() < 8 && InferenceEngine.myBallType == BallType.SOLID){
							updateScore += RIGHT_BALLTYPE_SUNK;
						}else{														//Sunk wrong type of ball
							updateScore += WRONG_BALLTYPE_SUNK;
						}
					}
					balls[i].alterX(-1);											//Sinks the ball
					balls[i].alterY(-1);
					newVelocities[i][0] = 0;
					newVelocities[i][1] = 0;
					if(visual){
						panel.addPoint(-1, -1, i);
					}
				}else{
					if((balls[i].getXPosition() - Ball.RADIUS <= 0 &&				//BALL-BUMPER collision (y-wall)
							newVelocities[i][0] < 0)
							|| (balls[i].getXPosition() + Ball.RADIUS >= InferenceEngine.MAX_X_COORDINATE &&
							newVelocities[i][0] > 0)
							&& isWallHere(balls[i].getYPosition(), false)){
						newVelocities[i][0] = ballToWallCollision(newVelocities[i][0], true);
						newVelocities[i][1] = ballToWallCollision(newVelocities[i][1], false);
					}
					if((balls[i].getYPosition() - Ball.RADIUS <= 0 &&				//BALL-BUMPER collision (x-wall)
							newVelocities[i][1] < 0)
							|| (balls[i].getYPosition() + Ball.RADIUS >= InferenceEngine.MAX_Y_COORDINATE &&
							newVelocities[i][1] > 0)
							&& isWallHere(balls[i].getXPosition(), true)){
						newVelocities[i][0] = ballToWallCollision(newVelocities[i][0], false);
						newVelocities[i][1] = ballToWallCollision(newVelocities[i][1], true);
					}
					
					for(int j = i+1; j < balls.length; j++){
						distance = Math.sqrt(
								Math.pow(Math.abs(balls[i].getXPosition() - balls[j].getXPosition()), 2) +
								Math.pow(Math.abs(balls[i].getYPosition() - balls[j].getYPosition()), 2));
						nextDistance = Math.sqrt(
								Math.pow(Math.abs(balls[i].getXPosition() + velocities[i][0]*TIME_STEP -
										balls[j].getXPosition() - velocities[j][0]*TIME_STEP), 2) +
								Math.pow(Math.abs(balls[i].getYPosition() + velocities[i][1]*TIME_STEP  -
										balls[j].getYPosition() - velocities[j][1]*TIME_STEP), 2));
						
						if(distance < Ball.RADIUS + Ball.RADIUS &&					//BALL-BALL collision
								nextDistance < distance){
							newVelocities[i] = ballToBallCollision(balls[i],
									balls[j],
									velocities[i][0],
									velocities[i][1],
									velocities[j][0],
									velocities[j][1]);
							newVelocities[j] = ballToBallCollision(balls[j],
									balls[i],
									velocities[j][0],
									velocities[j][1],
									velocities[i][0],
									velocities[i][1]);
						}
					}
				}
			}
		
		inMotion = false;
		for(int i = 0; i < newVelocities.length; i++){
			if(Math.abs(newVelocities[i][0]) + Math.abs(newVelocities[i][1]) > 0){
				inMotion = true;
				break;
			}
		}
		
		velocities = newVelocities;
		return updateScore;
	}//update()
	
	/**
	 * Returns the resulting velocity of the first ball according to the initial velocities and positions in a situation where two
	 * balls are colliding.
	 * @param a - The first ball involved in this collision.
	 * @param b - The second ball involved in this collision.
	 * @param vx1 - The x-component of the first ball's velocity.
	 * @param vy1 - The y-component of the first ball's velocity.
	 * @param vx2 - The x-component of the second ball's velocity.
	 * @param vy2 - The y-component of the second ball's velocity.
	 * @return The x- and y-components of the resulting velocity of the first ball.
	 */
	public static double[] ballToBallCollision(Ball a, Ball b, double vx1, double vy1, double vx2, double vy2){
		double alpha, newAlpha, theta1, theta2, x1, y1, x2, y2, v1, v2, v1Angle, v2Angle,
		ax, ay, bx, by, perp, perpx, perpy, para, parax, paray, pi2 = Math.PI*2;
		
		x1 = a.getXPosition();
		y1 = a.getYPosition();
		x2 = b.getXPosition();
		y2 = b.getYPosition();
		
		v1 = Math.sqrt(vx1*vx1 + vy1*vy1);
		v2 = Math.sqrt(vx2*vx2 + vy2*vy2);
		
		v1Angle = angleFromCoordinates(vx1, vy1);			//Angle of velocity vectors
		v2Angle = angleFromCoordinates(vx2, vy2);
		if(v1Angle > Math.PI){
			v1Angle -= pi2;
		}
		if(v2Angle > Math.PI){
			v2Angle -= pi2;
		}
		
		alpha = angleFromCoordinates(x2 - x1, y2 - y1);		//Angle between balls
		
		bx = -Math.cos(alpha);								//Unit vector along direction from ball 2 to ball 1
		by = -Math.sin(alpha);
		
		if(alpha > Math.PI){
			newAlpha = alpha - pi2;
		}else{
			newAlpha = alpha;
		}
		
		theta1 = v1Angle - newAlpha;						//Angle to ball's velocity vector
		theta2 = v2Angle - newAlpha;
		
		if(theta1 > Math.PI){
			theta1 = pi2 - theta1;
			if(v1Angle > theta1){
				theta1 = -theta1;
			}
		}else if(theta1 < -Math.PI){
			theta1 = pi2 + theta1;
			if(v1Angle > theta1){
				theta1 = -theta1;
			}
		}
		
		if(theta1 < 0){
			ax = Math.sin(alpha);							//Unit vector along parallel direction
			ay = -Math.cos(alpha);
		}else{
			ax = -Math.sin(alpha);							//Unit vector along parallel direction
			ay = Math.cos(alpha);
		}
		
		perp = Math.abs(v2*Math.cos(theta2));				//Velocity of ball 2 towards ball 1
		perpx = perp * bx;
		perpy = perp * by;
		
		para = Math.abs(v1*Math.sin(theta1));				//Velocity of ball 1 parallel to ball 2
		parax = para * ax;//-Math.cos(alpha);
		paray = para * ay;//-Math.sin(alpha);
		
		
		return new double[]{								//Add parallel and perpendicular components
				BALL_BALL_COEFFICIENT * (perpx + parax),
				BALL_BALL_COEFFICIENT * (perpy + paray)
		};
	}//ballToBallCollision()
	
	/**
	 * Returns the resulting one-dimensional velocity of a ball colliding with a wall.
	 * @param v - The ball's one-dimensional velocity.
	 * @param perpendicular - True if the velocity is perpendicular to the wall, false otherwise.
	 * @return The resulting one-dimensional velocity of the ball.
	 */
	public static double ballToWallCollision(double v, boolean perpendicular){
		if(perpendicular){
			return -v * BUMPER_COEFFICIENT;
		}else{
			return v;
		}
	}//ballToWallCollision()
	
	/**
	 * Checks if the ball will be sunk into any pocket. Returns true if it will be sunk.
	 * @param ball - The ball to check for whether it has been sunk.
	 * @param v - An array containing the x- and y-components of the velocity (should be of length 2).
	 * @return True if the ball will be sunk or false otherwise.
	 */
	public static boolean inPocket(double x, double y){
		double midpoint = InferenceEngine.MAX_X_COORDINATE / 2,
				lowMidpoint = midpoint - SIDE_PLAY/2, hiMidpoint = midpoint + SIDE_PLAY/2,
				smallX = x - CORNER_CENTER_X, smallY = y - CORNER_CENTER_Y,
				largeX = InferenceEngine.MAX_X_COORDINATE + CORNER_CENTER_X - x,
				largeY = InferenceEngine.MAX_Y_COORDINATE + CORNER_CENTER_Y - y;
		
		return Math.sqrt(Math.pow(smallX, 2) + Math.pow(smallY, 2))
				< CORNER_RADIUS + SINK_PROXIMITY									//bottom-left
				|| Math.sqrt(Math.pow(smallX, 2) + Math.pow(largeY, 2))
				< CORNER_RADIUS + SINK_PROXIMITY 									//top-left
				|| (lowMidpoint < x && hiMidpoint > x
						&& y < SINK_PROXIMITY)										//bottom-center
				|| (lowMidpoint < x && hiMidpoint > x
						&& InferenceEngine.MAX_Y_COORDINATE - y < SINK_PROXIMITY)	//top-center
				|| Math.sqrt(Math.pow(largeX, 2) + Math.pow(smallY, 2))
				< CORNER_RADIUS + SINK_PROXIMITY									//bottom-right
				|| Math.sqrt(Math.pow(largeX, 2) + Math.pow(largeY, 2))
				< CORNER_RADIUS + SINK_PROXIMITY;									//top-right
	}//inPocket()
	
	/**
	 * Returns whether there would be a wall at this position.
	 * @param val - The position along the x- or y-axis.
	 * @param xRail - Whether we are looking for the x- or the y-wall.
	 * @return Whether there would be a wall at this position.
	 */
	public static boolean isWallHere(double position, boolean xRail){
		double a, b, c, d;
		
		if(xRail){
			a = CORNER_SIDE_LENGTH;
			b = InferenceEngine.MAX_X_COORDINATE/2 - SIDE_MOUTH_WIDTH/2;
			c = b + SIDE_MOUTH_WIDTH;
			d = InferenceEngine.MAX_X_COORDINATE - CORNER_SIDE_LENGTH;
			
			return (position > a && position < b) || (position > c && position < d);
		}else{
			a = CORNER_SIDE_LENGTH;
			b = InferenceEngine.MAX_Y_COORDINATE - CORNER_SIDE_LENGTH;
			
			return position > a && position < b;
		}
	}//isWallHere()
	
	/**
	 * Considering a triangle in the origin going in the x- and y-directions as specified, computes the appropriate angle.
	 * @param x - The change in x.
	 * @param y - The change in y.
	 * @return The angle of this right triangle from the origin.
	 */
	public static double angleFromCoordinates(double x, double y){
		if(x == 0){						//Solve alpha
			if(y > 0){					//Special case: coordinate directly along y-axis
				return Math.PI / 2;
			}else{
				return 3*Math.PI / 2;
			}
		}else if(y == 0){
			if(x > 0){					//Special case: coordinate directly along x-axis
				return 0;
			}else{
				return Math.PI;
			}
		}
		
		if(y > 0){
			if(x > 0){
				return Math.atan(y / x);
			}else{
				return Math.PI + Math.atan(y / x);
			}
		}else{
			if(x > 0){
				return 2*Math.PI + Math.atan(y / x);
			}else{
				return Math.PI + Math.atan(y / x);
			}
		}
	}//angleFromCoordinates()
	
	//    Getters & Setters    //
	public boolean inMotion(){ return inMotion; }//inMotion()
}//SimulationInstance

class PointPanel extends JPanel{
	private static final long serialVersionUID = 1L;
	ArrayList<Ellipse2D> pointList, cuePointList;
	
	public PointPanel(Ball[] balls){
		pointList = new ArrayList<Ellipse2D>();
		
		for(int i = 0; i < balls.length; i++){
			pointList.add(new Ellipse2D.Double(0, 0, 0, 0));
		}
		
		addPoint(balls[0].getXPosition(), balls[0].getYPosition(), 0);
		for(int i = 1; i < balls.length; i++){
			addPoint(balls[i].getXPosition(), balls[i].getYPosition(), i);
		}
		
		setBackground(Color.white);
	}
	
	protected void paintComponent(Graphics g){
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		
		g2.setPaint(Color.black);
		g2.draw(pointList.get(0));
		for(int i = 1; i < pointList.size(); i++){
			g2.setPaint(Color.blue);
			g2.fill(pointList.get(i));
		}
		
		g2.drawLine((int)((0.924-0.057)*1300/1.848), 0, (int)((0.924-0.057)*1300/1.848), 10);
		g2.drawLine((int)((0.924+0.057)*1300/1.848), 0, (int)((0.924+0.057)*1300/1.848), 10);
		g2.drawLine((int)((0.924-0.057)*1300/1.848), 638, (int)((0.924-0.057)*1300/1.848), 648);
		g2.drawLine((int)((0.924+0.057)*1300/1.848), 638, (int)((0.924+0.057)*1300/1.848), 648);
		
		g2.drawLine((int)(0.0707*1300/1.848), 0, (int)(0.0707*1300/1.848), 10);
		g2.drawLine((int)(0.0707*1300/1.848), 638, (int)(0.0707*1300/1.848), 648);
		
		g2.drawLine((int)((1.848-0.0707)*1300/1.848), 0, (int)((1.848-0.0707)*1300/1.848), 10);
		g2.drawLine((int)((1.848-0.0707)*1300/1.848), 638, (int)((1.848-0.0707)*1300/1.848), 648);
		
		g2.drawLine(0, (int)((0.921-0.0707)*648/0.921), 10, (int)((0.921-0.0707)*648/0.921));
		g2.drawLine(0, (int)(0.0707*648/0.921), 10, (int)(0.0707*648/0.921));
		
		g2.drawLine(1290, (int)((0.921-0.0707)*648/0.921), 1300, (int)((0.921-0.0707)*648/0.921));
		g2.drawLine(1290, (int)(0.0707*648/0.921), 1300, (int)(0.0707*648/0.921));
	}
	
	public void addPoint(double x, double y, int val){
		x -= Ball.RADIUS;
		y += Ball.RADIUS;
		y = 0.921 - y;
		x *= 1300/1.848;
		y *= 648/0.921;
		
		try{Thread.sleep(1);}catch(Exception e){ e.printStackTrace(); }
		
		pointList.remove(val);
		pointList.add(val, new Ellipse2D.Double(x, y, 40, 40));
		repaint();
	}
}//PointPanel
