package com.fenritz.safecamera;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;

import com.fenritz.safecamera.util.Helpers;
import com.fenritz.safecamera.util.Preview;

public class CameraPreview extends Activity {
    private Preview mPreview;
    Camera mCamera;
    int numberOfCameras;
    int cameraCurrentlyLocked;
    FrameLayout.LayoutParams origParams;

    // The first rear facing camera
    int defaultCameraId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Create a RelativeLayout container that will hold a SurfaceView,
        // and set it as the content of our activity.
        
		setContentView(R.layout.camera);

		((ImageButton) findViewById(R.id.take_photo)).setOnClickListener(takePhoto());
		((ImageButton) findViewById(R.id.decrypt)).setOnClickListener(openGallery());
		
		mPreview = ((Preview)findViewById(R.id.camera_preview));
    }
    
    private OnClickListener openGallery() {
		return new OnClickListener() {
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClass(CameraPreview.this, GalleryActivity.class);
				startActivity(intent);
			}
		};
	}
	
	private OnClickListener takePhoto() {
		return new OnClickListener() {
			public void onClick(View v) {
				if (mCamera != null) {
					mCamera.takePicture(null, null, getPictureCallback());
					FrameLayout.LayoutParams params = (FrameLayout.LayoutParams)mPreview.getLayoutParams();
					origParams = params;
					params.width = 1024;
					params.height = 768;
					
					mPreview.setLayoutParams(params);
				}
			}
		};
	}
	
	class ResumePreview extends Handler {
		@Override
		public void handleMessage(Message msg) {
			if (mCamera != null) {
				mPreview.setLayoutParams(origParams);
				mCamera.startPreview();
			}
		}
	}
	
	private PictureCallback getPictureCallback() {

		return new PictureCallback() {

			public void onPictureTaken(byte[] data, Camera camera) {
				String filename = Helpers.getFilename(CameraPreview.this, Helpers.JPEG_FILE_PREFIX);
				
				new EncryptAndWriteFile(filename).execute(data);
				new EncryptAndWriteThumb(CameraPreview.this, filename).execute(data);
				
				Handler myHandler = new ResumePreview();
				myHandler.sendMessageDelayed(myHandler.obtainMessage(), 500);
			}
		};
	}
    
    @Override
    protected void onResume() {
        super.onResume();

        // Open the default i.e. the first rear facing camera.
        mCamera = Camera.open();
        cameraCurrentlyLocked = defaultCameraId;
        mPreview.setCamera(mCamera);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Because the Camera object is a shared resource, it's very
        // important to release it when the activity is paused.
        if (mCamera != null) {
            mPreview.setCamera(null);
            mCamera.release();
            mCamera = null;
        }
    }
    
    
    public class EncryptAndWriteFile extends AsyncTask<byte[], Void, Void> {
		
		private final String filename;
		private ProgressDialog progressDialog;
		
		@SuppressWarnings("unused")
		public EncryptAndWriteFile(){
			this(null);
		}

		public EncryptAndWriteFile(String pFilename){
			super();
			if(pFilename == null){
				pFilename = Helpers.getFilename(CameraPreview.this, Helpers.JPEG_FILE_PREFIX);
			}
			
			filename = pFilename;
		}
		
		@Override
    	protected void onPreExecute() {
    		super.onPreExecute();
    		progressDialog = ProgressDialog.show(CameraPreview.this, "", getString(R.string.encrypting_photo), false, false);
    	}
		
		@Override
		protected Void doInBackground(byte[]... params) {
			try {
				FileOutputStream out = new FileOutputStream(Helpers.getHomeDir(CameraPreview.this) + "/" + filename);
				Helpers.getAESCrypt().encrypt(params[0], out);
			} 
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			//new EncryptAndWriteThumb(filename).execute(params[0]);
			return null;
		}
		
		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			progressDialog.dismiss();
		}
		
	}
	
	public class EncryptAndWriteThumb extends AsyncTask<byte[], Void, Void> {
		
		private final String fileName;
		private final Context context;
		
		public EncryptAndWriteThumb(Context pContext){
			this(pContext, null);
		}
		
		public EncryptAndWriteThumb(Context pContext, String pFilename){
			super();
			context = pContext;
			if(pFilename == null){
				pFilename = Helpers.getFilename(context, Helpers.JPEG_FILE_PREFIX);
			}
			
			fileName = pFilename;
		}
		
		@Override
		protected Void doInBackground(byte[]... params) {
			try {
				Helpers.generateThumbnail(context, params[0], fileName);
			} 
			catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			return null;
		}
	}

}
