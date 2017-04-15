package testing;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;

import org.junit.Test;

import pcController.PCCommunicator;

/**
 * @author Eric Le Fort
 * @version 1.0
 */
public class PCVRTest{
	private static final String fileNames[] = new String[]{
			"resources/testingImages/TestTable1.jpg",
			"resources/testingImages/TestTable2.jpg",
			"resources/testingImages/TestTable3.jpg",
			"resources/testingImages/TestTable4.jpg",
			"resources/testingImages/TestTable5.jpg"
	}, productionPath = "resources/TableImage.jpg",
	tableState = "resources/TableState.csv";
	private static final double MAX_ERROR = 0.005;		//Positions must be accurate to within 5 millimeters of reality
	
	@Test
	public void testVRProgram(){
		File testImage, orig = new File(productionPath), tableStateFile = new File(tableState);
		double locations[][] ,expected[][][] = new double[][][]{
			{//TODO measure expected locations, include corresponding image files
				{}, {}, {}, {},
				{}, {}, {}, {},
				{}, {}, {}, {},
				{}, {}, {}, {}
			},{
				{}, {}, {}, {},
				{}, {}, {}, {},
				{}, {}, {}, {},
				{}, {}, {}, {}
			},{
				{}, {}, {}, {},
				{}, {}, {}, {},
				{}, {}, {}, {},
				{}, {}, {}, {}
			},{
				{}, {}, {}, {},
				{}, {}, {}, {},
				{}, {}, {}, {},
				{}, {}, {}, {}
			},{
				{}, {}, {}, {},
				{}, {}, {}, {},
				{}, {}, {}, {},
				{}, {}, {}, {}
			}
		};
		int warningMarker;
		
		for(int i = 0; i < expected.length; i++){
			testImage = new File(fileNames[i]);
			
			try{
				if(orig.exists()){							//Replace file on path to be used by the testing image file.
					orig.delete();
				}
				Files.copy(testImage.toPath(), orig.toPath());
			}catch(Exception e){
				fail("Preparation failure.");
			}
			
			PCCommunicator.initiateVR();					//Run VR program and give 15 seconds to complete
			//try{ Thread.sleep(15000); }catch(InterruptedException ie){ } TODO uncomment
			
			try{											//Read in the computed table state data
				locations = PCCommunicator.readTableStateFromFile(tableStateFile);
				
				for(int j = 0; j < locations.length; j++){	//Assure that the measured values are within acceptable error
					assertEquals(expected[i][j][0], locations[j][0], MAX_ERROR);
					assertEquals(expected[i][j][1], locations[j][1], MAX_ERROR);
				}
			}catch(FileNotFoundException fnfe){
				fail("Table state file not located");
			}
		}
	}//testVRProgram()
	
}//PCVRTest
