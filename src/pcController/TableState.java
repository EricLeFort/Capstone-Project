package pcController;

/**
 * @author Eric Le Fort
 * @version 1.0
 */
public class TableState{
	private Ball[] balls = new Ball[16];
	
	/**
	 * Constructs a new <code>TableState</code> according to the parameters passed in.<br>
	 * Passing in a position (-1, -1) signifies the ball is not on the table.
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
}//TableState
