package pcController;

public class Testing{
	
	public static void main(String[] args){
		double[][] positions = new double[][]{
			new double[]{ 1.7, 0.6 },	//Cue	
			new double[]{ 1.75, 0.85 },	//1
			new double[]{ -1, -1 },		//2	
			new double[]{ -1, -1 },		//3
			new double[]{ -1, -1 },		//4
			new double[]{ -1, -1 },		//5
			new double[]{ -1, -1 },		//6
			new double[]{ -1, -1 },		//7
			new double[]{ -1, -1 },		//8
			new double[]{ -1, -1 },		//9
			new double[]{ -1, -1 },		//10
			new double[]{ -1, -1 },		//11
			new double[]{ -1, -1 },		//12
			new double[]{ -1, -1 },		//13		
			new double[]{ -1, -1 },		//14		
			new double[]{ -1, -1 },		//15
		};
		Shot optimal;
		
		InferenceEngine.updateTableState(positions, BallType.SOLID);
		optimal = InferenceEngine.getBestShot();
		
		System.out.println("Score: " + optimal.getScore());
		System.out.println("Angle: " + optimal.getAngle());
		System.out.println("Power: " + optimal.getPower());
		System.out.println();
		System.out.println("X: " + optimal.getXPosition());
		System.out.println("Y: " + optimal.getYPosition());
	}
	
}
