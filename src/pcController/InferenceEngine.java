package pcController;

public class InferenceEngine{
	public static final double MAX_X_COORDINATE = 1.848, MAX_Y_COORDINATE = 0.921;	//max coordinates in m
	public static BallType myBallType = BallType.SOLID;
	private static final double ANGULAR_STEP = 0.0031416, HI_POWER = 1;							//minimum step size in radians
//			LOW_POWER = 0.4, MID_POWER = 0.75, HI_POWER = 1;						//power levels in %
	private static TableState currentTableState;
	private static double[][] positions;
	private static Shot bestShot;
	
	/**
	 * Updates the current <code>TableState</code> to represent the new positions passed in.
	 * @param positions - A 16-by-2 array containing the coordinates of each ball.
	 * @param myBallType - The <code>BallType</code> the computer is shooting for.
	 */
	public static void updateTableState(double[][] positions, BallType myBallType){
		InferenceEngine.positions = positions;
		currentTableState = new TableState(positions);
		bestShot = null;
		InferenceEngine.myBallType = myBallType;
	}//updateTableState()
	
	/**
	 * Returns the optimal shot to take according to the current <code>TableState</code>.<br>
	 * Must calculate the shot if it has not been previously for this <code>TableState</code>.
	 * @return The optimal shot to take according to the current <code>TableState</code>.
	 */
	public static Shot getBestShot(){
		if(currentTableState == null || myBallType == null 		//Necessary values are unusable
				|| myBallType == BallType.CUE){
			return null;
		}
		if(bestShot == null){									//Shot isn't already calculated.
			calculateBestShot();
		}
		return bestShot;
	}//getBestShot()
	
	/**
	 * Uses the current state of the table and simulates possible shots.<br>
	 * Updates the <code>bestShot</code> of this class to the best simulated shot.<br>
	 * Will not generate any shots if there are no suitable target balls on the table.
	 */
	private static void calculateBestShot(){
		double startAngle, lowAngle, highAngle, deltaAngle, deltaX, deltaY, pi2 = 2*Math.PI;
		int startBall, endBall;
		boolean shooting8 = true;
		
		if(myBallType == BallType.SOLID){
			startBall = 1;
			endBall = 7;
		}else{
			startBall = 9;
			endBall = 15;
		}
		
		for(int i = startBall; i <= endBall; i++){						//Checks if there's balls other than the 8 to shoot
			if(currentTableState.getBall(i).getXPosition() >= 0){
				shooting8 = false;
				break;
			}
		}
		if(shooting8){
			startBall = 8;
			endBall = 8;
		}
		
		outerloop:
		for(int i = startBall; i <= endBall; i++){
			while(currentTableState.getBall(i).getXPosition() < 0){		//Ignore balls off the table
				i++;
				if(i > endBall){
					break outerloop;
				}
			}
			deltaX = currentTableState.getBall(i).getXPosition() - currentTableState.getBall(0).getXPosition();
			deltaY = currentTableState.getBall(i).getYPosition() - currentTableState.getBall(0).getYPosition();
			startAngle = Math.atan2(deltaY, deltaX);									//direct cue-target ball angle
			if(startAngle < 0){
				startAngle += pi2;
			}
			
			deltaAngle = (2*Ball.RADIUS)/(Math.sqrt(deltaX*deltaX + deltaY*deltaY));	//arc length / radius
			lowAngle = (startAngle - deltaAngle + pi2) % pi2;							//prevents angle over/underflow
			highAngle = (startAngle + deltaAngle + pi2) % pi2;
			
			lowAngle -= lowAngle % ANGULAR_STEP;
			highAngle -= highAngle % ANGULAR_STEP;

			for(double j = lowAngle; j < highAngle; j += ANGULAR_STEP){					//Iterate through angles
//				simulateShot(new Shot(0, 0, j, LOW_POWER));
//				simulateShot(new Shot(0, 0, j, MID_POWER));
				simulateShot(new Shot(0, 0, j, HI_POWER));
			}
		}
		
		bestShot.setXPosition(currentTableState.getBall(0).getXPosition() +
				Ball.RADIUS * Math.cos((Math.PI + bestShot.getAngle()) % pi2));			//opposite angle
		bestShot.setYPosition(currentTableState.getBall(0).getYPosition() +
				Ball.RADIUS * Math.sin((Math.PI + bestShot.getAngle()) % pi2));
	}//calculateBestShot()
	
	/**
	 * Simulates the results of the <code>Shot</code> passed in and updates the best shot as appropriate.
	 * @param shot - The <code>Shot</code> to be simulated.
	 */
	private static void simulateShot(Shot shot){
		SimulationInstance instance = new SimulationInstance(positions, shot.getAngle(), shot.getPower());
		
		while(instance.inMotion()){				//Updates until the balls have stopped moving.
			shot.alterScore(instance.update());
		}
		
		if(bestShot == null || shot.getScore() > bestShot.getScore()){
			bestShot = shot;
		}
	}//simulateShot()
	
}//InferenceEngine
