package pcController;

import java.net.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.Scanner;

import javax.imageio.ImageIO;

/**
 * @author Eric Le Fort
 * @version 1.0
 */
public class PCCommunicator{
	private static TableState tableState;
	private static File tableStateFile = new File("resources/TableState.csv"),
			imageFile = new File("resources/TableImage.png");
	private static String addFileToPathCmd = "", runMatlabCmd = "src/matlabScript", imageFileType = "jpg";
	private final static int port = 100;
	private final static BallType myBallType = BallType.SOLID;
	
	public static void main(String[] args){
		for(;;){ uCListener(); }
	}
	
	public static void uCListener(){
		//TODO Await request from uC
		senduCReceipt();
		
		if(imageRequest()){					//Image received successfully
			initiateVR();
			
			InferenceEngine.updateTableState(readTableStateFromFile(), myBallType);
			
			while(!sendShot(InferenceEngine.getBestShot()));
			System.out.println("The shot has been successfully received!");
		}else{
			System.out.println("Error receiving image.");
		}
	}//uCListener()
	
	public static void senduCReceipt(){
		//TODO
	}//senduCReceipt()
	
	public static boolean sendShot(Shot shot){
		//TODO
		return false;
	}//sendShot()
	
	/**
	 * Creates a socket to be accessed by the camera. Once the socket is established
	 * @return
	 */
	private static boolean imageRequest(){
		ArrayList<Byte> imageByteList = new ArrayList<Byte>();
		byte[] imageByteArray;
		ServerSocket serverSocket;
		Socket clientSocket;
		InputStream in;
		BufferedImage imageStream;
		InputStream fileStream;
		Byte nextByte = null;
		
		try{
			serverSocket = new ServerSocket(port);
			clientSocket = serverSocket.accept();					//Waits for connection to be made
			in = clientSocket.getInputStream();
			
			while((nextByte = (byte)in.read()) == -1){				//Wait patiently for response.
				try{
					Thread.sleep(50);//TODO adjust wait as appropriate.
				}catch(InterruptedException ie){
					ie.printStackTrace();
					serverSocket.close();
					return false;
				}
			}
			//TODO do I need to allow time to complete image transmission?
			
			while(nextByte != -1){
				imageByteList.add(nextByte);
				nextByte = (byte)in.read();
			}
			
			imageByteArray = new byte[imageByteList.size()];		//Converts the ArrayList to a primitive array
			for(int i = 0; i < imageByteArray.length; i++){
				imageByteArray[i] = imageByteList.get(i);
			}
			
			fileStream = new ByteArrayInputStream(imageByteArray);	//Writes image to file using byte array
			imageStream = ImageIO.read(fileStream);
			ImageIO.write(imageStream, imageFileType, imageFile);
			
			serverSocket.close();
			return true;
		}catch(IOException e){
			System.out.println("Exception caught when trying to listen on port "
					+ port + " or listening for a connection");
			System.out.println(e.getMessage());
			return false;
		}
	}//imageRequest()
	
	/**
	 * Runs the MATLAB program "TableStateVR" automatically.
	 */
	private static void initiateVR(){
		Process p;
		
		try{
			p = Runtime.getRuntime().exec(runMatlabCmd);
			p.waitFor();
		}catch (Exception e){
			e.printStackTrace();
		}
		
	}//initiateVR()
	
	
	/**
	 * Reads in an array of 16 ball locations from a file.
	 * @return A 16x2 array of doubles.
	 * @throws IllegalStateException - Thrown if the file is ill-formatted.
	 */
	private static double[][] readTableStateFromFile() throws IllegalStateException{
		Scanner in;
		double[][] locations = new double[16][2];
		String temp[] = new String[2];
		
		try{												//Reads the ids from the text file at the given path into ids.
			in = new Scanner(new FileReader(tableStateFile));
			for(int i = 0; i < 16; i++){
				temp = in.nextLine().split(",");
				locations[i][0] = Double.parseDouble(temp[0]);
				locations[i][1] = Double.parseDouble(temp[1]);
			}
			in.close();
		}catch(FileNotFoundException fnfe){
			fnfe.printStackTrace();
		}catch(InputMismatchException ime){
			ime.printStackTrace();
		}catch(Exception e){
			throw new IllegalStateException("File format invalid.");
		}
		
		return locations;
	}//readTableStateFromFile()
}//PCCommunicator
