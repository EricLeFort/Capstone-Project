package pcController;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * @author Eric Le Fort
 * @version 1.0
 */
public class PCCommunicator{
	private static File tableStateFile = new File("resources/TableState.csv"),
			imageFile = new File("resources/TableImage.png");
	private static String addFileToPathCmd = "",
			runMatlabCmd = "src/matlabScript";
	
	public static void main(String[] args){
		initiateVR();
	}
	
	public static void uCListener(){
		//TODO
	}//uCListener()
	
	public static void senduCReceipt(){
		//TODO
	}//senduCReceipt()
	
	public static boolean sendShot(Shot shot){
		//TODO
		return false;
	}//sendShot()
	
	public static boolean imageRequest(){
		//TODO USE SERVER TO DOWNLOAD byte[] AND WRITE THAT TO IMAGE FILE.
		return false;
	}//imageRequest()
	
	/**
	 * Runs the MATLAB program "TableStateVR" automatically.
	 */
	public static void initiateVR(){
		Process p;
		
		try{
			p = Runtime.getRuntime().exec(runMatlabCmd);
			p.waitFor();
		}catch (Exception e){
			e.printStackTrace();
		}
		
	}//initiateVR()
	
	public static double[][] readTableStateFromFile(){
		//TODO 
		return null;
	}//readTableStateFromFile()
}//PCCommunicator
