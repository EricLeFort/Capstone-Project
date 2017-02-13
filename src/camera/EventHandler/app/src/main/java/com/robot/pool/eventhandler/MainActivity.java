package com.robot.pool.eventhandler;

import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.Manifest;
import android.support.annotation.NonNull;

/**
 * @author Eric Le Fort
 * @version 0.1
 */
public class MainActivity extends AppCompatActivity{
    private static final int REQUEST_CAMERA = 1, REQUEST_WRITE_EXTERNAL_STORAGE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

		int camCheck = ContextCompat.checkSelfPermission(this,			//Check permissions
				Manifest.permission.CAMERA),
				dataCheck = ContextCompat.checkSelfPermission(this,
				Manifest.permission.WRITE_EXTERNAL_STORAGE);

		if(camCheck == PackageManager.PERMISSION_DENIED){				//Acquire permissions
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.CAMERA},
					REQUEST_CAMERA);
		}else{
			listenTakeAndTransmitPhoto();
		}
		if(dataCheck == PackageManager.PERMISSION_DENIED){
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
					REQUEST_WRITE_EXTERNAL_STORAGE);
		}
    }//onCreate()

	public void onRequestPermissionsResult(int requestCode,
										   @NonNull String permissions[],
										   @NonNull int[] grantResults){
		if(grantResults.length > 0
				&& grantResults[0] == PackageManager.PERMISSION_GRANTED){
			listenTakeAndTransmitPhoto();
		}
	}//onRequestPermissionResult()

	private void listenTakeAndTransmitPhoto(){
		ImageCaptureService service;
		//for(;;){ TODO Very difficult to run through more than once.
			service = new ImageCaptureService();
			service.start(this);
		//}
	}//takePhoto()
}//MainActivity