package com.sk.weichat.video;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.SensorEvent;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.view.Display;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.sk.weichat.R;
import com.sk.weichat.util.FileUtil;

/**
 * 根据OpenCamera修改的，只保留了基本的录像功能
 * 
 * @author Dean Tao
 * @version 1.0
 */
@SuppressWarnings({ "unused", "deprecation" })
public class MyPreView extends SurfaceView implements SurfaceHolder.Callback {
	private static final String TAG = "PreView";

	private SurfaceHolder mHolder = null;
	private Camera mCamera = null;
	private int mCameraId = 0;
	private int display_orientation;// 方向

	// 整个app的控制
	private boolean has_surface = false;// Surface是否已经创建
	private boolean app_is_paused = true;// activity 是否在暂停中

	// Video Size和Quality
	// video_quality can either be:
	// - an int, in which case it refers to a CamcorderProfile
	// - of the form [CamcorderProfile]_r[width]x[height] - we use the CamcorderProfile as a base, and override the video resolution - this is needed to support resolutions which
	// don't have corresponding camcorder profiles
	private List<String> video_quality = null;
	private int current_video_quality = -1; // this is an index into the video_quality array, or -1 if not found (though this shouldn't happen?)
	private List<Camera.Size> video_sizes = null;
	private int[] current_fps_range = new int[2];
	private SparseArray<Pair<Integer, Integer>> video_quality_pair;
	private List<Camera.Size> sizes = null;
	private List<Camera.Size> supported_preview_sizes = null;

	// focus mode
	private List<String> supported_focus_values = null; // our "values" format
	private int current_focus_index = -1; // this is an index into the supported_focus_values array, or -1 if no focus modes available
	private int focus_success = FOCUS_DONE;
	private static final int FOCUS_WAITING = 0;
	private static final int FOCUS_SUCCESS = 1;
	private static final int FOCUS_FAILED = 2;
	private static final int FOCUS_DONE = 3;
	private long focus_complete_time = -1;

	private boolean successfully_focused = false;
	private long successfully_focused_time = -1;
	private String set_flash_after_autofocus = "";// 未知
	private boolean has_focus_area = false;
	private int focus_screen_x = 0;
	private int focus_screen_y = 0;
	private boolean touch_was_multitouch = false;
	private Matrix camera_to_preview_matrix = new Matrix();
	private Matrix preview_to_camera_matrix = new Matrix();

	private boolean is_preview_started = false;
	private boolean is_video = false;

	private boolean has_aspect_ratio = false;
	private double aspect_ratio = 0.0f;

	public static final int EXPECT_WIDTH = 480;// 期望的宽
	public static final int EXPECT_HEIGHT = 320;// 期望的高

	private int current_orientation = 0; // orientation received by onOrientationChanged
	private Camera.CameraInfo camera_info = new Camera.CameraInfo();
	private int current_rotation = 0; // orientation relative to camera's orientation (used for parameters.setOrientation())

	public MyPreView(Context context) {
		super(context);
		mHolder = getHolder();
		mHolder.addCallback(this);
		// deprecated setting, but required on Android versions prior to 3.0
		mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS); // deprecated
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// The Surface has been created, acquire the camera and tell it where
		// to draw.
		has_surface = true;
		openCamera();
		setWillNotDraw(false); // see http://stackoverflow.com/questions/2687015/extended-surfaceviews-ondraw-method-never-called
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		if (MyDebug.LOG)
			Log.d(TAG, "surfaceChanged " + w + ", " + h);
		// surface size is now changed to match the aspect ratio of camera preview - so we shouldn't change the preview to match the surface size, so no need to restart preview
		// here
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		if (MyDebug.LOG)
			Log.d(TAG, "surfaceDestroyed()");
		// Surface will be destroyed when we return, so stop the preview.
		// Because the CameraDocanevice object is not a shared resource, it's very
		// important to release it when the activity is paused.
		this.has_surface = false;
		this.closeCamera();
	}

	// for taking photos - from http://developer.android.com/reference/android/hardware/Camera.Parameters.html#setRotation(int)
	private void onOrientationChanged(int orientation) {
		/*
		 * if( MyDebug.LOG ) { Log.d(TAG, "onOrientationChanged()"); Log.d(TAG, "orientation: " + orientation); }
		 */
		if (orientation == OrientationEventListener.ORIENTATION_UNKNOWN)
			return;
		if (mCamera == null)
			return;
		Camera.getCameraInfo(mCameraId, camera_info);
		orientation = (orientation + 45) / 90 * 90;
		this.current_orientation = orientation % 360;
		int new_rotation = 0;
		if (camera_info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			new_rotation = (camera_info.orientation - orientation + 360) % 360;
		} else { // back-facing camera
			new_rotation = (camera_info.orientation + orientation) % 360;
		}
		if (new_rotation != current_rotation) {
			/*
			 * if( MyDebug.LOG ) { Log.d(TAG, "    current_orientation is " + current_orientation); Log.d(TAG, "    info orientation is " + camera_info.orientation); Log.d(TAG,
			 * "    set Camera rotation from " + current_rotation + " to " + new_rotation); }
			 */
			this.current_rotation = new_rotation;
		}
	}

	void onAccelerometerSensorChanged(SensorEvent event) {
		this.invalidate();
	}

	private void calculateCameraToPreviewMatrix() {
		camera_to_preview_matrix.reset();
		// from http://developer.android.com/reference/android/hardware/Camera.Face.html#rect
		Camera.getCameraInfo(mCameraId, camera_info);
		// Need mirror for front camera.
		boolean mirror = (camera_info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT);
		camera_to_preview_matrix.setScale(mirror ? -1 : 1, 1);
		// This is the value for android.hardware.Camera.setDisplayOrientation.
		camera_to_preview_matrix.postRotate(display_orientation);
		// Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
		// UI coordinates range from (0, 0) to (width, height).
		camera_to_preview_matrix.postScale(this.getWidth() / 2000f, this.getHeight() / 2000f);
		camera_to_preview_matrix.postTranslate(this.getWidth() / 2f, this.getHeight() / 2f);
	}

	private void calculatePreviewToCameraMatrix() {
		calculateCameraToPreviewMatrix();
		if (!camera_to_preview_matrix.invert(preview_to_camera_matrix)) {
			if (MyDebug.LOG)
				Log.d(TAG, "calculatePreviewToCameraMatrix failed to invert matrix!?");
		}
	}

	@TargetApi(14)
	private ArrayList<Camera.Area> getAreas(float x, float y) {
		float[] coords = { x, y };
		calculatePreviewToCameraMatrix();
		preview_to_camera_matrix.mapPoints(coords);
		float focus_x = coords[0];
		float focus_y = coords[1];

		int focus_size = 50;
		if (MyDebug.LOG) {
			Log.d(TAG, "x, y: " + x + ", " + y);
			Log.d(TAG, "focus x, y: " + focus_x + ", " + focus_y);
		}
		Rect rect = new Rect();
		rect.left = (int) focus_x - focus_size;
		rect.right = (int) focus_x + focus_size;
		rect.top = (int) focus_y - focus_size;
		rect.bottom = (int) focus_y + focus_size;
		if (rect.left < -1000) {
			rect.left = -1000;
			rect.right = rect.left + 2 * focus_size;
		} else if (rect.right > 1000) {
			rect.right = 1000;
			rect.left = rect.right - 2 * focus_size;
		}
		if (rect.top < -1000) {
			rect.top = -1000;
			rect.bottom = rect.top + 2 * focus_size;
		} else if (rect.bottom > 1000) {
			rect.bottom = 1000;
			rect.top = rect.bottom - 2 * focus_size;
		}

		ArrayList<Camera.Area> areas = new ArrayList<Camera.Area>();
		areas.add(new Camera.Area(rect, 1000));
		return areas;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getPointerCount() != 1) {
			// multitouch_time = System.currentTimeMillis();
			touch_was_multitouch = true;
			return true;
		}
		if (event.getAction() != MotionEvent.ACTION_UP) {
			if (event.getAction() == MotionEvent.ACTION_DOWN && event.getPointerCount() == 1) {
				touch_was_multitouch = false;
			}
			return true;
		}
		if (touch_was_multitouch) {
			return true;
		}

		if (this.isTakingPhotoOrOnTimer()) {
			return true;
		}

		// note, we always try to force start the preview (in case is_preview_paused has become false)
		startCameraPreview();
		cancelAutoFocus();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (mCamera != null) {
				Camera.Parameters parameters = mCamera.getParameters();
				String focus_mode = parameters.getFocusMode();
				this.has_focus_area = false;
				if (parameters.getMaxNumFocusAreas() != 0
						&& (focus_mode.equals(Camera.Parameters.FOCUS_MODE_AUTO) || focus_mode.equals(Camera.Parameters.FOCUS_MODE_MACRO)
								|| focus_mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) || focus_mode
									.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))) {
					if (MyDebug.LOG)
						Log.d(TAG, "set focus (and metering?) area");
					this.has_focus_area = true;
					this.focus_screen_x = (int) event.getX();
					this.focus_screen_y = (int) event.getY();

					ArrayList<Camera.Area> areas = getAreas(event.getX(), event.getY());
					parameters.setFocusAreas(areas);
					// also set metering areas
					if (parameters.getMaxNumMeteringAreas() == 0) {
						if (MyDebug.LOG)
							Log.d(TAG, "metering areas not supported");
					} else {
						parameters.setMeteringAreas(areas);
					}

					try {
						if (MyDebug.LOG)
							Log.d(TAG, "set focus areas parameters");
						mCamera.setParameters(parameters);
						if (MyDebug.LOG)
							Log.d(TAG, "done");
					} catch (RuntimeException e) {
						// just in case something has gone wrong
						if (MyDebug.LOG)
							Log.d(TAG, "failed to set parameters for focus area");
						e.printStackTrace();
					}
				} else if (parameters.getMaxNumMeteringAreas() != 0) {
					if (MyDebug.LOG)
						Log.d(TAG, "set metering area");
					// don't set has_focus_area in this mode
					ArrayList<Camera.Area> areas = getAreas(event.getX(), event.getY());
					parameters.setMeteringAreas(areas);

					try {
						mCamera.setParameters(parameters);
					} catch (RuntimeException e) {
						// just in case something has gone wrong
						if (MyDebug.LOG)
							Log.d(TAG, "failed to set parameters for focus area");
						e.printStackTrace();
					}
				}
			}
		}

		tryAutoFocus(false, true);
		return true;
	}

	public boolean isTakingPhotoOrOnTimer() {
		// return this.is_taking_photo;
		return this.phase == PHASE_TAKING_PHOTO || this.phase == PHASE_TIMER;
	}

	@Override
	protected void onMeasure(int widthSpec, int heightSpec) {
		if (!this.has_aspect_ratio) {
			super.onMeasure(widthSpec, heightSpec);
			return;
		}
		int previewWidth = MeasureSpec.getSize(widthSpec);
		int previewHeight = MeasureSpec.getSize(heightSpec);

		// Get the padding of the border background.
		int hPadding = getPaddingLeft() + getPaddingRight();
		int vPadding = getPaddingTop() + getPaddingBottom();

		// Resize the preview frame with correct aspect ratio.
		previewWidth -= hPadding;
		previewHeight -= vPadding;

		boolean widthLonger = previewWidth > previewHeight;
		int longSide = (widthLonger ? previewWidth : previewHeight);
		int shortSide = (widthLonger ? previewHeight : previewWidth);
		if (longSide > shortSide * aspect_ratio) {
			longSide = (int) ((double) shortSide * aspect_ratio);
		} else {
			shortSide = (int) ((double) longSide / aspect_ratio);
		}
		if (widthLonger) {
			previewWidth = longSide;
			previewHeight = shortSide;
		} else {
			previewWidth = shortSide;
			previewHeight = longSide;
		}

		// Add the padding of the border.
		previewWidth += hPadding;
		previewHeight += vPadding;

		// Ask children to follow the new preview dimension.
		super.onMeasure(MeasureSpec.makeMeasureSpec(previewWidth, MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(previewHeight, MeasureSpec.EXACTLY));
	}

	private int ui_rotation = 0;
	private int[] gui_location = new int[2];
	private Paint p = new Paint();
	private Rect text_bounds = new Rect();

	@Override
	public void onDraw(Canvas canvas) {
		if (this.app_is_paused) {
			return;
		}

		VideoRecordActivity main_activity = (VideoRecordActivity) this.getContext();
		final float scale = getResources().getDisplayMetrics().density;

		canvas.save();
		canvas.rotate(ui_rotation, canvas.getWidth() / 2, canvas.getHeight() / 2);

		int text_y = (int) (20 * scale + 0.5f); // convert dps to pixels
		// fine tuning to adjust placement of text with respect to the GUI, depending on orientation
		int text_base_y = 0;
		if (ui_rotation == 0) {
			text_base_y = canvas.getHeight() - (int) (0.5 * text_y);
		} else if (ui_rotation == 180) {
			text_base_y = canvas.getHeight() - (int) (2.5 * text_y);
		} else if (ui_rotation == 90 || ui_rotation == 270) {
			// text_base_y = canvas.getHeight() + (int)(0.5*text_y);
			Button view = (Button) main_activity.findViewById(R.id.take_video_btn);
			// align with "top" of the take_photo button, but remember to take the rotation into account!
			view.getLocationOnScreen(gui_location);
			int view_left = gui_location[0];
			this.getLocationOnScreen(gui_location);
			int this_left = gui_location[0];
			int diff_x = view_left - (this_left + canvas.getWidth() / 2);
			/*
			 * if( MyDebug.LOG ) { Log.d(TAG, "view left: " + view_left); Log.d(TAG, "this left: " + this_left); Log.d(TAG, "canvas is " + canvas.getWidth() + " x " +
			 * canvas.getHeight()); }
			 */
			int max_x = canvas.getWidth();
			if (ui_rotation == 90) {
				// so we don't interfere with the top bar info (time, etc)
				max_x -= (int) (1.5 * text_y);
			}
			if (canvas.getWidth() / 2 + diff_x > max_x) {
				// in case goes off the size of the canvas, for "black bar" cases (when preview aspect ratio != screen aspect ratio)
				diff_x = max_x - canvas.getWidth() / 2;
			}
			text_base_y = canvas.getHeight() / 2 + diff_x - (int) (0.5 * text_y);
		}

		if (mCamera != null && this.phase != PHASE_PREVIEW_PAUSED) {
			if (this.video_recorder != null && video_start_time_set) {
				long video_time = (System.currentTimeMillis() - video_start_time);
				// int ms = (int)(video_time % 1000);
				video_time /= 1000;
				int secs = (int) (video_time % 60);
				video_time /= 60;
				int mins = (int) (video_time % 60);
				video_time /= 60;
				long hours = video_time;
				// String time_s = hours + ":" + String.format("%02d", mins) + ":" + String.format("%02d", secs) + ":" + String.format("%03d", ms);
				String time_s = hours + ":" + String.format("%02d", mins) + ":" + String.format("%02d", secs);

				p.setTextSize(14 * scale + 0.5f); // convert dps to pixels
				p.setTextAlign(Paint.Align.CENTER);
				// int pixels_offset_y = (int) (164 * scale + 0.5f); // convert dps to pixels
				// drawTextWithBackground(canvas, p, "" + time_s, Color.RED, Color.BLACK, canvas.getWidth() / 2, canvas.getHeight() - pixels_offset_y);
				int pixels_offset_y = 3 * text_y; // avoid overwriting the zoom label
				drawTextWithBackground(canvas, p, "" + time_s, Color.RED, Color.BLACK, canvas.getWidth() / 2, text_base_y - pixels_offset_y);
			}
		} else if (mCamera == null) {
			p.setColor(Color.WHITE);
			p.setTextSize(14 * scale + 0.5f); // convert dps to pixels
			p.setTextAlign(Paint.Align.CENTER);
			int pixels_offset = (int) (20 * scale + 0.5f); // convert dps to pixels
			canvas.drawText(getResources().getString(R.string.failed_to_open_camera_1), canvas.getWidth() / 2, canvas.getHeight() / 2, p);
			canvas.drawText(getResources().getString(R.string.failed_to_open_camera_2), canvas.getWidth() / 2,
					canvas.getHeight() / 2 + pixels_offset, p);
			canvas.drawText(getResources().getString(R.string.failed_to_open_camera_3), canvas.getWidth() / 2, canvas.getHeight() / 2 + 2
					* pixels_offset, p);
			// canvas.drawRect(0.0f, 0.0f, 100.0f, 100.0f, p);
			// canvas.drawRGB(255, 0, 0);
			// canvas.drawRect(0.0f, 0.0f, canvas.getWidth(), canvas.getHeight(), p);
		}
		canvas.restore();

		if (this.focus_success != FOCUS_DONE) {
			int size = (int) (50 * scale + 0.5f); // convert dps to pixels
			if (this.focus_success == FOCUS_SUCCESS)
				p.setColor(Color.GREEN);
			else if (this.focus_success == FOCUS_FAILED)
				p.setColor(Color.RED);
			else
				p.setColor(Color.WHITE);
			p.setStyle(Paint.Style.STROKE);
			int pos_x = 0;
			int pos_y = 0;
			if (has_focus_area) {
				pos_x = focus_screen_x;
				pos_y = focus_screen_y;
			} else {
				pos_x = canvas.getWidth() / 2;
				pos_y = canvas.getHeight() / 2;
			}
			canvas.drawRect(pos_x - size, pos_y - size, pos_x + size, pos_y + size, p);
			if (focus_complete_time != -1 && System.currentTimeMillis() > focus_complete_time + 1000) {
				focus_success = FOCUS_DONE;
			}
			p.setStyle(Paint.Style.FILL); // reset
		}
	}

	private void drawTextWithBackground(Canvas canvas, Paint paint, String text, int foreground, int background, int location_x, int location_y) {
		final float scale = getResources().getDisplayMetrics().density;
		p.setStyle(Paint.Style.FILL);
		paint.setColor(background);
		paint.setAlpha(127);
		paint.getTextBounds(text, 0, text.length(), text_bounds);
		final int padding = (int) (2 * scale + 0.5f); // convert dps to pixels
		if (paint.getTextAlign() == Paint.Align.RIGHT || paint.getTextAlign() == Paint.Align.CENTER) {
			float width = paint.measureText(text); // n.b., need to use measureText rather than getTextBounds here
			/*
			 * if( MyDebug.LOG ) Log.d(TAG, "width: " + width);
			 */
			if (paint.getTextAlign() == Paint.Align.CENTER)
				width /= 2.0f;
			text_bounds.left -= width;
			text_bounds.right -= width;
		}
		/*
		 * if( MyDebug.LOG ) Log.d(TAG, "text_bounds left-right: " + text_bounds.left + " , " + text_bounds.right);
		 */
		text_bounds.left += location_x - padding;
		text_bounds.top += location_y - padding;
		text_bounds.right += location_x + padding;
		text_bounds.bottom += location_y + padding;
		canvas.drawRect(text_bounds, paint);
		paint.setColor(foreground);
		canvas.drawText(text, location_x, location_y, paint);
	}

	public void onResume() {
		app_is_paused = false;
	}

	private void openCamera() {
		// need to init everything now, in case we don't open the camera (but these may already be initialised from an earlier call - e.g., if we are now switching to another
		// camera)
		has_focus_area = false;
		focus_success = FOCUS_DONE;
		successfully_focused = false;
		sizes = null;
		video_quality = null;
		current_video_quality = -1;
		supported_focus_values = null;
		current_focus_index = -1;

		showGUI(true);

		if (!has_surface) {
			if (MyDebug.LOG) {
				Log.d(TAG, "preview surface not yet available");
			}
			return;
		}

		if (app_is_paused) {
			if (MyDebug.LOG) {
				Log.d(TAG, "don't open camera as app is paused");
			}
			return;
		}

		try {
			mCamera = Camera.open(mCameraId);
		} catch (RuntimeException e) {
			if (MyDebug.LOG)
				Log.e(TAG, "Failed to open camera: " + e.getMessage());
			e.printStackTrace();
			mCamera = null;
		}

		if (mCamera != null) {
			Activity activity = (Activity) this.getContext();
			this.setCameraDisplayOrientation(activity);
			new OrientationEventListener(activity) {
				@Override
				public void onOrientationChanged(int orientation) {
					MyPreView.this.onOrientationChanged(orientation);
				}
			}.enable();

			if (MyDebug.LOG)
				Log.d(TAG, "call setPreviewDisplay");
			try {
				mCamera.setPreviewDisplay(mHolder);
			} catch (IOException e) {
				if (MyDebug.LOG)
					Log.e(TAG, "Failed to set preview display: " + e.getMessage());
				e.printStackTrace();
			}

			View switchCameraButton = (View) activity.findViewById(R.id.switch_camera_btn);
			switchCameraButton.setVisibility(Camera.getNumberOfCameras() > 1 ? View.VISIBLE : View.GONE);

			setupCamera();
		}
	}

	private void showGUI(final boolean show) {
		final Activity activity = (Activity) this.getContext();
		activity.runOnUiThread(new Runnable() {
			public void run() {
				final int visibility = show ? View.VISIBLE : View.GONE;
				View switchCameraButton = (View) activity.findViewById(R.id.switch_camera_btn);
				if (Camera.getNumberOfCameras() > 1) {
					switchCameraButton.setVisibility(visibility);
				}
			}
		});
	}

	/*
	 * Should only be called after camera first opened, or after preview is paused.
	 */
	void setupCamera() {
		if (MyDebug.LOG)
			Log.d(TAG, "setupCamera()");
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());

		Camera.Parameters parameters = mCamera.getParameters();

		sizes = parameters.getSupportedPictureSizes();

		// get available sizes
		initialiseVideoSizes(parameters);
		initialiseVideoQuality();

		current_video_quality = -1;

		// 获取保存在sharedPreferences中的video_quality，免费重复计算
		String video_quality_value_s = sharedPreferences.getString(getVideoQualityPreferenceKey(mCameraId), "");
		if (MyDebug.LOG) {
			Log.d(TAG, "video_quality_value: " + video_quality_value_s);
		}

		if (video_quality_value_s.length() > 0) {// 如果有缓存
			// parse the saved video quality, and make sure it is still valid
			// now find value in valid list
			for (int i = 0; i < video_quality.size() && current_video_quality == -1; i++) {
				if (video_quality.get(i).equals(video_quality_value_s)) {
					current_video_quality = i;
					if (MyDebug.LOG)
						Log.d(TAG, "set current_video_quality to: " + current_video_quality);
				}
			}
			if (current_video_quality == -1) {
				if (MyDebug.LOG)
					Log.e(TAG, "failed to find valid video_quality");
			}
		}

		if (current_video_quality == -1 && video_quality.size() > 0) {
			// 计算获取最接近480*320的video_quality
			current_video_quality = getExceptVideoQuality();
			if (MyDebug.LOG)
				Log.d(TAG, "set video_quality value to " + video_quality.get(current_video_quality));
		}
		if (current_video_quality != -1) {
			// now save, so it's available for PreferenceActivity
			SharedPreferences.Editor editor = sharedPreferences.edit();
			editor.putString(getVideoQualityPreferenceKey(mCameraId), video_quality.get(current_video_quality));
			editor.apply();
		}

		parameters.getPreviewFpsRange(current_fps_range);
		if (MyDebug.LOG)
			Log.d(TAG, "    current fps range: " + current_fps_range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] + " to "
					+ current_fps_range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);

		// update parameters
		mCamera.setParameters(parameters);

		// foucus mode 设置
		List<String> supported_focus_modes = parameters.getSupportedFocusModes(); // Android format
		current_focus_index = -1;
		if (supported_focus_modes != null && supported_focus_modes.size() > 1) {
			if (MyDebug.LOG)
				Log.d(TAG, "focus modes: " + supported_focus_modes);
			supported_focus_values = convertFocusModesToValues(supported_focus_modes); // convert to our format (also resorts)

			String focus_value = sharedPreferences.getString(getFocusPreferenceKey(mCameraId), "");
			if (focus_value.length() > 0) {
				if (MyDebug.LOG)
					Log.d(TAG, "found existing focus_value: " + focus_value);
				if (!updateFocus(focus_value, false, false, true)) { // don't need to save, as this is the value that's already saved
					if (MyDebug.LOG)
						Log.d(TAG, "focus value no longer supported!");
					updateFocus(0, false, true, true);
				}
			} else {
				if (MyDebug.LOG)
					Log.d(TAG, "found no existing focus_value");
				updateFocus(0, false, true, true);
			}
		} else {
			if (MyDebug.LOG)
				Log.d(TAG, "focus not supported");
			supported_focus_values = null;
		}

		// now switch to video if saved
		boolean saved_is_video = true;
		if (saved_is_video != this.is_video) {
			this.switchVideo(false, false);
		}

		// Must set preview size before starting camera preview
		// and must do it after setting photo vs video mode
		setPreviewSize(); // need to call this when we switch cameras, not just when we run for the first time
		// Must call startCameraPreview after checking if face detection is present - probably best to call it after setting all parameters that we want
		startCameraPreview();

		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				tryAutoFocus(true, false); // so we get the autofocus when starting up - we do this on a delay, as calling it immediately means the autofocus doesn't seem to work
				// properly sometimes (at least on Galaxy Nexus)
			}
		}, 500);
	}

	private void startCameraPreview() {
		long debug_time = 0;
		if (MyDebug.LOG) {
			Log.d(TAG, "startCameraPreview");
			debug_time = System.currentTimeMillis();
		}
		// if( camera != null && !is_taking_photo && !is_preview_started ) {
		if (mCamera != null && !is_preview_started) {
			if (MyDebug.LOG)
				Log.d(TAG, "starting the camera preview");
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				if (MyDebug.LOG)
					Log.d(TAG, "setRecordingHint: " + is_video);
				Camera.Parameters parameters = mCamera.getParameters();
				// Calling setParameters here with continuous video focus mode causes preview to not restart after taking a photo on Galaxy Nexus?! (fine on my Nexus 7).
				// The issue seems to specifically be with setParameters (i.e., the problem occurs even if we don't setRecordingHint).
				// In addition, I had a report of a bug on HTC Desire X, Android 4.0.4 where the saved video was corrupted.
				// This worked fine in 1.7, then not in 1.8 and 1.9, then was fixed again in 1.10
				// The only thing in common to 1.7->1.8 and 1.9-1.10, that seems relevant, was adding this code to setRecordingHint() and setParameters() (unclear which would have
				// been the problem),
				// so we should be very careful about enabling this code again!
				String focus_mode = parameters.getFocusMode();
				if (focus_mode != null && !focus_mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
					parameters.setRecordingHint(this.is_video);
					mCamera.setParameters(parameters);
				}
			}
			if (this.is_video) {
				matchPreviewFpsToVideo();
			}
			// else, we reset the preview fps to default in switchVideo
			mCamera.startPreview();
			this.is_preview_started = true;
			if (MyDebug.LOG) {
				Log.d(TAG, "time after starting camera preview: " + (System.currentTimeMillis() - debug_time));
			}

		}
		this.setPreviewPaused(false);
	}

	private void matchPreviewFpsToVideo() {
		if (MyDebug.LOG)
			Log.d(TAG, "matchPreviewFpsToVideo()");
		CamcorderProfile profile = getCamcorderProfile();
		Camera.Parameters parameters = mCamera.getParameters();

		List<int[]> fps_ranges = parameters.getSupportedPreviewFpsRange();
		int selected_min_fps = -1, selected_max_fps = -1, selected_diff = -1;
		for (int[] fps_range : fps_ranges) {
			if (MyDebug.LOG) {
				Log.d(TAG, "    supported fps range: " + fps_range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] + " to "
						+ fps_range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
			}
			int min_fps = fps_range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
			int max_fps = fps_range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
			if (min_fps <= profile.videoFrameRate * 1000 && max_fps >= profile.videoFrameRate * 1000) {
				int diff = max_fps - min_fps;
				if (selected_diff == -1 || diff < selected_diff) {
					selected_min_fps = min_fps;
					selected_max_fps = max_fps;
					selected_diff = diff;
				}
			}
		}
		if (selected_min_fps == -1) {
			selected_diff = -1;
			int selected_dist = -1;
			for (int[] fps_range : fps_ranges) {
				int min_fps = fps_range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX];
				int max_fps = fps_range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX];
				int diff = max_fps - min_fps;
				int dist = -1;
				if (max_fps < profile.videoFrameRate * 1000)
					dist = profile.videoFrameRate * 1000 - max_fps;
				else
					dist = min_fps - profile.videoFrameRate * 1000;
				if (MyDebug.LOG) {
					Log.d(TAG, "    supported fps range: " + min_fps + " to " + max_fps + " has dist " + dist + " and diff " + diff);
				}
				if (selected_dist == -1 || dist < selected_dist || (dist == selected_dist && diff < selected_diff)) {
					selected_min_fps = min_fps;
					selected_max_fps = max_fps;
					selected_dist = dist;
					selected_diff = diff;
				}
			}
			if (MyDebug.LOG)
				Log.d(TAG, "    can't find match for fps range, so choose closest: " + selected_min_fps + " to " + selected_max_fps);
			parameters.setPreviewFpsRange(selected_min_fps, selected_max_fps);
			mCamera.setParameters(parameters);
		} else {
			if (MyDebug.LOG) {
				Log.d(TAG, "    chosen fps range: " + selected_min_fps + " to " + selected_max_fps);
			}
			parameters.setPreviewFpsRange(selected_min_fps, selected_max_fps);
			mCamera.setParameters(parameters);
		}
	}

	private void setPreviewPaused(boolean paused) {
		if (MyDebug.LOG)
			Log.d(TAG, "setPreviewPaused: " + paused);
		if (paused) {
			this.phase = PHASE_PREVIEW_PAUSED;
		} else {
			this.phase = PHASE_NORMAL;
			showGUI(true);
		}
	}

	// /////////////////////////////PreView 设置////////////////////////////////

	private void setPreviewSize() {
		if (MyDebug.LOG)
			Log.d(TAG, "setPreviewSize()");
		// also now sets picture size
		if (mCamera == null) {
			return;
		}
		if (is_preview_started) {
			if (MyDebug.LOG)
				Log.d(TAG, "setPreviewSize() shouldn't be called when preview is running");
			throw new RuntimeException();
		}
		// first set picture size (for photo mode, must be done now so we can set the picture size from this; for video, doesn't really matter when we set it)
		Camera.Parameters parameters = mCamera.getParameters();
		if (this.is_video) {
			// In theory, the picture size shouldn't matter in video mode, but the stock Android camera sets a picture size
			// which is the largest that matches the video's aspect ratio.
			// This seems necessary to work around an aspect ratio bug introduced in Android 4.4.3 (on Nexus 7 at least): http://code.google.com/p/android/issues/detail?id=70830
			// which results in distorted aspect ratio on preview and recorded video!
			CamcorderProfile profile = getCamcorderProfile();
			if (MyDebug.LOG)
				Log.d(TAG, "video size: " + profile.videoFrameWidth + " x " + profile.videoFrameHeight);
			double targetRatio = ((double) profile.videoFrameWidth) / (double) profile.videoFrameHeight;
			Camera.Size best_size = getOptimalVideoPictureSize(sizes, targetRatio);
			parameters.setPictureSize(best_size.width, best_size.height);
			if (MyDebug.LOG)
				Log.d(TAG, "set picture size for video: " + parameters.getPictureSize().width + ", " + parameters.getPictureSize().height);
		} else {
			// Toast.makeText(getContext(), "启动视频录制失败", Toast.LENGTH_SHORT).show();
			Toast.makeText(getContext(), getContext().getString(R.string.start_failed), Toast.LENGTH_SHORT).show();

			// if (current_size_index != -1) {
			// Camera.Size current_size = sizes.get(current_size_index);
			// parameters.setPictureSize(current_size.width, current_size.height);
			// if (MyDebug.LOG)
			// Log.d(TAG, "set picture size for photo: " + parameters.getPictureSize().width + ", " + parameters.getPictureSize().height);
			// }
		}
		// need to set parameteres, so that picture size is set
		mCamera.setParameters(parameters);
		parameters = mCamera.getParameters();
		// set optimal preview size
		if (MyDebug.LOG)
			Log.d(TAG, "current preview size: " + parameters.getPreviewSize().width + ", " + parameters.getPreviewSize().height);
		supported_preview_sizes = parameters.getSupportedPreviewSizes();
		if (supported_preview_sizes.size() > 0) {
			/*
			 * Camera.Size best_size = supported_preview_sizes.get(0); for(Camera.Size size : supported_preview_sizes) { if( MyDebug.LOG ) Log.d(TAG, "    supported preview size: "
			 * + size.width + ", " + size.height); if( size.width*size.height > best_size.width*best_size.height ) { best_size = size; } }
			 */
			Camera.Size best_size = getOptimalPreviewSize(supported_preview_sizes);
			parameters.setPreviewSize(best_size.width, best_size.height);
			if (MyDebug.LOG)
				Log.d(TAG, "new preview size: " + parameters.getPreviewSize().width + ", " + parameters.getPreviewSize().height);
			this.setAspectRatio(((double) parameters.getPreviewSize().width) / (double) parameters.getPreviewSize().height);

			mCamera.setParameters(parameters);
		}
	}

	private void setAspectRatio(double ratio) {
		if (ratio <= 0.0)
			throw new IllegalArgumentException();
		has_aspect_ratio = true;
		if (aspect_ratio != ratio) {
			aspect_ratio = ratio;
			if (MyDebug.LOG)
				Log.d(TAG, "new aspect ratio: " + aspect_ratio);
			requestLayout();
		}
	}

	public CamcorderProfile getCamcorderProfile() {
		CamcorderProfile profile = null;
		if (current_video_quality != -1) {
			profile = getCamcorderProfile(video_quality.get(current_video_quality));
		} else {
			profile = CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_HIGH);
		}
		return profile;
	}

	private CamcorderProfile getCamcorderProfile(String quality) {
		if (MyDebug.LOG)
			Log.e(TAG, "getCamcorderProfile(): " + quality);
		CamcorderProfile camcorder_profile = CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_HIGH); // default
		try {
			String profile_string = quality;
			int index = profile_string.indexOf('_');
			if (index != -1) {
				profile_string = quality.substring(0, index);
				if (MyDebug.LOG)
					Log.e(TAG, "    profile_string: " + profile_string);
			}
			int profile = Integer.parseInt(profile_string);
			camcorder_profile = CamcorderProfile.get(mCameraId, profile);
			if (index != -1 && index + 1 < quality.length()) {
				String override_string = quality.substring(index + 1);
				if (MyDebug.LOG)
					Log.e(TAG, "    override_string: " + override_string);
				if (override_string.charAt(0) == 'r' && override_string.length() >= 4) {
					index = override_string.indexOf('x');
					if (index == -1) {
						if (MyDebug.LOG)
							Log.d(TAG, "override_string invalid format, can't find x");
					} else {
						String resolution_w_s = override_string.substring(1, index); // skip first 'r'
						String resolution_h_s = override_string.substring(index + 1);
						if (MyDebug.LOG) {
							Log.d(TAG, "resolution_w_s: " + resolution_w_s);
							Log.d(TAG, "resolution_h_s: " + resolution_h_s);
						}
						// copy to local variable first, so that if we fail to parse height, we don't set the width either
						int resolution_w = Integer.parseInt(resolution_w_s);
						int resolution_h = Integer.parseInt(resolution_h_s);
						camcorder_profile.videoFrameWidth = resolution_w;
						camcorder_profile.videoFrameHeight = resolution_h;
					}
				} else {
					if (MyDebug.LOG)
						Log.d(TAG, "unknown override_string initial code, or otherwise invalid format");
				}
			}
		} catch (NumberFormatException e) {
			if (MyDebug.LOG)
				Log.e(TAG, "failed to parse video quality: " + quality);
			e.printStackTrace();
		}
		return camcorder_profile;
	}

	public Camera.Size getOptimalVideoPictureSize(List<Camera.Size> sizes, double targetRatio) {
		if (MyDebug.LOG)
			Log.d(TAG, "getOptimalVideoPictureSize()");
		final double ASPECT_TOLERANCE = 0.05;
		if (sizes == null)
			return null;
		Camera.Size optimalSize = null;
		// Try to find largest size that matches aspect ratio
		for (Camera.Size size : sizes) {
			if (MyDebug.LOG)
				Log.d(TAG, "    supported preview size: " + size.width + ", " + size.height);
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (optimalSize == null || size.width > optimalSize.width) {
				optimalSize = size;
			}
		}
		if (optimalSize == null) {
			// can't find match for aspect ratio, so find closest one
			if (MyDebug.LOG)
				Log.d(TAG, "no picture size matches the aspect ratio");
			optimalSize = getClosestSize(sizes, targetRatio);
		}
		if (MyDebug.LOG) {
			Log.d(TAG, "chose optimalSize: " + optimalSize.width + " x " + optimalSize.height);
			Log.d(TAG, "optimalSize ratio: " + ((double) optimalSize.width / optimalSize.height));
		}
		return optimalSize;
	}

	public Camera.Size getClosestSize(List<Camera.Size> sizes, double targetRatio) {
		if (MyDebug.LOG)
			Log.d(TAG, "getClosestSize()");
		Camera.Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;
		for (Camera.Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(ratio - targetRatio);
			}
		}
		return optimalSize;
	}

	public Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes) {
		if (MyDebug.LOG)
			Log.d(TAG, "getOptimalPreviewSize()");
		final double ASPECT_TOLERANCE = 0.05;
		if (sizes == null)
			return null;
		Camera.Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;
		Point display_size = new Point();
		Activity activity = (Activity) this.getContext();
		{
			Display display = activity.getWindowManager().getDefaultDisplay();

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
				display.getSize(display_size);
			} else {
				display_size.set(display.getWidth(), display.getHeight());
			}
			if (MyDebug.LOG)
				Log.d(TAG, "display_size: " + display_size.x + " x " + display_size.y);
		}
		double targetRatio = getTargetRatioForPreview(display_size);
		int targetHeight = Math.min(display_size.y, display_size.x);
		if (targetHeight <= 0) {
			targetHeight = display_size.y;
		}
		// Try to find an size match aspect ratio and size
		for (Camera.Size size : sizes) {
			if (MyDebug.LOG)
				Log.d(TAG, "    supported preview size: " + size.width + ", " + size.height);
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}
		if (optimalSize == null) {
			// can't find match for aspect ratio, so find closest one
			if (MyDebug.LOG)
				Log.d(TAG, "no preview size matches the aspect ratio");
			optimalSize = getClosestSize(sizes, targetRatio);
		}
		if (MyDebug.LOG) {
			Log.d(TAG, "chose optimalSize: " + optimalSize.width + " x " + optimalSize.height);
			Log.d(TAG, "optimalSize ratio: " + ((double) optimalSize.width / optimalSize.height));
		}
		return optimalSize;
	}

	public double getTargetRatioForPreview(Point display_size) {
		double targetRatio = 0.0f;
		Activity activity = (Activity) this.getContext();
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
		String preview_size = sharedPreferences.getString("preference_preview_size", "preference_preview_size_wysiwyg");
		// should always use wysiwig for video mode, otherwise we get incorrect aspect ratio shown when recording video (at least on Galaxy Nexus, e.g., at 640x480)
		// also not using wysiwyg mode with video caused corruption on Samsung cameras (tested with Samsung S3, Android 4.3, front camera, infinity focus)
		if (preview_size.equals("preference_preview_size_wysiwyg") || this.is_video) {
			if (this.is_video) {
				if (MyDebug.LOG)
					Log.d(TAG, "set preview aspect ratio from video size (wysiwyg)");
				CamcorderProfile profile = getCamcorderProfile();
				if (MyDebug.LOG)
					Log.d(TAG, "video size: " + profile.videoFrameWidth + " x " + profile.videoFrameHeight);
				targetRatio = ((double) profile.videoFrameWidth) / (double) profile.videoFrameHeight;
			} else {
				if (MyDebug.LOG)
					Log.d(TAG, "set preview aspect ratio from photo size (wysiwyg)");
				Camera.Parameters parameters = mCamera.getParameters();
				Camera.Size picture_size = parameters.getPictureSize();
				if (MyDebug.LOG)
					Log.d(TAG, "picture_size: " + picture_size.width + " x " + picture_size.height);
				targetRatio = ((double) picture_size.width) / (double) picture_size.height;
			}
		} else {
			if (MyDebug.LOG)
				Log.d(TAG, "set preview aspect ratio from display size");
			// base target ratio from display size - means preview will fill the device's display as much as possible
			// but if the preview's aspect ratio differs from the actual photo/video size, the preview will show a cropped version of what is actually taken
			targetRatio = ((double) display_size.x) / (double) display_size.y;
		}
		if (MyDebug.LOG)
			Log.d(TAG, "targetRatio: " + targetRatio);
		return targetRatio;
	}

	void pausePreview() {
		if (MyDebug.LOG)
			Log.d(TAG, "pausePreview()");
		this.setPreviewPaused(false);
		mCamera.stopPreview();
		this.phase = PHASE_NORMAL;
		this.is_preview_started = false;
		showGUI(true);
	}

	// /////////////////////////////切换到视频准备/////////////////////////////////////
	private MediaRecorder video_recorder = null;
	private boolean video_start_time_set = false;
	private long video_start_time = 0;
	private String video_name = null;
	private Timer restartVideoTimer = new Timer();
	private TimerTask restartVideoTimerTask = null;

	private final int PHASE_NORMAL = 0;
	private final int PHASE_TIMER = 1;
	private final int PHASE_TAKING_PHOTO = 2;
	private final int PHASE_PREVIEW_PAUSED = 3; // the paused state after taking a photo
	private int phase = PHASE_NORMAL;

	void switchVideo(boolean save, boolean update_preview_size) {
		if (MyDebug.LOG)
			Log.d(TAG, "switchVideo()");
		if (this.mCamera == null) {
			return;
		}
		boolean old_is_video = is_video;
		if (this.is_video) {
			if (video_recorder != null) {
				stopVideo();
			}
			this.is_video = false;
		} else {
			// if( is_taking_photo_on_timer ) {
			if (this.phase == PHASE_TAKING_PHOTO) {
				// wait until photo taken
				if (MyDebug.LOG)
					Log.d(TAG, "wait until photo taken");
			} else {
				this.is_video = true;
			}

			if (this.is_video) {
				// showPhotoVideoToast();
			}
		}

		if (is_video != old_is_video) {
			updateFocusForVideo(true);

			if (update_preview_size) {
				if (this.is_preview_started) {
					mCamera.stopPreview();
					this.is_preview_started = false;
				}
				setPreviewSize();
				if (!is_video) {
					// if is_video is true, we set the preview fps range in startCameraPreview()
					if (MyDebug.LOG)
						Log.d(TAG, "    reset preview to current fps range: " + current_fps_range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] + " to "
								+ current_fps_range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
					Camera.Parameters parameters = mCamera.getParameters();
					parameters.setPreviewFpsRange(current_fps_range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
							current_fps_range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
					mCamera.setParameters(parameters);
				}
				// always start the camera preview, even if it was previously paused
				this.startCameraPreview();
			}
		}
	}

	private void stopVideo() {

		VideoRecordActivity activity = (VideoRecordActivity) this.getContext();

		if (restartVideoTimerTask != null) {
			restartVideoTimerTask.cancel();
		}
		if (video_recorder != null) { // check again, just to be safe
			/*
			 * is_taking_photo = false; is_taking_photo_on_timer = false;
			 */
			this.phase = PHASE_NORMAL;
			long end_time = 0;
			try {
				video_recorder.stop();
				end_time = System.currentTimeMillis();
			} catch (RuntimeException e) {
				// stop() can throw a RuntimeException if stop is called too soon after start - we have no way to detect this, so have to catch it
				if (MyDebug.LOG)
					Log.d(TAG, "runtime exception when stopping video");
			}
			video_recorder.reset();
			video_recorder.release();
			video_recorder = null;
			reconnectCamera(false);
			if (video_name != null) {
				File file = new File(video_name);
				if (file != null) {
					// need to scan when finished, so we update for the completed file
					// TODO 视频录制成功
					activity.broadcastFile(file, false, true, end_time - video_start_time);
				}
				video_name = null;
			}
		}
	}

	private void reconnectCamera(boolean quiet) {
		if (mCamera != null) { // just to be safe
			try {
				mCamera.reconnect();
				this.startCameraPreview();
			} catch (IOException e) {
				if (MyDebug.LOG)
					Log.e(TAG, "failed to reconnect to camera");
				e.printStackTrace();
				closeCamera();
			}
			try {
				tryAutoFocus(false, false);
			} catch (RuntimeException e) {
				if (MyDebug.LOG)
					Log.e(TAG, "tryAutoFocus() threw exception: " + e.getMessage());
				e.printStackTrace();
				// this happens on Nexus 7 if trying to record video at bitrate 50Mbits or higher - it's fair enough that it fails, but we need to recover without a crash!
				// not safe to call closeCamera, as any call to getParameters may cause a RuntimeException
				this.is_preview_started = false;
				mCamera.release();
				mCamera = null;
				if (!quiet) {
					// TODO 错误提示
				}
				openCamera();
			}
		}
	}

	private void closeCamera() {
		if (MyDebug.LOG) {
			Log.d(TAG, "closeCamera()");
		}
		has_focus_area = false;
		focus_success = FOCUS_DONE;
		successfully_focused = false;

		if (mCamera != null) {
			if (video_recorder != null) {
				stopVideo();
			}
			if (this.is_video) {
				// make sure we're into continuous video mode for closing
				// workaround for bug on Samsung Galaxy S5 with UHD, where if the user switches to another (non-continuous-video) focus mode, then goes to Settings, then returns
				// and records video, the preview freezes and the video is corrupted
				// so to be safe, we always reset to continuous video mode
				this.updateFocusForVideo(false);
			}
			// camera.setPreviewCallback(null);
			pausePreview();
			mCamera.release();
			mCamera = null;
		}
	}

	private void updateFocusForVideo(boolean auto_focus) {
		if (MyDebug.LOG)
			Log.d(TAG, "updateFocusForVideo()");
		if (this.supported_focus_values != null && mCamera != null) {
			Camera.Parameters parameters = mCamera.getParameters();
			String current_focus_mode = parameters.getFocusMode();
			boolean focus_is_video = current_focus_mode != null && current_focus_mode.equals(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
			if (MyDebug.LOG) {
				Log.d(TAG, "current_focus_mode: " + current_focus_mode);
				Log.d(TAG, "focus_is_video: " + focus_is_video + " , is_video: " + is_video);
			}
			if (focus_is_video != is_video) {
				if (MyDebug.LOG)
					Log.d(TAG, "need to change focus mode");
				updateFocus(is_video ? "focus_mode_continuous_video" : "focus_mode_auto", true, true, auto_focus);
				if (MyDebug.LOG) {
					parameters = mCamera.getParameters();
					current_focus_mode = parameters.getFocusMode();
					Log.d(TAG, "new focus mode: " + current_focus_mode);
				}
			}
		}
	}

	// /////////////////视频尺寸的初始化////////////////////////////////////
	private void initialiseVideoSizes(Camera.Parameters parameters) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {// 3.0之后才有此方法
			video_sizes = parameters.getSupportedVideoSizes();
		}

		if (video_sizes == null) {
			// if null, we should use the preview sizes - see http://stackoverflow.com/questions/14263521/android-getsupportedvideosizes-allways-returns-null
			if (MyDebug.LOG)
				Log.d(TAG, "take video_sizes from preview sizes");
			video_sizes = parameters.getSupportedPreviewSizes();
		}
		this.sortVideoSizes();
		for (Camera.Size size : video_sizes) {
			if (MyDebug.LOG)
				Log.d(TAG, "    supported video size: " + size.width + ", " + size.height);
		}
	}

	private void initialiseVideoQuality() {
		SparseArray<Pair<Integer, Integer>> profiles = new SparseArray<Pair<Integer, Integer>>();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			initialiseVideoQualityAboveHONEYCOMB(profiles);
		} else {
			initialiseVideoQualityBelowHONEYCOMB(profiles);
		}

		initialiseVideoQualityFromProfiles(profiles);
	}

	@TargetApi(11)
	private void initialiseVideoQualityAboveHONEYCOMB(SparseArray<Pair<Integer, Integer>> profiles) {
		if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_HIGH)) {
			CamcorderProfile profile = CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_HIGH);
			profiles.put(CamcorderProfile.QUALITY_HIGH, new Pair<Integer, Integer>(profile.videoFrameWidth, profile.videoFrameHeight));
		}
		if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_1080P)) {
			CamcorderProfile profile = CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_1080P);
			profiles.put(CamcorderProfile.QUALITY_1080P, new Pair<Integer, Integer>(profile.videoFrameWidth, profile.videoFrameHeight));
		}
		if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_720P)) {
			CamcorderProfile profile = CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_720P);
			profiles.put(CamcorderProfile.QUALITY_720P, new Pair<Integer, Integer>(profile.videoFrameWidth, profile.videoFrameHeight));
		}
		if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_480P)) {
			CamcorderProfile profile = CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_480P);
			profiles.put(CamcorderProfile.QUALITY_480P, new Pair<Integer, Integer>(profile.videoFrameWidth, profile.videoFrameHeight));
		}
		if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_CIF)) {
			CamcorderProfile profile = CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_CIF);
			profiles.put(CamcorderProfile.QUALITY_CIF, new Pair<Integer, Integer>(profile.videoFrameWidth, profile.videoFrameHeight));
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {// 4.0.3之后才有此方法
			if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_QVGA)) {
				CamcorderProfile profile = CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_QVGA);
				profiles.put(CamcorderProfile.QUALITY_QVGA, new Pair<Integer, Integer>(profile.videoFrameWidth, profile.videoFrameHeight));
			}
		}
		if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_QCIF)) {
			CamcorderProfile profile = CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_QCIF);
			profiles.put(CamcorderProfile.QUALITY_QCIF, new Pair<Integer, Integer>(profile.videoFrameWidth, profile.videoFrameHeight));
		}
		if (CamcorderProfile.hasProfile(mCameraId, CamcorderProfile.QUALITY_LOW)) {
			CamcorderProfile profile = CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_LOW);
			profiles.put(CamcorderProfile.QUALITY_LOW, new Pair<Integer, Integer>(profile.videoFrameWidth, profile.videoFrameHeight));
		}
	}

	private void initialiseVideoQualityBelowHONEYCOMB(SparseArray<Pair<Integer, Integer>> profiles) {
		CamcorderProfile profile1 = CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_HIGH);
		if (profile1 != null) {
			profiles.put(CamcorderProfile.QUALITY_HIGH, new Pair<Integer, Integer>(profile1.videoFrameWidth, profile1.videoFrameHeight));
		}

		CamcorderProfile profile2 = CamcorderProfile.get(mCameraId, CamcorderProfile.QUALITY_LOW);
		if (profile2 != null) {
			profiles.put(CamcorderProfile.QUALITY_LOW, new Pair<Integer, Integer>(profile2.videoFrameWidth, profile2.videoFrameHeight));
		}
	}

	public void initialiseVideoQualityFromProfiles(SparseArray<Pair<Integer, Integer>> profiles) {
		video_quality = new Vector<String>();
		boolean done_video_size[] = null;
		if (video_sizes != null) {
			done_video_size = new boolean[video_sizes.size()];
			for (int i = 0; i < video_sizes.size(); i++)
				done_video_size[i] = false;
		}

		// video_quality_pair 存储的值，根据video_quality在list中的position对应，存储size
		video_quality_pair = new SparseArray<Pair<Integer, Integer>>();

		if (profiles.get(CamcorderProfile.QUALITY_HIGH) != null) {
			Pair<Integer, Integer> pair = profiles.get(CamcorderProfile.QUALITY_HIGH);
			addVideoResolutions(done_video_size, CamcorderProfile.QUALITY_HIGH, pair.first, pair.second);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if (profiles.get(CamcorderProfile.QUALITY_1080P) != null) {
				Pair<Integer, Integer> pair = profiles.get(CamcorderProfile.QUALITY_1080P);
				addVideoResolutions(done_video_size, CamcorderProfile.QUALITY_1080P, pair.first, pair.second);
			}
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if (profiles.get(CamcorderProfile.QUALITY_720P) != null) {
				Pair<Integer, Integer> pair = profiles.get(CamcorderProfile.QUALITY_720P);
				addVideoResolutions(done_video_size, CamcorderProfile.QUALITY_720P, pair.first, pair.second);
			}
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if (profiles.get(CamcorderProfile.QUALITY_480P) != null) {
				Pair<Integer, Integer> pair = profiles.get(CamcorderProfile.QUALITY_480P);
				addVideoResolutions(done_video_size, CamcorderProfile.QUALITY_480P, pair.first, pair.second);
			}
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if (profiles.get(CamcorderProfile.QUALITY_CIF) != null) {
				Pair<Integer, Integer> pair = profiles.get(CamcorderProfile.QUALITY_CIF);
				addVideoResolutions(done_video_size, CamcorderProfile.QUALITY_CIF, pair.first, pair.second);
			}
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {// 4.0.3之后才有此方法
			if (profiles.get(CamcorderProfile.QUALITY_QVGA) != null) {
				Pair<Integer, Integer> pair = profiles.get(CamcorderProfile.QUALITY_QVGA);
				addVideoResolutions(done_video_size, CamcorderProfile.QUALITY_QVGA, pair.first, pair.second);
			}
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			if (profiles.get(CamcorderProfile.QUALITY_QCIF) != null) {
				Pair<Integer, Integer> pair = profiles.get(CamcorderProfile.QUALITY_QCIF);
				addVideoResolutions(done_video_size, CamcorderProfile.QUALITY_QCIF, pair.first, pair.second);
			}
		}

		if (profiles.get(CamcorderProfile.QUALITY_LOW) != null) {
			Pair<Integer, Integer> pair = profiles.get(CamcorderProfile.QUALITY_LOW);
			addVideoResolutions(done_video_size, CamcorderProfile.QUALITY_LOW, pair.first, pair.second);
		}
		if (MyDebug.LOG) {
			for (int i = 0; i < video_quality.size(); i++) {
				Log.d(TAG, "supported video quality: " + video_quality.get(i));
			}
		}
	}

	private void addVideoResolutions(boolean done_video_size[], int base_profile, int min_resolution_w, int min_resolution_h) {
		if (video_sizes == null) {
			return;
		}
		for (int i = 0; i < video_sizes.size(); i++) {
			if (done_video_size[i])
				continue;
			Camera.Size size = video_sizes.get(i);
			if (size.width == min_resolution_w && size.height == min_resolution_h) {
				String str = "" + base_profile;
				video_quality.add(str);
				video_quality_pair.put(video_quality.size() - 1, new Pair<Integer, Integer>(size.width, size.height));
				done_video_size[i] = true;
			} else if (base_profile == CamcorderProfile.QUALITY_LOW || size.width * size.height >= min_resolution_w * min_resolution_h) {
				String str = "" + base_profile + "_r" + size.width + "x" + size.height;
				video_quality.add(str);
				video_quality_pair.put(video_quality.size() - 1, new Pair<Integer, Integer>(size.width, size.height));
				done_video_size[i] = true;
			}
		}
	}

	/**
	 * 
	 * @return -1 代表一个错误的尺寸<br/>
	 */
	private int getExceptVideoQuality() {
		if (video_quality == null || video_quality_pair == null) {
			return -1;
		}
		// video_quality 是以从高到低的质量排序的，因为我所需要的480*320质量较低，所以倒序循环获取

		int minDifValue = Integer.MAX_VALUE;// 视频尺寸与期望尺寸的差值
		int minDifPosition = -1;
		for (int i = video_quality.size() - 1; i >= 0; i--) {
			Pair<Integer, Integer> pair = video_quality_pair.get(i);
			if (pair == null) {
				continue;
			}
			int difValue = Math.abs(pair.first * pair.second - EXPECT_WIDTH * EXPECT_HEIGHT);
			if (difValue < minDifValue) {
				minDifValue = difValue;
				minDifPosition = i;
			}
		}

		if (MyDebug.LOG) {
			if (minDifPosition != -1) {
				Log.d(TAG, " the most except video quality is :" + video_quality.get(minDifPosition));
			}
		}

		return minDifPosition;
	}

	private void sortVideoSizes() {
		Collections.sort(this.video_sizes, new Comparator<Camera.Size>() {
			public int compare(final Camera.Size a, final Camera.Size b) {
				return b.width * b.height - a.width * a.height;
			}
		});
	}

	// ///////////////////聚焦设置///////////////////////
	private List<String> convertFocusModesToValues(List<String> supported_focus_modes) {
		List<String> output_modes = new Vector<String>();
		if (supported_focus_modes != null) {
			// also resort as well as converting
			// first one will be the default choice
			if (supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
				output_modes.add("focus_mode_auto");
			}
			if (supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
				output_modes.add("focus_mode_infinity");
			}
			if (supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_MACRO)) {
				output_modes.add("focus_mode_macro");
			}
			if (supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
				output_modes.add("focus_mode_manual");
			}
			if (supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
				output_modes.add("focus_mode_fixed");
			}
			if (supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_EDOF)) {
				output_modes.add("focus_mode_edof");
			}
			if (supported_focus_modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
				output_modes.add("focus_mode_continuous_video");
			}
		}
		return output_modes;
	}

	private boolean updateFocus(String focus_value, boolean quiet, boolean save, boolean auto_focus) {
		if (this.supported_focus_values != null) {
			int new_focus_index = supported_focus_values.indexOf(focus_value);
			if (new_focus_index != -1) {
				updateFocus(new_focus_index, quiet, save, auto_focus);
				return true;
			}
		}
		return false;
	}

	private void updateFocus(int new_focus_index, boolean quiet, boolean save, boolean auto_focus) {
		// updates the Focus button, and Focus camera mode
		if (this.supported_focus_values != null && new_focus_index != current_focus_index) {
			// boolean initial = current_focus_index == -1;
			current_focus_index = new_focus_index;
			String focus_value = supported_focus_values.get(current_focus_index);
			setFocusValue(focus_value, auto_focus);
			if (save) {
				// now save
				SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putString(getFocusPreferenceKey(mCameraId), focus_value);
				editor.apply();
			}
		}
	}

	private void setFocusValue(String focus_value, boolean auto_focus) {
		if (mCamera == null) {
			if (MyDebug.LOG)
				Log.d(TAG, "null camera");
			return;
		}
		cancelAutoFocus();
		Camera.Parameters parameters = mCamera.getParameters();
		if (focus_value.equals("focus_mode_auto") || focus_value.equals("focus_mode_manual")) {
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		} else if (focus_value.equals("focus_mode_infinity")) {
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
		} else if (focus_value.equals("focus_mode_macro")) {
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
		} else if (focus_value.equals("focus_mode_fixed")) {
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
		} else if (focus_value.equals("focus_mode_edof")) {
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_EDOF);
		} else if (focus_value.equals("focus_mode_continuous_video")) {
			parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
		} else {
			if (MyDebug.LOG)
				Log.d(TAG, "setFocusValue() received unknown focus value " + focus_value);
		}
		mCamera.setParameters(parameters);
		clearFocusAreas();
		// n.b., we reset even for manual focus mode
		if (auto_focus) {
			tryAutoFocus(false, false);
		}
	}

	public void clearFocusAreas() {
		if (MyDebug.LOG)
			Log.d(TAG, "clearFocusAreas()");
		if (mCamera == null) {
			return;
		}
		cancelAutoFocus();
		Camera.Parameters parameters = mCamera.getParameters();
		boolean update_parameters = false;
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			if (parameters.getMaxNumFocusAreas() > 0) {
				parameters.setFocusAreas(null);
				update_parameters = true;
			}
			if (parameters.getMaxNumMeteringAreas() > 0) {
				parameters.setMeteringAreas(null);
				update_parameters = true;
			}
		}

		if (update_parameters) {
			mCamera.setParameters(parameters);
		}
		has_focus_area = false;
		focus_success = FOCUS_DONE;
		successfully_focused = false;
	}

	private void tryAutoFocus(final boolean startup, final boolean manual) {
		// manual: whether user has requested autofocus (by touching screen)
		if (MyDebug.LOG) {
			Log.d(TAG, "tryAutoFocus");
			Log.d(TAG, "startup? " + startup);
			Log.d(TAG, "manual? " + manual);
		}
		if (mCamera == null) {
			if (MyDebug.LOG)
				Log.d(TAG, "no camera");
		} else if (!this.has_surface) {
			if (MyDebug.LOG)
				Log.d(TAG, "preview surface not yet available");
		} else if (!this.is_preview_started) {
			if (MyDebug.LOG)
				Log.d(TAG, "preview not yet started");
		} else {
			// it's only worth doing autofocus when autofocus has an effect (i.e., auto or macro mode)
			Camera.Parameters parameters = mCamera.getParameters();
			String focus_mode = parameters.getFocusMode();
			// getFocusMode() is documented as never returning null, however I've had null pointer exceptions reported in Google Play from the below line (v1.7),
			// on Galaxy Tab 10.1 (GT-P7500), Android 4.0.3 - 4.0.4; HTC EVO 3D X515m (shooteru), Android 4.0.3 - 4.0.4
			if (focus_mode != null && (focus_mode.equals(Camera.Parameters.FOCUS_MODE_AUTO) || focus_mode.equals(Camera.Parameters.FOCUS_MODE_MACRO))) {
				if (MyDebug.LOG)
					Log.d(TAG, "try to start autofocus");
				String old_flash = parameters.getFlashMode();
				if (MyDebug.LOG)
					Log.d(TAG, "old_flash: " + old_flash);
				set_flash_after_autofocus = "";
				// getFlashMode() may return null if flash not supported! (下面方法无效，因为没有支持flash的切换)
				if (startup && old_flash != null && old_flash != Camera.Parameters.FLASH_MODE_OFF) {
					set_flash_after_autofocus = old_flash;
					parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
					mCamera.setParameters(parameters);
				}
				Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
					@Override
					public void onAutoFocus(boolean success, Camera camera) {
						if (MyDebug.LOG)
							Log.d(TAG, "autofocus complete: " + success);
						autoFocusCompleted(manual, success, false);
					}
				};

				this.focus_success = FOCUS_WAITING;
				this.focus_complete_time = -1;
				this.successfully_focused = false;
				try {
					mCamera.autoFocus(autoFocusCallback);
				} catch (RuntimeException e) {
					// just in case? We got a RuntimeException report here from 1 user on Google Play
					autoFocusCallback.onAutoFocus(false, mCamera);

					if (MyDebug.LOG)
						Log.e(TAG, "runtime exception from autoFocus");
					e.printStackTrace();
				}
			} else if (has_focus_area) {
				// do this so we get the focus box, for focus modes that support focus area, but don't support autofocus
				focus_success = FOCUS_SUCCESS;
				focus_complete_time = System.currentTimeMillis();
			}
		}
	}

	private void cancelAutoFocus() {
		if (MyDebug.LOG)
			Log.d(TAG, "cancelAutoFocus");
		if (mCamera != null) {
			try {
				mCamera.cancelAutoFocus();
			} catch (RuntimeException e) {
				// had a report of crash on some devices, see comment at https://sourceforge.net/p/opencamera/tickets/4/ made on 20140520
				if (MyDebug.LOG)
					Log.d(TAG, "camera.cancelAutoFocus() failed");
				e.printStackTrace();
			}
			autoFocusCompleted(false, false, true);
		}
	}

	private void autoFocusCompleted(boolean manual, boolean success, boolean cancelled) {
		if (MyDebug.LOG) {
			Log.d(TAG, "autoFocusCompleted");
			Log.d(TAG, "    manual? " + manual);
			Log.d(TAG, "    success? " + success);
			Log.d(TAG, "    cancelled? " + cancelled);
		}
		if (cancelled) {
			focus_success = FOCUS_DONE;
		} else {
			focus_success = success ? FOCUS_SUCCESS : FOCUS_FAILED;
			focus_complete_time = System.currentTimeMillis();
		}
		if (manual && !cancelled && success) {
			successfully_focused = true;
			successfully_focused_time = focus_complete_time;
		}
		if (set_flash_after_autofocus.length() > 0 && mCamera != null) {
			if (MyDebug.LOG)
				Log.d(TAG, "set flash back to: " + set_flash_after_autofocus);
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.setFlashMode(set_flash_after_autofocus);
			set_flash_after_autofocus = "";
			mCamera.setParameters(parameters);
		}
	}

	void onPause() {
		if (MyDebug.LOG)
			Log.d(TAG, "onPause");
		this.app_is_paused = true;
		this.closeCamera();
	}

	void switchCamera() {
		if (MyDebug.LOG)
			Log.d(TAG, "switchCamera()");
		// if( is_taking_photo && !is_taking_photo_on_timer ) {
		if (this.phase == PHASE_TAKING_PHOTO) {
			// just to be safe - risk of cancelling the autofocus before taking a photo, or otherwise messing things up
			if (MyDebug.LOG)
				Log.d(TAG, "currently taking a photo");
			return;
		}
		int n_cameras = Camera.getNumberOfCameras();
		if (MyDebug.LOG)
			Log.d(TAG, "found " + n_cameras + " cameras");
		if (n_cameras > 1) {
			closeCamera();
			mCameraId = (mCameraId + 1) % n_cameras;
			Camera.CameraInfo info = new Camera.CameraInfo();
			Camera.getCameraInfo(mCameraId, info);
			// zoom_factor = 0; // reset zoom when switching camera
			this.openCamera();
			// we update the focus, in case we weren't able to do it when switching video with a camera that didn't support focus modes
			updateFocusForVideo(true);
		}
	}

	void takePicturePressed() {
		if (MyDebug.LOG)
			Log.d(TAG, "takePicturePressed");
		if (mCamera == null) {
			if (MyDebug.LOG)
				Log.d(TAG, "camera not available");
			/*
			 * is_taking_photo_on_timer = false; is_taking_photo = false;
			 */
			this.phase = PHASE_NORMAL;
			return;
		}
		if (!this.has_surface) {
			if (MyDebug.LOG)
				Log.d(TAG, "preview surface not yet available");
			/*
			 * is_taking_photo_on_timer = false; is_taking_photo = false;
			 */
			this.phase = PHASE_NORMAL;
			return;
		}
		// if( is_taking_photo ) {
		if (this.phase == PHASE_TAKING_PHOTO) {
			if (is_video) {
				if (!video_start_time_set || System.currentTimeMillis() - video_start_time < 1000) {
					// if user presses to stop too quickly, we ignore
					// firstly to reduce risk of corrupt video files when stopping too quickly (see RuntimeException we have to catch in stopVideo),
					// secondly, to reduce a backlog of events which slows things down, if user presses start/stop repeatedly too quickly
					if (MyDebug.LOG)
						Log.d(TAG, "ignore pressing stop video too quickly after start");
				} else {
					stopVideo();
				}
			} else {
				if (MyDebug.LOG)
					Log.d(TAG, "already taking a photo");
			}
			return;
		}

		// make sure that preview running (also needed to hide trash/share icons)
		this.startCameraPreview();

		takePicture();
	}

	private void takePicture() {
		if (MyDebug.LOG)
			Log.d(TAG, "takePicture");
		this.phase = PHASE_TAKING_PHOTO;
		if (mCamera == null) {
			if (MyDebug.LOG)
				Log.d(TAG, "camera not available");
			/*
			 * is_taking_photo_on_timer = false; is_taking_photo = false;
			 */
			this.phase = PHASE_NORMAL;
			showGUI(true);
			return;
		}
		if (!this.has_surface) {
			if (MyDebug.LOG)
				Log.d(TAG, "preview surface not yet available");
			/*
			 * is_taking_photo_on_timer = false; is_taking_photo = false;
			 */
			this.phase = PHASE_NORMAL;
			showGUI(true);
			return;
		}
		focus_success = FOCUS_DONE; // clear focus rectangle

		if (is_video) {
			if (MyDebug.LOG)
				Log.d(TAG, "start video recording");
			// VideoRecordActivity main_activity = (VideoRecordActivity) this.getContext();
			// File videoFile = main_activity.getOutputMediaFile(VideoRecordActivity.MEDIA_TYPE_VIDEO);
			String filePath = FileUtil.getRandomVideoFilePath();
			if (filePath == null) {
				Log.e(TAG, "Couldn't create media video file; check storage permissions?");
				Toast.makeText(getContext(), getContext().getString(R.string.save_failed), Toast.LENGTH_SHORT).show();
			} else {
				video_name = filePath;
				if (MyDebug.LOG)
					Log.d(TAG, "save to: " + video_name);

				CamcorderProfile profile = getCamcorderProfile();

				this.mCamera.unlock();
				video_recorder = new MediaRecorder();
				video_recorder.setCamera(mCamera);
				video_recorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
				video_recorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

				video_recorder.setProfile(profile);

				if (MyDebug.LOG) {
					Log.d(TAG, "video fileformat: " + profile.fileFormat);
				}

				video_recorder.setOrientationHint(this.current_rotation);
				video_recorder.setOutputFile(video_name);
				try {
					showGUI(false);

					video_recorder.setPreviewDisplay(mHolder.getSurface());
					video_recorder.prepare();
					video_recorder.start();
					video_start_time = System.currentTimeMillis();
					video_start_time_set = true;
					// don't send intent for ACTION_MEDIA_SCANNER_SCAN_FILE yet - wait until finished, so we get completed file

					final Activity activity = (Activity) getContext();
					// handle restart timer
					long timer_delay = 0;
					if (activity.getIntent() != null) {
						timer_delay = activity.getIntent().getIntExtra(VideoRecordActivity.EXTRA_TIME_LIMIT, 0);
					}

					if (timer_delay > 0) {
						class RestartVideoTimerTask extends TimerTask {
							public void run() {
								if (MyDebug.LOG)
									Log.e(TAG, "stop video on timer");
								// need to run on UI thread, as stopVideo->MainActivity.updateGalleryIconToBitmap must be run on UI thread
								activity.runOnUiThread(new Runnable() {
									public void run() {
										restartVideo();
									}
								});
							}
						}
						restartVideoTimer.schedule(restartVideoTimerTask = new RestartVideoTimerTask(), timer_delay);
					}
				} catch (IOException e) {
					if (MyDebug.LOG)
						Log.e(TAG, "failed to save video");
					e.printStackTrace();
					video_recorder.reset();
					video_recorder.release();
					video_recorder = null;
					/*
					 * is_taking_photo = false; is_taking_photo_on_timer = false;
					 */
					this.phase = PHASE_NORMAL;
					showGUI(true);
					this.reconnectCamera(true);
				} catch (RuntimeException e) {
					// needed for emulator at least - although MediaRecorder not meant to work with emulator, it's good to fail gracefully
					if (MyDebug.LOG)
						Log.e(TAG, "runtime exception starting video recorder");
					e.printStackTrace();
					video_recorder.reset();
					video_recorder.release();
					video_recorder = null;
					/*
					 * is_taking_photo = false; is_taking_photo_on_timer = false;
					 */
					this.phase = PHASE_NORMAL;
					showGUI(true);
					this.reconnectCamera(true);
				}
			}
			return;
		}

	}

	private void restartVideo() {
		if (MyDebug.LOG)
			Log.d(TAG, "restartVideo()");
		if (video_recorder != null) {
			stopVideo();
		}
	}

	// //////////////////////////////// 工具方法 //////////////////////////////////////////////
	// for the Preview - from http://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation(int)
	// note, if orientation is locked to landscape this is only called when setting up the activity, and will always have the same orientation
	void setCameraDisplayOrientation(Activity activity) {
		if (mCamera == null)
			return;
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(mCameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}

		int result = 0;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360; // compensate the mirror
		} else { // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		mCamera.setDisplayOrientation(result);
		display_orientation = result;
	}

	// must be static, to safely call from other Activities
	public static String getVideoQualityPreferenceKey(int cameraId) {
		return "video_quality_" + cameraId;
	}

	// must be static, to safely call from other Activities
	public static String getFocusPreferenceKey(int cameraId) {
		return "focus_value_" + cameraId;
	}

}
