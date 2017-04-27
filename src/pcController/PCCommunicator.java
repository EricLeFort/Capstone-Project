package pcController;

import gnu.io.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.TooManyListenersException;
import javax.imageio.ImageIO;

/**
 * @author Eric Le Fort
 * @version 1.0
 */
public class PCCommunicator implements SerialPortEventListener{
	private static File tableStateFile = new File("resources/TableState.csv"),
			imageFile = new File("resources/TableImage.jpg");
	private static final String PORT_NAME_1 = "/dev/tty.usbmodem1411",
			PORT_NAME_2 = "COM5";															//TODO different name for Windows
	private static final int PORT = 8000, TIMEOUT = 20000, SETUP_DELAY = 1500, DATA_RATE = 9600,
			SHOT_SPEC = 170, REQUEST = 55, CONFIRM = 200;
	private static final BallType myBallType = BallType.SOLID;
	
	SerialPort serialPort;
	private static BufferedReader input;
	private static OutputStream output;
	private static String runCCmd = "src/pcVR/TableStateVR.exe", imageFileType = "jpg";
	private static boolean requestReceived, confirmReceived;
	
	public static void main(String[] args){
		PCCommunicator arduinoComm = new PCCommunicator();
		arduinoComm.initializeSerialConnection();
		
		try{ Thread.sleep(SETUP_DELAY); }catch(InterruptedException ie){ }
		
		for(;;){ uCListener(); }
	}
	
	/**
	 * Waits for a request from the microcontroller and initiates all necessary steps in order to compute and transmit
	 * the shot to be taken.
	 */
	public static void uCListener(){
		Shot shot;
		
		requestReceived = false;
		while(!requestReceived){			//Await request
			try{ Thread.sleep(100); }catch(InterruptedException ie){ }
		}
		
		senduCReceipt();
		initiateVR();
		
		try{
			InferenceEngine.updateTableState(readTableStateFromFile(tableStateFile), myBallType);
			shot = InferenceEngine.getBestShot();
			
			System.out.println(shot);
			SimulationInstance.setVisible(true);		//Display anticipated result of optimal shot.
			InferenceEngine.simulateShot(shot);
			SimulationInstance.setVisible(false);
			
			try{
				if(tableStateFile.exists()){			//Trash old table state after use
					Files.delete(tableStateFile.toPath());
				}
			}catch(IOException ioe){
				System.out.println("Old table state file not deleted.");
			}
			
			while(!sendShot(shot));
			confirmReceived = false;
		}catch(FileNotFoundException fnfe){
			System.out.println("Table state file not found.");
		}
	}//uCListener()
	
	/**
	 * Sends a receipt message to the microcontroller to indicate that the request was successfully received.
	 */
	public static void senduCReceipt(){
		try{
			output.write(CONFIRM);
			output.flush();
		}catch(IOException ioe){
			System.out.println("Communication error.");
		}
	}//senduCReceipt()
	
	/**
	 * Communicates the specification for the selected shot to the microcontroller.
	 * @param shot - The <code>Shot</code> to be communicated to the microcontroller.
	 * @return Whether the shot was successfully received by the microcontroller.
	 */
	public static boolean sendShot(Shot shot){
		int count = 0;
		
		writeStringBytes((float)SHOT_SPEC);
		writeStringBytes((float)shot.getXPosition());
		writeStringBytes((float)shot.getYPosition());
		writeStringBytes((float)shot.getAngle());
		writeStringBytes((float)shot.getPower());
		try{
			output.write('\n');
			output.flush();
		}catch(IOException e){ }
		
		
		System.out.println("Shot communicated!");
		
		while(!confirmReceived){
			try{ Thread.sleep(10); }catch(InterruptedException ie){ }
			count++;
			if(count*10 > TIMEOUT){			//We've waited too long.
				System.out.println("Communication timeout");
				return false;
			}
		}
		return true;
	}//sendShot()
	
	/**
	 * Creates a socket to be accessed by the camera. Once the socket is established it receives image data
	 * from the image capture device.
	 * @return Whether the image was successfully received or not.
	 */
	public static boolean imageRequest(){
		ArrayList<Byte> imageByteList = new ArrayList<Byte>();
		byte[] imageByteArray;
		ServerSocket serverSocket;
		Socket clientSocket;
		InputStream in;
		BufferedImage imageStream;
		InputStream fileStream;
		
		try{
			serverSocket = new ServerSocket(PORT);
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
			System.out.println("Bytes received: " + imageByteList.size());
			
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
					+ PORT + " or listening for a connection");
			System.out.println(e.getMessage());
			return false;
		}
	}//imageRequest()
	
	/**
	 * Runs the MATLAB program "TableStateVR" automatically.
	 */
	public static void initiateVR(){
		ProcessBuilder pb;
		Process p;
		BufferedReader in;
		String line;
		
		try{
			pb = new ProcessBuilder(runCCmd);
			pb.redirectErrorStream(true);
			
			p = pb.start();
			in = new BufferedReader(new InputStreamReader(p.getInputStream())); 
			
			while(!(line = in.readLine()).equals("finished")){
				System.out.println(line);
			}
			System.out.println("Finished");
		}catch (Exception e){
			e.printStackTrace();
		}
	}//initiateVR()
	
	/**
	 * Reads in an array of 16 ball locations from a file.
	 * @param fileName - The file to read from.
	 * @return A 16x2 array of doubles.
	 * @throws IllegalStateException - Thrown if the file is ill-formatted.
	 */
	public static double[][] readTableStateFromFile(File fileName)
			throws FileNotFoundException, NoSuchElementException, IndexOutOfBoundsException, NumberFormatException{
		Scanner in;
		double[][] locations = new double[16][2];
		String temp[] = new String[2];
		
		in = new Scanner(new FileReader(fileName));
		for(int i = 0; i < 16; i++){
			temp = in.nextLine().split(",");
			locations[i][0] = Double.parseDouble(temp[0]);
			locations[i][1] = Double.parseDouble(temp[1]);
		}
		in.close();
		
		return locations;
	}//readTableStateFromFile()
	
	/**
	 * Initializes the connection and sets the parameters for the communication with the microcontroller.
	 */
	public void initializeSerialConnection(){
		CommPortIdentifier portId = null, currPortId;
		Enumeration portEnum = CommPortIdentifier.getPortIdentifiers();
		
		while(portEnum.hasMoreElements()){
			currPortId = (CommPortIdentifier) portEnum.nextElement();
			if(currPortId.getName().equals(PORT_NAME_1)
					|| currPortId.getName().equals(PORT_NAME_2)){
				portId = currPortId;
				break;
			}
		}
		
		if(portId == null){
			System.out.println("Could not find port.");
			return;
		}
		
		try{
			serialPort = (SerialPort) portId.open(this.getClass().getName(), TIMEOUT);
			
			serialPort.setSerialPortParams(DATA_RATE,
					SerialPort.DATABITS_8,
					SerialPort.STOPBITS_1,
					SerialPort.PARITY_NONE);
			
			input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
			output = serialPort.getOutputStream();
			
			serialPort.addEventListener(this);
			serialPort.notifyOnDataAvailable(true);
		}catch(PortInUseException piue){
			System.out.println("Port is already in use.");
		}catch(UnsupportedCommOperationException ucoe){
			System.out.println("Unsupported communication operation selected.");
		}catch(TooManyListenersException tmle){
			System.out.println("Too many listeners.");
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}//initializeSerialConnection()
	
	/**
	 * This method is invoked when there is an event that occurred relating to the serial port.
	 * It will read in the data from the input buffer if there is any available.
	 */
	@Override
	public void serialEvent(SerialPortEvent event){
		String msg = "<!-- -->";
		int value;
		
		if(event.getEventType() == SerialPortEvent.DATA_AVAILABLE){
			try{
				msg = input.readLine();
				value = Integer.valueOf(msg);
				System.out.println("Received: " + value);
				if(value == REQUEST){
					requestReceived = true;
				}else if(value == CONFIRM){
					confirmReceived = true;
				}
			}catch(IOException ioe){
				System.out.println("Error reading input stream.");
			}catch(NumberFormatException nfe){
				System.out.println("Received: " + msg);
			}
		}else{
			System.out.println("Non-data available event.");
		}
	}//serialEvent()
	
	/**
	 * Handles cleaning up the Arduino communication infrastructure on termination.
	 */
	public synchronized void close(){
		if(serialPort != null){
			serialPort.removeEventListener();
			serialPort.close();
		}
	}//close()
	
	/**
	 * Writes the provided <code>String</code> character by character into the output buffer.
	 * @param f - The <code>String</code> to be sent.
	 */
	private static void writeStringBytes(float f){
		String data = "" + f;
		
		for(int i = 0; i < data.length(); i++){
			try{
				output.write(data.charAt(i));
				output.flush();
			}catch(IOException ioe){
				System.out.println("Error transmitting data.");
			}
		}
		
		try{
			Thread.sleep(1200);				//Pause for the Arduino to catch up
		}catch(InterruptedException ie){ ie.printStackTrace(); }
	}//writeStringBytes()
}//PCCommunicator
