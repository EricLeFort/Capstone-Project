package testing;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.Test;

import pcController.Ball;
import pcController.PCCommunicator;
import pcController.TableState;

/**
 * @author Eric Le Fort
 * @version 1.0
 */
public class TableStateTest{
	private static final String fullTable = "resources/testingFiles/GoodTableState1.csv",
			oneSolidTable = "resources/testingFiles/GoodTableState2.csv",
			oneStripeTable = "resources/testingFiles/GoodTableState3.csv",
			eightBallTable = "resources/testingFiles/GoodTableState4.csv";
	private static final double DELTA = 1e-15;
	
	@Test
	public void testConstructor(){
		TableState state;
		double[][][] locations;
		
		try{
			locations = new double[][][]{
				PCCommunicator.readTableStateFromFile(new File(fullTable)),
				PCCommunicator.readTableStateFromFile(new File(oneSolidTable)),
				PCCommunicator.readTableStateFromFile(new File(oneStripeTable)),
				PCCommunicator.readTableStateFromFile(new File(eightBallTable)),
				{
					{1, 0.5}, {1, 0.5}, {1, 0.5}, {1, 0.5},
					{1, 0.5}, {1, 0.5}, {1, 0.5}, {1, 0.5},
					{1, 0.5}, {1, 0.5}, {1, 0.5}, {1, 0.5},
					{1, 0.5}, {1, 0.5}, {1, 0.5}, {1, 0.5},
					{1, 0.5}
				},{
					{1, 0.5}, {1, 0.5}, {1, 0.5}, {1, 0.5},
					{1, 0.5}, {1, 0.5}, {1, 0.5}, {1, 0.5},
					{1, 0.5}, {1, 0.5}, {1, 0.5}, {1, 0.5},
					{1, 0.5}, {1, 0.5}, {1, 0.5}
				},{
					{1, 0.5}, {1, 0.5}, {1, 0.5}, {1, 0.5},
					{1, 0.5}, {1, 0.5}, {1, 0.5}, {1, 0.5},
					{1, 0.5}, {1, 0.5}, {1, 0.5}, {1},
					{1, 0.5}, {1, 0.5}, {1, 0.5}, {1, 0.5}
				},{
					{1, 0.5}, {1, 0.5}, {1, 0.5}, {1, 0.5},
					{1, 0.5}, {1, 0.5}, {1, 0.5}, {1, 0.5},
					{1, 0.5}, {1, 0.5}, {1, 0.5}, {1, 0.5, 0},
					{1, 0.5}, {1, 0.5}, {1, 0.5}, {1, 0.5}
				}
			};
			
			for(int i = 0; i < 4; i++){
				state = new TableState(locations[i]);
				for(int j = 0; j < 16; j++){
					assertEquals(locations[i][j][0], state.getBall(j).getXPosition(), DELTA);
					assertEquals(locations[i][j][1], state.getBall(j).getYPosition(), DELTA);
				}
			}
			
			state = null;
			for(int i = 4; i < locations.length; i++){
				try{
					state = new TableState(locations[i]);
				}catch(IllegalArgumentException iae){
					assertTrue(state == null);
				}catch(Exception e){
					fail("Unexpected exception.");
				}
			}
		}catch(FileNotFoundException fnfe){
			fail("Error reading table state file.");
		}
	}//testConstructor()
	
	@Test
	public void testDeepCopy(){
		TableState state;
		Ball[] copy;
		double[][][] locations;
		
		try{
			locations = new double[][][]{
				PCCommunicator.readTableStateFromFile(new File(fullTable)),
				PCCommunicator.readTableStateFromFile(new File(oneSolidTable)),
				PCCommunicator.readTableStateFromFile(new File(oneStripeTable)),
				PCCommunicator.readTableStateFromFile(new File(eightBallTable))
			};
			
			for(int i = 0; i < locations.length; i++){
				state = new TableState(locations[i]);
				copy = state.deepCopy();
				for(int j = 0; j < 16; j++){
					assertTrue(state.getBall(j) != copy[j]);							//Ensure it's not the same object
					assertEquals(locations[i][j][0], copy[j].getXPosition(), DELTA);	//Ensure correct position values
					assertEquals(locations[i][j][1], copy[j].getYPosition(), DELTA);
				}
			}
		}catch(FileNotFoundException fnfe){
			fail("Error reading table state file.");
		}
	}//testDeepCopy()
	
}//TableStateTest
