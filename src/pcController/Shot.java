package pcController;

/**
 * @author Eric Le Fort
 * @version 1.0
 */
public class Shot{
	private double xPosition, yPosition, angle, power;
	private int score;
	
	/**
	 * Constructs a new <code>Shot</code> according to the parameters passed in.<br>
	 * Initial score is set to be -1.
	 * @param xPosition - The x-coordinate from which to take this shot (0 to the end of the table).
	 * @param yPosition - The y-coordinate from which to take this shot (0 to the end of the table).
	 * @param angle - The angle relative to the cue ball with which to take this shot (from 0 to 360).
	 * @param power - The power with which to take this shot (from 0 to 1).
	 */
	public Shot(double xPosition, double yPosition, double angle, double power){
		score = -1;
		
		if(yPosition < 0 || yPosition > InferenceEngine.MAX_Y_COORDINATE){			//Verify parameters.
			throw new IllegalArgumentException("Y-Coordinate out of range.");
		}else if(xPosition < 0 || xPosition > InferenceEngine.MAX_X_COORDINATE){
			throw new IllegalArgumentException("X-Coordinate out of range.");
		}else if(angle < 0 || angle >= 360){
			throw new IllegalArgumentException("Angle out of range.");
		}else if(power < 0 || power > 1){
			throw new IllegalArgumentException("Power out of range.");
		}
		
		this.xPosition = xPosition;
		this.yPosition = yPosition;
		this.angle = angle;
		this.power = power;
	}//Constructor
	
	//    GETTERS & SETTERS    //
	public void setScore(int score){ this.score = score; }//setScore()
	public int getScore(){ return score; }//getScore()
	public double getXPosition(){ return xPosition; }//getXPosition()
	public double getYPosition(){ return yPosition; }//getYPosition()
	public double getAngle(){ return angle; }//getAngle()
	public double getPower(){ return power; }//getPower()
}//Shot
