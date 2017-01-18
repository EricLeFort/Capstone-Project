package pcController;

/**
 * @author Eric Le Fort
 * @version 1.0
 */
public class TableState{
	private Ball[] balls = new Ball[16];
	
	/**
	 * Constructs a new <code>TableState</code> according to the parameters passed in.<br>
	 * Passing in a position (-x, -y) signifies the ball is not on the table.
	 * @param positions - A 16-by-2 array containing the coordinates of each ball.
	 */
	public TableState(double[][] positions){
		if(positions.length != 16){
			throw new IllegalArgumentException("Positions array not of length 16.");
		}
		
		for(byte i = 0; i < positions.length; i++){
			if(positions[i].length != 2){
				throw new IllegalArgumentException("Element in array not of length 2.");
			}
			balls[i] = new Ball(positions[i][0], positions[i][1], i);
		}
	}//Constructor
	
	/**
	 * Returns the ball at index <code>i</code>.
	 * @param i - The index of the ball to be returned.
	 * @return The ball at index <code>i</code>.
	 */
	Ball getBall(int i){ return balls[i]; }//getBall()
	
	/**
	 * Creates and returns a deep copy of the array of <code>Balls</code> in the current <code>TableState</code>.
	 * @return An array of type <code>Ball</code> with length 16.
	 */
	Ball[] deepCopy(){
		Ball[] balls = new Ball[16];
		
		for(byte i = 0; i < balls.length; i++){
			balls[i] = new Ball(this.balls[i].getXPosition(), this.balls[i].getYPosition(), i);
		}
		return balls;
	}//deepCopy
}//TableState
