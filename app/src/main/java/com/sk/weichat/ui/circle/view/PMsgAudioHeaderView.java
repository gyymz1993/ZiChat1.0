package com.sk.weichat.ui.circle.view;

import java.util.Formatter;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.sk.weichat.R;
import com.sk.weichat.audio.AudioPalyer;
import com.sk.weichat.bean.circle.PublicMessage;
import com.sk.weichat.helper.AvatarHelper;

public class PMsgAudioHeaderView extends PMsgTypeView implements AudioPalyer.AudioPlayListener {

	private FrameLayout mPlayFrame;
	private ImageView mThumbImg;
	@SuppressWarnings("unused")
	private ImageView mStartImg;
	private ProgressBar mCacheProgressBar;
	private MediaControlView mMediaControlView;

	private AudioPalyer mAudioPalyer;

	private StringBuilder mFormatBuilder;
	private Formatter mFormatter;

	private PublicMessage mPublicMessage;

	public PMsgAudioHeaderView(Context context) {
		super(context);
		init();
	}

	public PMsgAudioHeaderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	@SuppressLint("NewApi")
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public PMsgAudioHeaderView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		LayoutInflater.from(getContext()).inflate(R.layout.header_view_p_msg_audio, this);
		mPlayFrame = (FrameLayout) findViewById(R.id.play_frame);
		mThumbImg = (ImageView) findViewById(R.id.thumb_img);
		mStartImg = (ImageView) findViewById(R.id.start_img);
		mCacheProgressBar = (ProgressBar) findViewById(R.id.cache_progress_bar);
		mMediaControlView = (MediaControlView) findViewById(R.id.media_control_view);

		mFormatBuilder = new StringBuilder();
		mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());

		mMediaControlView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return true;
			}
		});

		initAudioControl();
	}

	/**
	 * 时间显示的工具方法
	 */
	private String stringForTime(int timeMs) {
		int totalSeconds = timeMs / 1000;

		int seconds = totalSeconds % 60;
		int minutes = (totalSeconds / 60) % 60;
		int hours = totalSeconds / 3600;

		mFormatBuilder.setLength(0);
		if (hours > 0) {
			return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
		} else {
			return mFormatter.format("%02d:%02d", minutes, seconds).toString();
		}
	}

	public void attachPublicMessage(PublicMessage publicMessage) {
		mPublicMessage = publicMessage;
		// mBVideoView.setVideoPath(publicMessage.getBody().getVideos());
		// List<Images> images=publicMessage.getBody().getImages();
		// if(images!=null){
		//
		// }
		String imageUrl = mPublicMessage.getFirstImageOriginal();
		if (TextUtils.isEmpty(imageUrl)) {
			AvatarHelper.getInstance().displayAvatar(mPublicMessage.getUserId(), mThumbImg, false);
		} else {
			ImageLoader.getInstance().displayImage(imageUrl, mThumbImg);
		}
		mAudioPalyer.play(publicMessage.getFirstAudio());
	}

	/** 控制 */
	private View.OnClickListener mMediaControlViewVisibilityListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (isControllerShowing()) {
				hide();
			} else {
				show();
			}
		}
	};

	private boolean isControllerShowing() {
		return mMediaControlView.getVisibility() == View.VISIBLE;
	}

	private boolean mDragging = false;// 是否正在拖动进度条

	/**
	 * Show the controller on screen. It will go away automatically after 3
	 * seconds of inactivity.
	 */
	public void show() {
		show(sDefaultTimeout);
	}

	/**
	 * Show the controller on screen. It will go away automatically after
	 * 'timeout' milliseconds of inactivity.
	 * 
	 * @param timeout
	 *            The timeout in milliseconds. Use 0 to show the controller
	 *            until hide() is called.
	 */
	public void show(int timeout) {
		mMediaControlView.setVisibility(View.VISIBLE);
		setProgress();
		mMediaControlView.updatePausePlay(mAudioPalyer.isPlaying());

		// cause the progress bar to be updated even if mShowing
		// was already true. This happens, for example, if we're
		// paused with the progress bar showing the user hits play.
		mHandler.sendEmptyMessage(SHOW_PROGRESS);

		Message msg = mHandler.obtainMessage(FADE_OUT);
		if (timeout != 0) {
			mHandler.removeMessages(FADE_OUT);
			mHandler.sendMessageDelayed(msg, timeout);
		}
	}

	/**
	 * Remove the controller from the screen.
	 */
	public void hide() {
		if (isControllerShowing()) {
			mHandler.removeMessages(SHOW_PROGRESS);
			mMediaControlView.setVisibility(View.GONE);
		}
	}

	private static final int FADE_OUT = 1;
	private static final int SHOW_PROGRESS = 2;

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			int pos;
			switch (msg.what) {
			case FADE_OUT:
				hide();
				break;
			case SHOW_PROGRESS:
				pos = setProgress();
				if (!mDragging && mAudioPalyer.isPlaying()) {
					msg = obtainMessage(SHOW_PROGRESS);
					sendMessageDelayed(msg, 1000 - (pos % 1000));
				}
				break;
			}
		}
	};

	private int setProgress() {
		if (mDragging) {
			return 0;
		}
		int position = mAudioPalyer.getCurrentPosition();
		int duration = mAudioPalyer.getDuration();
		if (duration > 0) {
			// use long to avoid overflow
			long pos = 1000L * position / duration;
			mMediaControlView.setProgress((int) pos);
		}
		mMediaControlView.setTotalTime(stringForTime(duration));
		mMediaControlView.setPastTime(stringForTime(position));

		return position;
	}

	@Override
	public void onPause() {
		if (mAudioPalyer != null && mAudioPalyer.accidentPause()) {// 暂停了，那么就更新UI
			mMediaControlView.updatePausePlay(mAudioPalyer.isPlaying());
		}
	}

	@Override
	public void onResume() {
		if (mAudioPalyer != null && mAudioPalyer.accidentResume()) {// 恢复了暂停，那么就更新UI
			mMediaControlView.updatePausePlay(mAudioPalyer.isPlaying());
		}
	}

	@Override
	public void onDestory() {
		if (mAudioPalyer != null) {// 恢复了暂停，那么就更新UI
			mAudioPalyer.release();
		}
	}

	private void doPauseResume() {
		if (mAudioPalyer.isPlaying()) {
			mAudioPalyer.pause();
		} else {
			mAudioPalyer.play(mPublicMessage.getFirstAudio());
		}
		mMediaControlView.updatePausePlay(mAudioPalyer.isPlaying());
	}

	private static final int sDefaultTimeout = 8000;

	private void initAudioControl() {
		mAudioPalyer = new AudioPalyer();
		mAudioPalyer.setAudioPlayListener(this);
		mCacheProgressBar.setVisibility(View.GONE);
		mPlayFrame.setOnClickListener(mMediaControlViewVisibilityListener);

		mMediaControlView.setOnSeekBarChangeListener(mSeekListener);

		mMediaControlView.setOnPreClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int pos = mAudioPalyer.getCurrentPosition();
				pos -= 2000; // milliseconds
				mAudioPalyer.seekTo(pos);
			}
		});
		mMediaControlView.setOnNextClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int pos = mAudioPalyer.getCurrentPosition();
				pos += 2000; // milliseconds
				mAudioPalyer.seekTo(pos);
			}
		});
		mMediaControlView.setOnPlayPauseClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				doPauseResume();
				show(sDefaultTimeout);
			}
		});

	}

	private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
		public void onStartTrackingTouch(SeekBar bar) {
			show(3600000);
			mDragging = true;

			// By removing these pending progress messages we make sure
			// that a) we won't update the progress while the user adjusts
			// the seekbar and b) once the user is done dragging the thumb
			// we will post one of these messages to the queue again and
			// this ensures that there will be exactly one message queued up.
			mHandler.removeMessages(SHOW_PROGRESS);
		}

		public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
			if (!fromuser) {
				// We're not interested in programmatically generated changes to
				// the progress bar's position.
				return;
			}

			long duration = mAudioPalyer.getDuration();
			long newposition = (duration * progress) / 1000L;
			mMediaControlView.setPastTime(stringForTime((int) newposition));
		}

		public void onStopTrackingTouch(SeekBar bar) {
			mDragging = false;
			long duration = mAudioPalyer.getDuration();
			long newposition = (duration * bar.getProgress()) / 1000L;
			mAudioPalyer.seekTo((int) newposition);
		}
	};

//	@Override
//	public boolean dispatchKeyEvent(KeyEvent event) {
//		int keyCode = event.getKeyCode();
//		final boolean uniqueDown = event.getRepeatCount() == 0 && event.getAction() == KeyEvent.ACTION_DOWN;
//		if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_SPACE) {
//			if (uniqueDown) {
//				doPauseResume();
//				show(sDefaultTimeout);
//			}
//			return true;
//		} else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
//			if (uniqueDown && !mAudioPalyer.isPlaying()) {
//				mAudioPalyer.play(mPublicMessage.getFirstAudio());
//				mMediaControlView.updatePausePlay(mAudioPalyer.isPlaying());
//				show(sDefaultTimeout);
//			}
//			return true;
//		} else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
//			if (uniqueDown && mAudioPalyer.isPlaying()) {
//				mAudioPalyer.pause();
//				mMediaControlView.updatePausePlay(mAudioPalyer.isPlaying());
//				show(sDefaultTimeout);
//			}
//			return true;
//		} else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
//				|| keyCode == KeyEvent.KEYCODE_CAMERA) {
//			// don't show the controls for volume adjustment
//			return super.dispatchKeyEvent(event);
//		} else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU) {
//			if (uniqueDown) {
//				hide();
//			}
//			return true;
//		}
//
//		show(sDefaultTimeout);
//		return super.dispatchKeyEvent(event);
//	}
	
	@Override
	public void onPreparing() {
		mCacheProgressBar.setVisibility(View.VISIBLE);
	}

	@Override
	public void onPrepared() {
		show();
		mCacheProgressBar.setVisibility(View.GONE);
	}

	@Override
	public void onBufferingUpdate(int percent) {
	}

	@Override
	public void onError() {
		mMediaControlView.updatePausePlay(mAudioPalyer.isPlaying());
		mMediaControlView.setProgress(0);
	}

	@Override
	public void onSeekComplete() {
		mMediaControlView.updatePausePlay(mAudioPalyer.isPlaying());
		show(sDefaultTimeout);

		// Ensure that progress is properly updated in the future,
		// the call to show() does not guarantee this because it is a
		// no-op if we are already showing.
		mHandler.sendEmptyMessage(SHOW_PROGRESS);
	}

	@Override
	public void onCompletion() {
		mMediaControlView.updatePausePlay(mAudioPalyer.isPlaying());
		mMediaControlView.setProgress(0);
	}


}
