package testing;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.NoSuchElementException;

import org.junit.Test;
import pcController.PCCommunicator;

/**
 * @author Eric Le Fort
 * @version 1.0
 */
public class PCCommunicatorTest{
	private static final String goodTableState1 = "resources/testingFiles/GoodTableState1.csv",
			goodTableState2 = "resources/testingFiles/GoodTableState2.csv",
			notAFile = "resources/testingFiles/thisIsntAFileComeOn.csv",
			invalidFile1 = "resources/testingFiles/InvalidTableState1.csv",
			invalidFile2 = "resources/testingFiles/InvalidTableState2.csv",
			invalidFile3 = "resources/testingFiles/InvalidTableState3.csv",
			tempFile = "resources/Temp.csv",
			originalTableState = "resources/TableState.csv";
	private static final double DELTA = 1e-15;
	
	@Test
	public void testReadTableState(){
		double[][] locations,
		expected1 = new double[][]{
			{1.1, 0.0286},
			{0.8, 0.65},
			{1, 0.55},
			{1.5, 0.6},
			{1.819425, 0.4},
			{1.5, 0.2},
			{0.1, 0.8},
			{0.3, 0.892425},
			{0.6, 0.15},
			{0.7, 0.43},
			{0.0286, 0.2},
			{0.9, 0.5},
			{0.6, 0.69},
			{1.4, 0.028575},
			{0.15, 0.37},
			{0.1, 0.6}
		}, expected2 = new double[][]{
			{1.1, 0.0286},
			{-1, -1},
			{-1, -1},
			{1.5, 0.6},
			{-1, -1},
			{-1, -1},
			{-1, -1},
			{0.3, 0.892425},
			{-1, -1},
			{-1, -1},
			{-1, -1},
			{-1, -1},
			{-1, -1},
			{-1, -1},
			{-1, -1},
			{-1, -1}
		};
		
		try{										//Valid file, all balls on table.
			locations = PCCommunicator.readTableStateFromFile(new File(goodTableState1));
			
			assertEquals(expected1.length, locations.length);
			for(int i = 0; i < expected1.length; i++){
				assertEquals(expected1[i].length, locations[i].length);
				for(int j = 0; j < expected1[i].length; j++){
					assertEquals(expected1[i][j], locations[i][j], DELTA);
				}
			}
		}catch(FileNotFoundException fnfe){
			fail("Expected file not located.");
		}
		
		try{										//Valid file, only three balls on table.
			locations = PCCommunicator.readTableStateFromFile(new File(goodTableState2));
			
			assertEquals(expected2.length, locations.length);
			for(int i = 0; i < expected2.length; i++){
				assertEquals(expected2[i].length, locations[i].length);
				for(int j = 0; j < expected2[i].length; j++){
					assertEquals(expected2[i][j], locations[i][j], DELTA);
				}
			}
		}catch(FileNotFoundException fnfe){
			fail("Expected file not located.");
		}
		
		locations = null;
		try{										//File not found
			locations = PCCommunicator.readTableStateFromFile(new File(notAFile));
		}catch(FileNotFoundException fnfe){
			assert(locations == null);
		}catch(Exception e){
			fail("Unexpected error occurred.");
		}
		
		try{										//Not a number
			locations = PCCommunicator.readTableStateFromFile(new File(invalidFile1));
		}catch(NumberFormatException nane){
			assert(locations == null);
		}catch(Exception e){
			fail("Unexpected error occurred.");
		}
		
		try{										//Short line
			locations = PCCommunicator.readTableStateFromFile(new File(invalidFile2));
		}catch(IndexOutOfBoundsException ioobe){
			assert(locations == null);
		}catch(Exception e){
			fail("Unexpected error occurred.");
		}
		
		try{										//Not enough data
			locations = PCCommunicator.readTableStateFromFile(new File(invalidFile3));
		}catch(NoSuchElementException nsee){
			assert(locations == null);
		}catch(Exception e){
			fail("Unexpected error occurred.");
		}
	}//testReadTableState()

	@Test
	public void testInitiateVR(){
		File temp = new File(tempFile);
		File orig = new File(originalTableState);
		boolean check;
		
		try{
			Files.copy(orig.toPath(), temp.toPath());	//Copy original file
			Files.delete(orig.toPath());				//Delete original file
		}catch(IOException ioe){
			fail("Preparation failure.");
		}
		
		assertTrue(!orig.exists());						//Verify old file was deleted
		
		PCCommunicator.initiateVR();
		//TODO do I wait here to give VR time?
		
		check = orig.exists();							//Verify file was created -- shows that VR program ran
		
		try{
			if(orig.exists()){
				Files.delete(orig.toPath());			//Remove new file, otherwise copy fails
			}
			Files.copy(temp.toPath(), orig.toPath());	//Copy original file
			Files.delete(temp.toPath());				//Delete original file
		}catch(IOException ioe){
			ioe.printStackTrace();
			System.out.println("WARNING: ORIGINAL FILE NOT COPIED BACK SUCCESSFULLY.");
		}
		
		assertTrue(check);
	}//testInitiateVR()
	
	@Test
	public void testImageRequest(){
		fail("Not yet implemented.");
	}//testImageRequest()
	
}//PCCommunicatorTest
