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
	private static File tableStateFile = new File("resources/TableState.csv"),
			imageFile = new File("resources/TableImage.jpg");
	private static String runMatlabCmd = "src/matlabScript", imageFileType = "jpg";
	private final static int port = 8000;
	private final static BallType myBallType = BallType.SOLID;
	
	public static void main(String[] args){
		for(;;){ uCListener(); }
	}
	
	public static void uCListener(){
		Scanner in = new Scanner(System.in);
		//TODO Await request from uC
		//		senduCReceipt();
		
		InferenceEngine.updateTableState(readTableStateFromFile(), myBallType);
		InferenceEngine.getBestShot();
		
		if(imageRequest()){					//Image received successfully
			//			initiateVR();
			
			//			InferenceEngine.updateTableState(readTableStateFromFile(), myBallType);
			
			//			in.next();
			//			while(!sendShot(InferenceEngine.getBestShot()));
		}else{
			System.out.println("Error receiving image.");
		}
		in.close();
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
		
		try{
			serverSocket = new ServerSocket(port);
			System.out.println("Waiting for connection..");
			clientSocket = serverSocket.accept();						//Waits for connection to be made
			System.out.println("Connection established.");
			in = clientSocket.getInputStream();
			
			while(in.available() == 0){									//Wait patiently for response.
				try{
					Thread.sleep(50);
				}catch(InterruptedException ie){
					ie.printStackTrace();
					serverSocket.close();
					return false;
				}
			}
			
			while(in.available() != 0){
				imageByteList.add((byte)in.read());
			}
			System.out.println("Bytes received: " +imageByteList.size());
			
			imageByteArray = new byte[imageByteList.size()];			//Converts the ArrayList to a primitive array
			for(int i = 0; i < imageByteArray.length; i++){
				imageByteArray[i] = imageByteList.get(i);
			}
			
			fileStream = new ByteArrayInputStream(imageByteArray);		//Writes image to file using byte array
			imageStream = ImageIO.read(fileStream);
			ImageIO.write(imageStream, imageFileType, imageFile);
			
			serverSocket.close();
			System.out.println("The shot has been successfully received!");
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
	private static double[][] readTableStateFromFile()
			throws IllegalStateException, FileNotFoundException, InputMismatchException{
		Scanner in;
		double[][] locations = new double[16][2];
		String temp[] = new String[2];
		
		in = new Scanner(new FileReader(tableStateFile));
		for(int i = 0; i < 16; i++){
			temp = in.nextLine().split(",");
			locations[i][0] = Double.parseDouble(temp[0]);
			locations[i][1] = Double.parseDouble(temp[1]);
		}
		in.close();
		
		return locations;
	}//readTableStateFromFile()
}//PCCommunicator
