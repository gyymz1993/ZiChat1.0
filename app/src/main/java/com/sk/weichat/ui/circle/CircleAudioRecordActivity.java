package com.sk.weichat.ui.circle;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.sk.weichat.AppConstant;
import com.sk.weichat.R;
import com.sk.weichat.audio.AudioPalyer;
import com.sk.weichat.audio.AudioPalyer.AudioPlayListener;
import com.sk.weichat.audio.RecordController;
import com.sk.weichat.audio.RecordListener;
import com.sk.weichat.ui.base.ActionBackActivity;
import com.sk.weichat.util.CameraUtil;
import com.sk.weichat.util.ToastUtil;

/**
 * 发送音频的界面
 * 
 * 
 */
public class CircleAudioRecordActivity extends ActionBackActivity implements View.OnClickListener {

	private ImageView mImageView;
	private TextView mTimeLenTv;// 显示语音时长的TextView
	private ProgressBar mProgressBar;// 播放进度

	private Button mAddPicBtn;
	private Button mBackBtn;
	private Button mSubmitBtn;
	private Button mRecordBtn;
	private TextView mTextTipTv;

	// /
	private String mImageFilePath;
	private String mAudioFilePath;
	private int mTimeLen;
	private RecordController mRecordController;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (getIntent() != null) {
			mAudioFilePath = getIntent().getStringExtra(AppConstant.EXTRA_FILE_PATH);
			mTimeLen = getIntent().getIntExtra(AppConstant.EXTRA_TIME_LEN, 0);
			mImageFilePath = getIntent().getStringExtra(AppConstant.EXTRA_IMAGE_FILE_PATH);
		}

		setContentView(R.layout.activity_circle_audio_record);
		initView();
	}

	private void initView() {
		// find view
		mImageView = (ImageView) findViewById(R.id.image_view);
		mTimeLenTv = (TextView) findViewById(R.id.time_len_tv);
		mProgressBar = (ProgressBar) findViewById(R.id.progress);
		mAddPicBtn = (Button) findViewById(R.id.add_pic_button);
		mBackBtn = (Button) findViewById(R.id.back_btn);
		mSubmitBtn = (Button) findViewById(R.id.submit_btn);
		mTextTipTv = (TextView) findViewById(R.id.tip_text_tv);
		mRecordBtn = (Button) findViewById(R.id.record_btn);

		// init status
		if (!TextUtils.isEmpty(mImageFilePath)) {
			ImageLoader.getInstance().displayImage(Uri.fromFile(new File(mImageFilePath)).toString(), mImageView);
		}
		if (!TextUtils.isEmpty(mAudioFilePath) && mTimeLen > 0) {
			mTextTipTv.setText(R.string.motalk_voice_chat_tip_5);
			mSubmitBtn.setVisibility(View.VISIBLE);
			mTimeLenTv.setVisibility(View.VISIBLE);
			mTimeLenTv.setText(mTimeLen + "s");
		} else {
			mTextTipTv.setText(R.string.motalk_voice_chat_tip_1);
			mSubmitBtn.setVisibility(View.GONE);
			mTimeLenTv.setVisibility(View.GONE);
		}

		mImageView.setOnClickListener(this);
		mTimeLenTv.setOnClickListener(this);
		mAddPicBtn.setOnClickListener(this);
		mBackBtn.setOnClickListener(this);
		mSubmitBtn.setOnClickListener(this);

		mRecordController = new RecordController(this);
		mRecordController.setRecordListener(new RecordListener() {
			@Override
			public void onRecordSuccess(String filePath, int timeLen) {
				// 录音成功，返回录音文件的路径
				mTextTipTv.setText(R.string.motalk_voice_chat_tip_5);
				mRecordBtn.setBackgroundResource(R.drawable.publisher_continue_record);
				mAudioFilePath = filePath;
				mTimeLen = timeLen;
				mSubmitBtn.setVisibility(View.VISIBLE);
				mTimeLenTv.setVisibility(View.VISIBLE);

				mTimeLenTv.setText(timeLen + "s");
			}

			@Override
			public void onRecordStart() {
				if (mAudioPalyer.isPlaying()) {
					mAudioPalyer.stop();
				}
				mTimeLenTv.setVisibility(View.GONE);
				mProgressBar.setProgress(0);
				mHandler.removeMessages(SHOW_PROGRESS);

				// 录音开始
				mTextTipTv.setText(R.string.motalk_voice_chat_tip_6);
				mRecordBtn.setBackgroundResource(R.drawable.publisher_record_btn_red);
			}

			@Override
			public void onRecordCancel() {
				// 录音取消
				if (TextUtils.isEmpty(mAudioFilePath)) {
					mTextTipTv.setText(R.string.motalk_voice_chat_tip_1);
				} else {
					mTextTipTv.setText(R.string.motalk_voice_chat_tip_5);
				}
				mRecordBtn.setBackgroundResource(R.drawable.publisher_continue_record);
			}
		});
		mRecordBtn.setOnTouchListener(mRecordController);

		mAudioPalyer = new AudioPalyer();
		mAudioPalyer.setAudioPlayListener(mAudioPlayListener);

	}

	private AudioPalyer mAudioPalyer;

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.image_view:// 点击播放
		case R.id.time_len_tv:
			if (TextUtils.isEmpty(mAudioFilePath)) {
				return;
			}
			doPauseResume();
			break;

		case R.id.add_pic_button:// 点击选择图片
			showSelectPictureDialog();
			break;
		case R.id.back_btn:// 点击取消返回
			finish();
			break;
		case R.id.submit_btn:// 点击提交返回
			if (!TextUtils.isEmpty(mAudioFilePath)) {
				Intent intent = new Intent();
				intent.putExtra(AppConstant.EXTRA_FILE_PATH, mAudioFilePath);
				intent.putExtra(AppConstant.EXTRA_TIME_LEN, mTimeLen);
				intent.putExtra(AppConstant.EXTRA_IMAGE_FILE_PATH, mImageFilePath);
				setResult(Activity.RESULT_OK, intent);
			}
			finish();
			break;
		}
	}

	private void doPauseResume() {
		if (mAudioPalyer.isPlaying()) {
			mAudioPalyer.pause();
			mTimeLenTv.setVisibility(View.VISIBLE);
			mHandler.removeMessages(SHOW_PROGRESS);
		} else {
			mAudioPalyer.play(mAudioFilePath);
			mTimeLenTv.setVisibility(View.GONE);
			mHandler.sendEmptyMessage(SHOW_PROGRESS);
		}
	}

	private AudioPlayListener mAudioPlayListener = new AudioPlayListener() {
		@Override
		public void onSeekComplete() {
		}

		@Override
		public void onPreparing() {
		}

		@Override
		public void onPrepared() {
			mHandler.removeMessages(SHOW_PROGRESS);
			mHandler.sendEmptyMessage(SHOW_PROGRESS);
		}

		@Override
		public void onError() {
			mTimeLenTv.setVisibility(View.VISIBLE);
			mProgressBar.setProgress(0);
			mHandler.removeMessages(SHOW_PROGRESS);
			ToastUtil.showToast(CircleAudioRecordActivity.this, R.string.play_failed);
		}

		@Override
		public void onCompletion() {
			mTimeLenTv.setVisibility(View.VISIBLE);
			mProgressBar.setProgress(0);
			mHandler.removeMessages(SHOW_PROGRESS);
		}

		@Override
		public void onBufferingUpdate(int percent) {
		}
	};

	@Override
	public void onPause() {
		super.onPause();
		if (mAudioPalyer != null && mAudioPalyer.accidentPause()) {// 暂停了，那么就更新UI
			mTimeLenTv.setVisibility(View.VISIBLE);
			mHandler.removeMessages(SHOW_PROGRESS);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		if (mAudioPalyer != null && mAudioPalyer.accidentResume()) {// 恢复了暂停，那么就更新UI
			mTimeLenTv.setVisibility(View.GONE);
			mHandler.sendEmptyMessage(SHOW_PROGRESS);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mAudioPalyer != null) {// 恢复了暂停，那么就更新UI
			mAudioPalyer.release();
		}
	}

	private static final int SHOW_PROGRESS = 2;

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int pos;
			switch (msg.what) {
			case SHOW_PROGRESS:
				pos = setProgress();
				if (mAudioPalyer != null && mAudioPalyer.isPlaying()) {
					msg = obtainMessage(SHOW_PROGRESS);
					sendMessageDelayed(msg, 1000 - (pos % 1000));
				}
				break;
			}
		}
	};

	private int setProgress() {
		if (mAudioPalyer == null) {
			return 0;
		}
		int position = mAudioPalyer.getCurrentPosition();
		int duration = mAudioPalyer.getDuration();
		if (duration > 0) {
			// use long to avoid overflow
			long pos = 1000L * position / duration;
			mProgressBar.setProgress((int) pos);
		}
		return position;
	}

	private void showSelectPictureDialog() {
		String[] items = new String[] { getString(R.string.c_take_picture), getString(R.string.c_photo_album) };
		AlertDialog.Builder builder = new AlertDialog.Builder(this).setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (which == 0) {
					takePhoto();
				} else {
					selectPhoto();
				}
				dialog.dismiss();
			}
		});
		builder.show();
	}

	private static final int REQUEST_CODE_CAPTURE_PHOTO = 1;
	private static final int REQUEST_CODE_PICK_PHOTO = 2;
	private Uri mNewPhotoUri;

	private void takePhoto() {
		mNewPhotoUri = CameraUtil.getOutputMediaFileUri(this, CameraUtil.MEDIA_TYPE_IMAGE);
		CameraUtil.captureImage(this, mNewPhotoUri, REQUEST_CODE_CAPTURE_PHOTO);
	}

	private void selectPhoto() {
		CameraUtil.pickImageSimple(this, REQUEST_CODE_PICK_PHOTO);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE_CAPTURE_PHOTO) {// 拍照返回
			if (resultCode == Activity.RESULT_OK) {
				if (mNewPhotoUri != null) {
					mImageFilePath = mNewPhotoUri.getPath();
					ImageLoader.getInstance().displayImage(Uri.fromFile(new File(mImageFilePath)).toString(), mImageView);
				} else {
					ToastUtil.showToast(this, R.string.c_take_picture_failed);
				}
			}
		} else if (requestCode == REQUEST_CODE_PICK_PHOTO) {// 选择一张图片,然后立即调用裁减
			if (resultCode == Activity.RESULT_OK) {
				if (data != null && data.getData() != null) {
					mImageFilePath = CameraUtil.getImagePathFromUri(this, data.getData());
					ImageLoader.getInstance().displayImage(Uri.fromFile(new File(mImageFilePath)).toString(), mImageView);
				} else {
					ToastUtil.showToast(this, R.string.c_photo_album_failed);
				}
			}
		}

	}

}
