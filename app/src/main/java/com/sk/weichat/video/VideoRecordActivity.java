package com.sk.weichat.video;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;

import com.sk.weichat.R;
import com.sk.weichat.ui.me.ScreenListener;
import com.sk.weichat.util.ToastUtil;

import java.io.File;

class MyDebug {
	static final boolean LOG = true;
}

public class VideoRecordActivity extends Activity {

	public static final String EXTRA_TIME_LIMIT = "time_limit";
	public static final String EXTRA_RESULT_FILE_PATH = "result_file_path";
	public static final String EXTRA_RESULT_TIME_LEN = "result_time_len";
	public static final String EXTRA_RESULT_FILE_SIZE = "result_file_size";
	private Button mTakeVideoBtn;
	private Button mSwitchCameraBtn;

	private MyPreView mMyPreView;

	private SensorManager mSensorManager = null;
	private Sensor mSensorAccelerometer = null;

	private static final String TAG = "RecordActivity";
	private	ScreenListener listener;//锁屏亮屏的监听

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// 设置
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);// 去掉标题栏
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);// 去掉信息栏
		getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);

		setContentView(R.layout.activity_video_record);

		mMyPreView = new MyPreView(this);
		((ViewGroup) findViewById(R.id.preview_layout)).addView(mMyPreView);
		mTakeVideoBtn = (Button) findViewById(R.id.take_video_btn);
		mSwitchCameraBtn = (Button) findViewById(R.id.switch_camera_btn);

		mSwitchCameraBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mMyPreView.switchCamera();
			}
		});
		final long nowtime = System.currentTimeMillis();

		mTakeVideoBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mMyPreView.takePicturePressed();
				/*if (System.currentTimeMillis() > (nowtime+3000)) {

				}else{
					ToastUtil.showToast(VideoRecordActivity.this, "录制时间不能低于一秒！");
				}*/
			}
		});

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
			if (MyDebug.LOG)
				Log.d(TAG, "found accelerometer");
			mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		} else {
			if (MyDebug.LOG)
				Log.d(TAG, "no support for accelerometer");
		}
		listener = new ScreenListener(this);
		listener.begin(new ScreenListener.ScreenStateListener() {
			@Override
			public void onScreenOn() {
				Log.d("wang","开屏");

			}

			@Override
			public void onScreenOff() {
				Log.d("wang", "锁屏");
                  VideoRecordActivity.this.finish();//当锁屏后,让自己被finish掉
			}

			@Override
			public void onUserPresent() {
				Log.d("wang","解锁");

			}
		});
	}
	private SensorEventListener accelerometerListener = new SensorEventListener() {
		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			mMyPreView.onAccelerometerSensorChanged(event);
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		listener.unregisterListener();//销毁监听
	}

	@Override
	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(accelerometerListener, mSensorAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		mMyPreView.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(accelerometerListener);
		mMyPreView.onPause();
	}

	// TODO 这个扫描的处理最后完成，检测兼容性问题
	public void broadcastFile(File file, boolean is_new_picture, boolean is_new_video, long timeLen) {
		if (file == null || !file.exists()) {
			ToastUtil.showToast(this, R.string.record_failed);
			return;
		}
		// note that the new method means that the new folder shows up as a file when connected to a PC via MTP (at least tested on Windows 8)
		if (file.isDirectory()) {
			// this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.fromFile(file)));
			// ACTION_MEDIA_MOUNTED no longer allowed on Android 4.4! Gives: SecurityException: Permission Denial: not allowed to send broadcast android.intent.action.MEDIA_MOUNTED
			// note that we don't actually need to broadcast anything, the folder and contents appear straight away (both in Gallery on device, and on a PC when connecting via MTP)
			// also note that we definitely don't want to broadcast ACTION_MEDIA_SCANNER_SCAN_FILE or use scanFile() for folders, as this means the folder shows up as a file on a
			// PC via MTP (and isn't fixed by rebooting!)
		} else {
			// both of these work fine, but using MediaScannerConnection.scanFile() seems to be preferred over sending an intent
			// this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
			MediaScannerConnection.scanFile(this, new String[] { file.getAbsolutePath() }, null,
					new MediaScannerConnection.OnScanCompletedListener() {
						public void onScanCompleted(String path, Uri uri) {
							if (MyDebug.LOG) {
								Log.d("ExternalStorage", "Scanned " + path + ":");
								Log.d("ExternalStorage", "-> uri=" + uri);
							}
						}
					});
			if (is_new_picture) {
				// Api 14 Camera.ACTION_NEW_PICTURE="android.hardware.action.NEW_PICTURE";
				this.sendBroadcast(new Intent("android.hardware.action.NEW_PICTURE", Uri.fromFile(file)));
				// for compatibility with some apps - apparently this is what used to be broadcast on Android?
				this.sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", Uri.fromFile(file)));
			} else if (is_new_video) {
				// Api 14 Camera.ACTION_NEW_VIDEO="android.hardware.action.NEW_VIDEO";
				this.sendBroadcast(new Intent("android.hardware.action.NEW_VIDEO", Uri.fromFile(file)));
			}
			ToastUtil.showToast(this, R.string.record_succ);
			// 录制成功，返回数据
			Intent intent = new Intent();
			intent.putExtra(EXTRA_RESULT_FILE_PATH, file.getAbsolutePath());
			intent.putExtra(EXTRA_RESULT_TIME_LEN, timeLen);
			intent.putExtra(EXTRA_RESULT_FILE_SIZE, file.length());
			setResult(RESULT_OK, intent);
			finish();
		}
	}

	@Override
	public void onBackPressed() {
		if (mMyPreView.isTakingPhotoOrOnTimer()) {
			mMyPreView.takePicturePressed();
		} else {
			super.onBackPressed();
		}
	}

	// public static final int MEDIA_TYPE_IMAGE = 1;
	// public static final int MEDIA_TYPE_VIDEO = 2;
	//
	// public File getOutputMediaFile(int type) {
	// // To be safe, you should check that the SDCard is mounted
	// // using Environment.getExternalStorageState() before doing this.
	//
	// File mediaStorageDir = getImageFolder();
	// // This location works best if you want the created images to be shared
	// // between applications and persist after your app has been uninstalled.
	//
	// // Create the storage directory if it does not exist
	// if (!mediaStorageDir.exists()) {
	// if (!mediaStorageDir.mkdirs()) {
	// return null;
	// }
	// broadcastFile(mediaStorageDir, false, false);
	// }
	//
	// // Create a media file name
	// String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	// String index = "";
	// File mediaFile = null;
	// for (int count = 1; count <= 100; count++) {
	// if (type == MEDIA_TYPE_IMAGE) {
	// mediaFile = new File(mediaStorageDir.getPath() + File.separator + "IMG_" + timeStamp + index + ".jpg");
	// } else if (type == MEDIA_TYPE_VIDEO) {
	// mediaFile = new File(mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + index + ".mp4");
	// } else {
	// return null;
	// }
	// if (!mediaFile.exists()) {
	// break;
	// }
	// index = "_" + count; // try to find a unique filename
	// }
	// return mediaFile;
	// }

	// public File getImageFolder() {
	// String folder_name = getSaveLocation();
	// File file = null;
	// if (folder_name.length() > 0 && folder_name.lastIndexOf('/') == folder_name.length() - 1) {
	// folder_name = folder_name.substring(0, folder_name.length() - 1);
	// }
	// if (folder_name.startsWith("/")) {
	// file = new File(folder_name);
	// } else {
	// file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), folder_name);
	// }
	// return file;
	// }
	//
	// private String getSaveLocation() {
	// SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
	// String folder_name = sharedPreferences.getString("preference_save_location", "OpenCamera");
	// return folder_name;
	// }
}
