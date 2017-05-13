package testing;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Test;

import pcController.Ball;
import pcController.PCCommunicator;
import pcController.SimulationInstance;

/**
 * @author Eric Le Fort
 * @version 1.0
 */
public class SimulationInstanceTest{
	private static final String fullTable = "resources/testingFiles/GoodTableState1.csv",
			oneSolidTable = "resources/testingFiles/GoodTableState2.csv",
			oneStripeTable = "resources/testingFiles/GoodTableState3.csv",
			eightBallTable = "resources/testingFiles/GoodTableState4.csv";
	private static final double DELTA = 1e-15, ANGLE_PRECISION = 1e-6, VELOCITY_PRECISION = 1e-8;
	
	@Test
	public void testConstructor(){
		SimulationInstance instance;
		double locations[][][], angles[] = new double[]{
				2,
				0,
				2*Math.PI - 0.01,
				2*Math.PI + 0.01,
				2
		},powers[] = new double[]{
				1,
				1,
				1,
				1,
				2
		};
		//		,expectedNextCuePosition[][] = new double[][]{
		//			{},
		//			{},
		//			{},
		//			{},
		//			{}
		//		};
		
		try{
			locations = new double[][][]{
				PCCommunicator.readTableStateFromFile(new File(fullTable)),
				PCCommunicator.readTableStateFromFile(new File(oneSolidTable)),
				PCCommunicator.readTableStateFromFile(new File(oneStripeTable)),
				PCCommunicator.readTableStateFromFile(new File(eightBallTable))
			};
			
			for(int i = 0; i < locations.length; i++){
				for(int j = 0; j < 4; j++){
					instance = new SimulationInstance(locations[i], angles[j], powers[j]);
					for(int k = 0; k < 16; k++){
						assertEquals(instance.getBall(k).getXPosition(), locations[i][k][0], DELTA);
						assertEquals(instance.getBall(k).getYPosition(), locations[i][k][1], DELTA);
					}
					
					instance.update();							//Update positions for single timestep.
					
					for(int k = 1; k < 16; k++){				//Ensure all balls that are not the cue ball have not moved
						assertEquals(instance.getBall(k).getXPosition(), locations[i][k][0], DELTA);
						assertEquals(instance.getBall(k).getYPosition(), locations[i][k][1], DELTA);
					}
				}
				
				instance = null;
				try{
					instance = new SimulationInstance(locations[i], angles[4], powers[4]);
				}catch(IllegalArgumentException iae){
					assertTrue(instance == null);
				}catch(Exception e){
					fail("Unexpected error.");
				}
			}
		}catch(FileNotFoundException fnfe){
			fail("Table state file not found.");
		}
	}//testConstructor()
	
	@Test
	public void testBallToBallCollision(){
		fail("not yet implemented");
		Ball a, b;
		double expected[][] = new double[][]{		//zero, positive, negative theta for each testing category
			{}, {}, {},			//First quadrant, b stationary //TODO compute expected
			{}, {}, {},			//First quadrant, b moving
			{}, {}, {},			//Second quadrant, b stationary
			{}, {}, {},			//Second quadrant, b moving
			{}, {}, {},			//Third quadrant, b stationary
			{}, {}, {},			//Third quadrant, b moving
			{}, {}, {},			//Fourth quadrant, b stationary
			{}, {}, {}			//Third quadrant, b moving
		}, aVelocities[][] = new double[][]{//TODO select initial values
			{}, {}, {},
			{}, {}, {},
			{}, {}, {},
			{}, {}, {},
			{}, {}, {},
			{}, {}, {},
			{}, {}, {},
			{}, {}, {}
		}, bVelocities[][] = new double[][]{//TODO select initial values
			{}, {}, {},
			{}, {}, {},
			{}, {}, {},
			{}, {}, {},
			{}, {}, {},
			{}, {}, {},
			{}, {}, {},
			{}, {}, {}
		}, aPositions[][] = new double[][]{//TODO select initial values
			{}, {}, {},
			{}, {}, {},
			{}, {}, {},
			{}, {}, {},
			{}, {}, {},
			{}, {}, {},
			{}, {}, {},
			{}, {}, {}
		}, bPositions[][] = new double[][]{//TODO select initial values
			{}, {}, {},
			{}, {}, {},
			{}, {}, {},
			{}, {}, {},
			{}, {}, {},
			{}, {}, {},
			{}, {}, {},
			{}, {}, {}
		}, result[];
		
		for(int i = 0; i < expected.length; i++){
			a = new Ball(aPositions[i][0], aPositions[i][1], (byte)1);
			b = new Ball(bPositions[i][0], bPositions[i][1], (byte)2);
			result = SimulationInstance.ballToBallCollision(
					a,
					b,
					aVelocities[i][0],
					aVelocities[i][1],
					bVelocities[i][0],
					bVelocities[i][1]
					);
			assertEquals(result[0], expected[i][0], VELOCITY_PRECISION);
			assertEquals(result[1], expected[i][1], VELOCITY_PRECISION);
		}
	}//testBallToBallCollision()
	
	@Test
	public void testBallToWallCollision(){
		double velocities[] = new double[]{
				5,
				-1.2,
				0,
				0,
				-15.24,
				0.0001
		}, expected[] = new double[]{
				-4.33,
				-1.2,
				0,
				0,
				13.19784,
				-0.0000866
		};
		boolean[] perpendicular = new boolean[]{
				true,
				false,
				true,
				false,
				true,
				true
		};
		
		for(int i = 0; i < velocities.length; i++){
			assertEquals(expected[i],
					SimulationInstance.ballToWallCollision(velocities[i], perpendicular[i]),
					DELTA);
		}
	}//testBallToWallCollision()
	
	@Test
	public void testInPocket(){
		assertFalse(SimulationInstance.inPocket(1, 0.5));
		assertTrue(SimulationInstance.inPocket(0, 0));
		assertFalse(SimulationInstance.inPocket(0.08, 0.05));
		assertTrue(SimulationInstance.inPocket(0, 0.921));
		assertFalse(SimulationInstance.inPocket(0.08, 0.895));
		assertTrue(SimulationInstance.inPocket(0.924, 0));
		assertFalse(SimulationInstance.inPocket(0.92, 0.02));
		assertTrue(SimulationInstance.inPocket(0.924, 0.921));
		assertFalse(SimulationInstance.inPocket(0.967, 0.921));
		assertTrue(SimulationInstance.inPocket(1.848, 0));
		assertFalse(SimulationInstance.inPocket(1.828, 0.07));
		assertTrue(SimulationInstance.inPocket(1.848, 0.921));
		assertFalse(SimulationInstance.inPocket(1.8, 0.87));
	}//testInPocket()
	
	@Test
	public void testIsWallHere(){
		assertFalse(SimulationInstance.isWallHere(0.07070, true));
		assertTrue(SimulationInstance.isWallHere(0.07072, true));
		assertTrue(SimulationInstance.isWallHere(0.866, true));
		assertFalse(SimulationInstance.isWallHere(0.868, true));
		assertFalse(SimulationInstance.isWallHere(0.980, true));
		assertTrue(SimulationInstance.isWallHere(0.982, true));
		assertTrue(SimulationInstance.isWallHere(1.776, true));
		assertFalse(SimulationInstance.isWallHere(1.778, true));
		assertFalse(SimulationInstance.isWallHere(0.07070, false));
		assertTrue(SimulationInstance.isWallHere(0.07072, false));
		assertTrue(SimulationInstance.isWallHere(0.849, false));
		assertFalse(SimulationInstance.isWallHere(0.851, false));
	}//testIsWallHere()
	
	@Test
	public void testAngleFromCoordinates(){
		double coordinates[][] = new double[][]{
			{1, 0},
			{2, 1},
			{0, 1},
			{-1, 2},
			{-1, 0},
			{-1, -5},
			{0, -1},
			{2, -3}
		}, expected[] = new double[]{
				0,
				0.463647609,
				Math.PI/2,
				2.034443936,
				Math.PI,
				4.514993421,
				3*Math.PI/2,
				5.300391584
		};
		
		for(int i = 0; i < coordinates.length; i++){
			assertEquals(expected[i],
					SimulationInstance.angleFromCoordinates(coordinates[i][0], coordinates[i][1]),
					ANGLE_PRECISION);
		}
	}//testAngleFromCoordinates()
	
}//SimulationInstanceTest
