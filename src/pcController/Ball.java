package pcController;

/**
 * @author Eric Le Fort
 * @version 1.0
 */
public class Ball{
	public static final int EIGHT_BALL_NUM = 8, CUE_NUM = 0;
	public static final double RADIUS = .028575,	//radius in m
			MASS = 0.163;							//mass in kg
	private double xPosition, yPosition;
	private byte value;
	
	/**
	 * Constructs a new <code>Ball</code> according to the parameters passed in.<br>
	 * Passing in a position (-1, -1) signifies the ball is not on the table.
	 * @param xPosition - The x-coordinate from which to take this shot (-1 to the end of the table).
	 * @param yPosition - The y-coordinate from which to take this shot (-1 to the end of the table).
	 * @param value - The value representing this ball (from 0 to 15).
	 */
	public Ball(double xPosition, double yPosition, byte value){
		if(yPosition < -1 || yPosition > InferenceEngine.MAX_Y_COORDINATE){			//Verify parameters.
			throw new IllegalArgumentException("Y-Coordinate out of range.");
		}else if(xPosition < -1 || xPosition > InferenceEngine.MAX_X_COORDINATE){
			throw new IllegalArgumentException("X-Coordinate out of range.");
		}else if(value < 0 || value > 15){
			throw new IllegalArgumentException("Value out of range.");
		}
		
		this.xPosition = xPosition;
		this.yPosition = yPosition;
		this.value = value;
	}//Constructor
	
	//    GETTERS & SETTERS    //
	public double getXPosition(){ return xPosition; }//getXPosition()
	public double getYPosition(){ return yPosition; }//getYPosition()
	public byte getValue(){ return value; }//getValue()
	//TODO
}//Ball
