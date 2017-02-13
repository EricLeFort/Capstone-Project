package com.robot.pool.eventhandler;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.camera2.*;
import android.media.Image;
import android.view.Surface;
import android.graphics.BitmapFactory;
import android.media.ImageReader;
import android.os.IBinder;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eric Le Fort
 * @version 1.0
 */
public class ImageCaptureService extends Service{
	private static CameraDevice camera;
	private static CameraManager manager;
	private static CaptureRequest.Builder captureBuilder;
	private static ImageReader imageReader;
	private static Handler backgroundHandler;
	private static HandlerThread backgroundThread;
	private static String cameraID;

	private CameraDevice.StateCallback cameraStateCallback = new CameraDevice.StateCallback(){
		@Override
		public void onOpened(@NonNull CameraDevice camera){
			ImageCaptureService.camera = camera;
			takePicture();
		}//onOpened()

		@Override
		public void onDisconnected(@NonNull CameraDevice camera){
			closeCamera();
		}//onDisconnected()

		@Override
		public void onError(@NonNull CameraDevice camera, int error){
			closeCamera();
		}//onError()
	};
	final private CameraCaptureSession.CaptureCallback captureListener =
			new CameraCaptureSession.CaptureCallback(){
		@Override
		public void onCaptureCompleted(@NonNull CameraCaptureSession session,
									   @NonNull CaptureRequest request,
									   @NonNull TotalCaptureResult result){
			super.onCaptureCompleted(session, request, result);

			closeCamera();
		}
	};

	public void start(Activity activity){
		manager = (CameraManager)activity.getSystemService(CAMERA_SERVICE);
		cameraID = getCamera(manager);

		try{
			startBackgroundThread();
			manager.openCamera(cameraID, cameraStateCallback, null);
		}catch(CameraAccessException | SecurityException e){ e.printStackTrace(); }
	}//start()

	private void takePicture(){
		Size[] dimensions;
		CameraCharacteristics character;

		try{
			character = manager.getCameraCharacteristics(cameraID);
			dimensions = character.get(
					CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
					.getOutputSizes(ImageFormat.JPEG);

			imageReader = ImageReader.newInstance(dimensions[0].getWidth(),
					dimensions[0].getHeight(),
					ImageFormat.JPEG,
					1);

			final List<Surface> outputSurfaces = new ArrayList<>(2);
			outputSurfaces.add(imageReader.getSurface());

			captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
			captureBuilder.addTarget(imageReader.getSurface());
			captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);

			ImageReader.OnImageAvailableListener readerListener = (ImageReader readerL) -> {
				final Image image = readerL.acquireLatestImage();
				final ByteBuffer buffer = image.getPlanes()[0].getBuffer();
				final byte[] bytes = new byte[buffer.capacity()];

				buffer.get(bytes);
				processImage(bytes);

				image.close();
			};
			imageReader.setOnImageAvailableListener(readerListener, backgroundHandler);

			camera.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback(){
				@Override
				public void onConfigured(@NonNull CameraCaptureSession session){
					try{
						session.capture(captureBuilder.build(), captureListener, backgroundHandler);
					}catch(CameraAccessException cae){ cae.printStackTrace(); }
				}

				@Override
				public void onConfigureFailed(@NonNull CameraCaptureSession session){}
			}, backgroundHandler);
		}catch(CameraAccessException cae){ cae.printStackTrace(); }
	}//takePicture()

	private void startBackgroundThread(){
		if(backgroundThread == null){
			backgroundThread = new HandlerThread("Camera Background" + cameraID);
			backgroundThread.start();
			backgroundHandler = new Handler(backgroundThread.getLooper());
		}
	}//startBackgroundThread()

	private void stopBackgroundThread(){
		backgroundThread.quitSafely();

		try{
			backgroundThread.join();
			backgroundThread = null;
			backgroundHandler = null;
		}catch(InterruptedException ie){ ie.printStackTrace(); }
	}//stopBackgroundThread()

	private void closeCamera(){
		if(camera != null){
			camera.close();
			stopBackgroundThread();
			camera = null;
		}
		if(imageReader != null){
			imageReader.close();
			imageReader = null;
		}
	}//closeCamera()

	@Override
	public IBinder onBind(Intent intent){
		return null;
	}//onBind()

	private String getCamera(CameraManager manager){
		try{
			for(String cameraId : manager.getCameraIdList()){
				if(manager.getCameraCharacteristics(cameraId)
						.get(CameraCharacteristics.LENS_FACING)
						== CameraCharacteristics.LENS_FACING_FRONT){
					return cameraId;
				}
			}
		}catch(CameraAccessException e){ e.printStackTrace(); }

		return null;
	}//getCamera()

	private void processImage(byte[] bytes){
		OutputStream outStream;
		Socket socket = null;
		String address = "10.0.2.2";
		Bitmap bmp;
		int port = 8000;

		try{
			bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, null);

			while(socket == null){
				try{
					socket = new Socket(address, port);		//Attach to socket
				}catch(ConnectException ce){ }
			}
			System.out.println("Connection established.");

			outStream = socket.getOutputStream();

			bmp.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
			outStream.flush();

			System.out.println("File transmitted.");
			socket.close();									//Disconnect from socket
		}catch(IOException ioe){ ioe.printStackTrace(); }
	}//processImage(byte[])
}//ImageCaptureService