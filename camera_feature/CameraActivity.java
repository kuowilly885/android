
package com.insyde.factorytest;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.TableRow;
import android.hardware.Camera.CameraInfo;

public class CameraActivity extends Activity
{
	//broadcast from PC
	//ex. : am broadcast -a com.insyde.factorytest.result.manualui.rear.blemish --ez result true
	// The prefix must be com.insyde.factorytest and the last word must the name of the test item.
	private static final String ResultTakePic = "com.insyde.factorytest.result.manualui.takepic";
	private static final String ResultRearBlem = "com.insyde.factorytest.result.manualui.rear.blemish";
	private static final String ResultRearShad = "com.insyde.factorytest.result.manualui.rear.shade";
	private static final String ResultFocus = "com.insyde.factorytest.result.manualui.result.focus";
	private static final String ResultPreview = "com.insyde.factorytest.result.manualui.result.preview";

	// Message IDs for handler to change layouts & button's color
	private static final int MESSAGE_MAKE_BLEM_RED = 0;
	private static final int MESSAGE_MAKE_BLEM_GREEN = 1;
	private static final int MESSAGE_MAKE_SHAD_RED = 2;
	private static final int MESSAGE_MAKE_SHAD_GREEN = 3;
	private static final int MESSAGE_MAKE_REAR_RED = 4;
	private static final int MESSAGE_MAKE_REAR_GREEN = 5;
	private static final int MESSAGE_MAKE_FRONT_RED = 6;
	private static final int MESSAGE_MAKE_FRONT_GREEN = 7;
	private static final int MESSAGE_SET_BUTTONS = 8;

	// All Test Items, display as a big button
	private Button mBlem;
	private Button mShad;
	private Button mFront;
	private Button mRear;

	// Check each test item is pass or not
	private boolean isRearTakePicPass = false;
	private boolean isFrontTakePicPass = false;
	private boolean isBlemPass = false;
	private boolean isShadPass = false;

	// Check each test item is tested or not
	private boolean isRearTakePicTested = false;
	private boolean isFrontTakePicTested = false;
	private boolean isBlemTested = false;
	private boolean isShadTested = false;

	// Other variable defines below
	private Camera myCamera = null;
	private int myCameraType = CameraInfo.CAMERA_FACING_BACK;
	private SurfaceView previewSurfaceView;
	private SurfaceHolder previewSurfaceHolder;
	private boolean previewing = false;
	private Context mContext;
	private Thread mWorkThread = null;
	private boolean isSurfaceCreated = false;
	private Object lockForSurfaceCreate = new Object();
	private static final String TAG = "FactoryTest:CameraActivity";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cam);

		// Fix orientation
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

	    // Get Activity context.
	    mContext = this;

		// Get Camera Type
		myCameraType = getIntent().getIntExtra("myCameraType", 0) == 0 ? CameraInfo.CAMERA_FACING_BACK : CameraInfo.CAMERA_FACING_FRONT;
		mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_SET_BUTTONS));

		//Let the window pick PixelFormat itself.
	    getWindow().setFormat(PixelFormat.UNKNOWN);

		// Get the previewing view, set it callback
	    previewSurfaceView = (SurfaceView)findViewById(R.id.cam_surface);
	    previewSurfaceHolder = previewSurfaceView.getHolder();
	    previewSurfaceHolder.addCallback(new surfaceCallback());

		// Get all buttons from layout
	    mBlem = (Button) findViewById(R.id.cam_rear_blem);
	    mShad = (Button) findViewById(R.id.cam_rear_shad);
	    mFront = (Button) findViewById(R.id.cam_front);
	    mRear = (Button) findViewById(R.id.cam_rear);

	    // Set buttons disabled
	    mBlem.setEnabled(false);
	    mShad.setEnabled(false);
	    mFront.setEnabled(false);
	    mRear.setEnabled(false);
	}

	public void onStart() {
		super.onStart();

		// If need to receive result from PC, register the receiver
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ResultTakePic);
		intentFilter.addAction(ResultRearBlem);
		intentFilter.addAction(ResultRearShad);
		intentFilter.addAction(ResultFocus);
		intentFilter.addAction(ResultPreview);
		registerReceiver(mBroadcastReceiver, intentFilter);
		new ResultThread().start();

		// Open camera when start
		mWorkThread = new Thread(new openCam());
		mWorkThread.start();
	}

	public void onStop() {
		super.onStop();
		unregisterReceiver(mBroadcastReceiver);
		if(myCamera != null) {
			// Release Camera when leave.
			myCamera.stopPreview();
			myCamera.release();
			myCamera = null;
		}

		mWorkThread = null;

		// If the test item is tested, set it as tested
		isRearTakePicTested = isBlemTested = isShadTested = true;
	}

	// Receive intent from PC, don't need the change the name.
	private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        public void onReceive(final Context context, final Intent intent) {
			String action = intent.getAction();
			if (action.equals(ResultTakePic)) {
				if (myCameraType == CameraInfo.CAMERA_FACING_BACK) {
					isRearTakePicPass = intent.getBooleanExtra("result", false);
					if (isRearTakePicPass)
						mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_MAKE_REAR_GREEN));
					else
						mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_MAKE_REAR_RED));

					// If the test item is tested, set it as tested
					isRearTakePicTested = true;
				} else {
					isFrontTakePicPass = intent.getBooleanExtra("result", false);
					if (isFrontTakePicPass)
						mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_MAKE_FRONT_GREEN));
					else
						mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_MAKE_FRONT_RED));

					// If the test item is tested, set it as tested
					isFrontTakePicTested = true;
				}
			} else if (action.equals(ResultRearBlem)) {
				isBlemPass = intent.getBooleanExtra("result", false);
				if (isBlemPass)
					mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_MAKE_BLEM_GREEN));
				else
					mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_MAKE_BLEM_RED));

				// If the test item is tested, set it as tested
				isBlemTested = true;
			} else if (action.equals(ResultRearShad)) {
				isShadPass = intent.getBooleanExtra("result", false);
				if (isShadPass)
					mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_MAKE_SHAD_GREEN));
				else
					mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_MAKE_SHAD_RED));

				// If the test item is tested, set it as tested
				isShadTested = true;
			} else if (action.equals(ResultFocus)) {
				if (intent.getBooleanExtra("result", false))
					CameraUtil.writeLog("afstatus", "1");
				else
					CameraUtil.writeLog("afstatus", "0");
			} else if(action.equals(ResultPreview)) {
				if (intent.getBooleanExtra("result", false))
					CameraUtil.writeLog("previewstatus", "1");
				else
					CameraUtil.writeLog("previewstatus", "0");
			}
        }
    };

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
		if(e.getAction() == KeyEvent.ACTION_DOWN) {
			switch(e.getKeyCode()) {
				case KeyEvent.KEYCODE_A:
					Log.d(TAG,"get keyevent A");
					// focus & capture
					if (!mWorkThread.isAlive()) {
						mWorkThread = new Thread(new focusAndTakePic());
						mWorkThread.start();
					}
					return true;
				case KeyEvent.KEYCODE_B:
					Log.d(TAG,"get keyevent B");
					// Capture
					if (!mWorkThread.isAlive()) {
						mWorkThread = new Thread(new takePic());
						mWorkThread.start();
					}
					return true;
				case KeyEvent.KEYCODE_C:
					Log.d(TAG,"get keyevent C");
					// Switch Front/Rear Camera
					if (!mWorkThread.isAlive()) {
						mWorkThread = new Thread(new closeCam());
						mWorkThread.start();
						try {mWorkThread.join();} catch (InterruptedException e1) {/*get interrupted*/}
						switch (myCameraType) {
							case CameraInfo.CAMERA_FACING_BACK:
								myCameraType = CameraInfo.CAMERA_FACING_FRONT;
								break;
							case CameraInfo.CAMERA_FACING_FRONT:
								myCameraType = CameraInfo.CAMERA_FACING_BACK;
								break;
						}
						mWorkThread = new Thread(new openCam());
						mWorkThread.start();
						mHandler.sendMessage(mHandler.obtainMessage(MESSAGE_SET_BUTTONS));
					}
					return true;
				case KeyEvent.KEYCODE_F:
					Log.d(TAG,"get keyevent F");
					// Capture
					if (!mWorkThread.isAlive()) {
						mWorkThread = new Thread(new closeCam());
						mWorkThread.start();
					}
					try {mWorkThread.join();} catch (InterruptedException e1) {/*get interrupted*/}
					finish();
					return true;
			}
		}
		return super.dispatchKeyEvent(e);
	};

	// This handler is used to change color of the buttons and layouts. Don't need to change the name.
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
			switch(msg.what) {
				case MESSAGE_MAKE_BLEM_RED:
					mBlem.setBackgroundColor(Color.RED);
					break;
				case MESSAGE_MAKE_BLEM_GREEN:
					mBlem.setBackgroundColor(Color.GREEN);
					break;
				case MESSAGE_MAKE_SHAD_RED:
					mShad.setBackgroundColor(Color.RED);
					break;
				case MESSAGE_MAKE_SHAD_GREEN:
					mShad.setBackgroundColor(Color.GREEN);
					break;
				case MESSAGE_MAKE_REAR_RED:
					mRear.setBackgroundColor(Color.RED);
					break;
				case MESSAGE_MAKE_REAR_GREEN:
					mRear.setBackgroundColor(Color.GREEN);
					break;
				case MESSAGE_MAKE_FRONT_RED:
					mFront.setBackgroundColor(Color.RED);
					break;
				case MESSAGE_MAKE_FRONT_GREEN:
					mFront.setBackgroundColor(Color.GREEN);
					break;
				case MESSAGE_SET_BUTTONS:
					TableRow.LayoutParams invisible =
							new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT);
					invisible.weight = 0;
					TableRow.LayoutParams visible =
							new TableRow.LayoutParams(0, TableRow.LayoutParams.MATCH_PARENT);
					visible.weight = 1;
					switch (myCameraType) {
						case CameraInfo.CAMERA_FACING_BACK:
							mBlem.setLayoutParams(visible);
							mShad.setLayoutParams(visible);
							mRear.setLayoutParams(visible);
							mFront.setLayoutParams(invisible);
							break;
						case CameraInfo.CAMERA_FACING_FRONT:
							mBlem.setLayoutParams(invisible);
							mShad.setLayoutParams(invisible);
							mRear.setLayoutParams(invisible);
							mFront.setLayoutParams(visible);
							break;
					}
					break;
			}
		}
	};

	// This thread is used to check all test items are tested. If so, return the result to MainActivity.
	class ResultThread extends Thread {
		public void run() {
			// exist this loop if all test items are tested.
			while(!isRearTakePicTested || !isFrontTakePicTested || !isBlemTested || !isShadTested) {
				SystemClock.sleep(1000);
			}

			// check the result of all test items. If any fail, set result to fail.
			if(isRearTakePicPass && isFrontTakePicPass && isBlemPass && isShadPass) {
	            LogUtil.writeLog("Manual_Rear_Camera_BlemishCheckLog.bat", "GetRear_Camera_Blemish_TestResult", "PASS");
				setResult(RESULT_OK);
			}
			else {
	            LogUtil.writeLog("Manual_Rear_Camera_BlemishCheckLog.bat", "GetRear_Camera_Blemish_TestResult", "FAIL");
				setResult(RESULT_FIRST_USER);	//fail
			}

			finish();
		}
	}

// Write all testing classes/method belows this line ##########################

	private class openCam implements Runnable {
		public void run() {
			Log.d(TAG,"openCam thread is running");
			if(!previewing && myCamera == null) {
			    CameraInfo cameraInfo = new CameraInfo();
			    int cameraCount = 0;
			    cameraCount = Camera.getNumberOfCameras();
			    for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
			        Camera.getCameraInfo(camIdx, cameraInfo);
			        if (cameraInfo.facing == myCameraType) {
			        	// Create Camera
			        	myCamera = Camera.open(camIdx);
			            break;
			        }
			    }

				// Set Camera pic size
				Camera.Parameters params = myCamera.getParameters();
				for (Camera.Size size : params.getSupportedPictureSizes()) {
					if(size.height > 700) {
						params.setPictureSize(size.width, size.height);
					}
				}
				myCamera.setParameters(params);

                 //setCameraDisplayOrientation
				int rotation = ((Activity) mContext).getWindowManager().getDefaultDisplay().getRotation();
				int degrees = 0;
				switch (rotation) {
					case Surface.ROTATION_0: degrees = 0; break;
					case Surface.ROTATION_90: degrees = 90; break;
					case Surface.ROTATION_180: degrees = 180; break;
					case Surface.ROTATION_270: degrees = 270; break;
				}
				int result;
				if (cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT) {
					result = (cameraInfo.orientation + degrees) % 360;
					result = (360 - result) % 360;  // compensate the mirror
				}
				else {//back
					result = (cameraInfo.orientation - degrees + 360) % 360;
				}
				myCamera.setDisplayOrientation(result);

				// Set camera data stream on the surfaceview.
				try {
					if(!isSurfaceCreated) {
				        synchronized(lockForSurfaceCreate) {try { lockForSurfaceCreate.wait(); } catch (InterruptedException e) {/*treat interrupt as exit request*/}}
					}
					myCamera.setPreviewDisplay(previewSurfaceHolder);
					myCamera.startPreview();
				} catch (Exception e) {
                    Log.e(TAG, "failed to preview : " + e.getLocalizedMessage());
					// Set previewstatus
                    mContext.sendBroadcast(new Intent(ResultPreview).putExtra("result", false));
					myCamera.release();
					myCamera = null;
                    return;
				}

				// Set previewstatus
                mContext.sendBroadcast(new Intent(ResultPreview).putExtra("result", true));

				// Set previewing status true.
				previewing = true;
			}
		}
	}

	private class closeCam implements Runnable {
		public void run() {
			Log.d(TAG,"closeCam thread is running");
			if(previewing && myCamera != null) {
				myCamera.stopPreview();
				myCamera.release();
				myCamera = null;

				// Set surfaceview to black.
				previewSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);

				// Set surfaceview to OPAQUE.
				previewSurfaceHolder.setFormat(PixelFormat.OPAQUE);

				// Set previewing status false.
				previewing = false;
			}
		}
	}

	private class focusAndTakePic implements Runnable {
		public void run() {
			Log.d(TAG,"focusAndTakePic thread is running");
			if(previewing && myCamera != null) {
				Camera.Parameters params = myCamera.getParameters();

				boolean autoFocusSupport = false;
				boolean macroFocusSupport = false;

				for (String mode : params.getSupportedFocusModes()) {
					if (mode.equals(Camera.Parameters.FOCUS_MODE_AUTO)) {
						autoFocusSupport = true;
					} else if (mode.equals(Camera.Parameters.FOCUS_MODE_MACRO))
						macroFocusSupport = true;
				}

				if (autoFocusSupport)
					params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
				else if (macroFocusSupport)
					params.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
				myCamera.setParameters(params);

				try {
					myCamera.autoFocus(autoFocusCallback);
					if(!(autoFocusSupport || macroFocusSupport)) throw new Exception("camera not support focus");
				} catch (Exception e) {
                    Log.e(TAG, "Camera focus failed : " + e.getLocalizedMessage());
					autoFocusSupport = false;
					macroFocusSupport = false;
				}

				if(autoFocusSupport || macroFocusSupport)
					mContext.sendBroadcast(new Intent(ResultFocus).putExtra("result", true));
				else
					mContext.sendBroadcast(new Intent(ResultFocus).putExtra("result", false));
			}
        }
	}

	private AutoFocusCallback autoFocusCallback = new AutoFocusCallback() {
		@Override
		public void onAutoFocus(boolean success, Camera camera) {
			try {mWorkThread.join();} catch (InterruptedException e1) {/*get interrupted*/}
			mWorkThread = new Thread(new takePic());
			mWorkThread.start();
		}
	};

	private class surfaceCallback implements SurfaceHolder.Callback {
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d(TAG, "surfaceChanged");
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
            Log.d(TAG, "surfaceCreated");
            isSurfaceCreated = true;
	        synchronized(lockForSurfaceCreate) {try{lockForSurfaceCreate.notifyAll();}catch(IllegalMonitorStateException e){Log.e(TAG, "notify failed");}}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d(TAG, "surfaceDestroyed");
            isSurfaceCreated = false;
		}
	}

	private class takePic implements Runnable {
		public void run() {
			Log.d(TAG,"takePic thread is running");
			if(previewing && myCamera != null) {
				try {
					myCamera.takePicture(null, null, jpegPictureCallback);
				} catch (Exception e) {
                    Log.e(TAG, "Camera failed takepic : " + e.getLocalizedMessage());
                    mContext.sendBroadcast(new Intent(ResultTakePic).putExtra("result", false));
                    return;
				}
			}
		}
	}

	private PictureCallback jpegPictureCallback = new PictureCallback() {
		@Override
		public void onPictureTaken(byte[] arg0, Camera arg1) {
			Log.d(TAG,"jpegPictureCallback onPictureTaken called");
			Bitmap bitmapPicture = BitmapFactory.decodeByteArray(arg0, 0, arg0.length);
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			bitmapPicture.compress(Bitmap.CompressFormat.JPEG, 100, stream);
			byte[] byteArray = stream.toByteArray();
			String fileName = myCameraType == CameraInfo.CAMERA_FACING_BACK ?
					mContext.getString(R.string.manualui_rear_cam_filename) : mContext.getString(R.string.manualui_front_cam_filename);
			try {
				CameraUtil.savePhoto(fileName, byteArray);
			} catch(IOException ioe) {
                Log.e(TAG, "Camera failed takepic : " + ioe.getLocalizedMessage());
                mContext.sendBroadcast(new Intent(ResultTakePic).putExtra("result", false));
				return;
			}

			arg1.startPreview();

            mContext.sendBroadcast(new Intent(ResultTakePic).putExtra("result", true));
		}
	};
}
