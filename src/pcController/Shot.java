package pcController;

public class Shot{
	private double xPosition, yPosition, angle, power;
	private int score;
	
	public Shot(double xPosition, double yPosition, double angle, double power){
		score = -1;
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
