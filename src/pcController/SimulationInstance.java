package pcController;

public class SimulationInstance extends TableState{
	private final static double TIME_STEP = 0,										//TODO simulate time step in s
			TABLE_BALL_FRICTION = 0,												//TODO table-ball static friction
			BUMPER_COEFFICIENT= 0, BALL_BALL_COEFFICIENT = 0,						//TODO elastic coefficients
			INITIAL_LOW_SPEED = 0, INITIAL_MED_SPEED = 0, INITIAL_HI_SPEED = 0;		//TODO initial speeds in m/s
	private double[][] velocities = new double[16][2];
	private int score;
	private boolean inMotion;
	
	/**
	 * Constructs a new <code>SimulationInstance</code> according to the parameters passed in.<br>
	 * @param positions - A 16-by-2 array containing the coordinates of each ball.
	 * @param shotAngle - The angle relative to the cue ball with which to take this shot (from 0 to 360).
	 * @param shotPower - The power with which to take this shot (from 0 to 1).
	 */
	public SimulationInstance(double[][] positions, double shotAngle, double shotPower){
		super(positions);
		
		double radAngle = Math.toRadians(shotAngle), speed;
		
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
		super.getBalls();
		//TODO
	}//update()
	
	//    Getters & Setters    //
	public int getScore(){ return score; }//getScore()
	public boolean inMotion(){ return inMotion; }//inMotion()
}//SimulationInstance
