package com.sk.weichat.ui.circle.view;

import java.util.Formatter;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.baidu.cyberplayer.core.BVideoView;
import com.baidu.cyberplayer.core.BVideoView.OnCompletionListener;
import com.baidu.cyberplayer.core.BVideoView.OnErrorListener;
import com.baidu.cyberplayer.core.BVideoView.OnInfoListener;
import com.baidu.cyberplayer.core.BVideoView.OnPlayingBufferCacheListener;
import com.baidu.cyberplayer.core.BVideoView.OnPreparedListener;
import com.baidu.cyberplayer.core.BVideoView.OnSeekCompleteListener;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.sk.weichat.R;
import com.sk.weichat.bean.circle.PublicMessage;
import com.sk.weichat.helper.AvatarHelper;

public class PMsgVideoHeaderView extends PMsgTypeView implements OnPreparedListener, OnCompletionListener, OnErrorListener, OnInfoListener,
		OnPlayingBufferCacheListener, OnSeekCompleteListener {

	private FrameLayout mPlayFrame;
	private FrameLayout mVideoFrame;
	private ImageView mThumbImg;
	private BVideoView mBVideoView;
	private LinearLayout mCacheProgressLayout;
	private TextView mCacheProgressTv;
	private MediaControlView mMediaControlView;

	private StringBuilder mFormatBuilder;
	private Formatter mFormatter;

	private PublicMessage mPublicMessage;

	public PMsgVideoHeaderView(Context context) {
		super(context);
		init();
	}

	public PMsgVideoHeaderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	@SuppressLint("NewApi")
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public PMsgVideoHeaderView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		LayoutInflater.from(getContext()).inflate(R.layout.header_view_p_msg_video, this);
		setPortraitMode();
		mPlayFrame = (FrameLayout) findViewById(R.id.play_frame);
		mVideoFrame = (FrameLayout) findViewById(R.id.video_frame);
		mThumbImg = (ImageView) findViewById(R.id.thumb_img);
		mBVideoView = (BVideoView) findViewById(R.id.video_view);
		mCacheProgressLayout = (LinearLayout) findViewById(R.id.cache_progress_layout);
		mCacheProgressTv = (TextView) findViewById(R.id.cache_progress_tv);
		mMediaControlView = (MediaControlView) findViewById(R.id.media_control_view);

		mFormatBuilder = new StringBuilder();
		mFormatter = new Formatter(mFormatBuilder, Locale.getDefault());
		initVideoControl();
	}

	@SuppressWarnings("deprecation")
	public void setLandscapeMode() {
		int screenwidth = ((Activity) getContext()).getWindowManager().getDefaultDisplay().getWidth();
		int screenheight = ((Activity) getContext()).getWindowManager().getDefaultDisplay().getHeight();
		setLayoutParams(new AbsListView.LayoutParams(screenwidth, screenheight));
	}

	public void setPortraitMode() {// 设置动态高度为255dp
		setLayoutParams(new AbsListView.LayoutParams(AbsListView.LayoutParams.MATCH_PARENT, getResources().getDimensionPixelSize(
				R.dimen.pmsg_media_view_height)));
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b) {
		super.onLayout(changed, l, t, r, b);
		if (changed) {
			measureVideoSize();
		}
	}

	private void measureVideoSize() {
		if (mBVideoView != null && mPlayerStatus == PLAYER_STATUS.PLAYER_PREPARED) {
			int width = mBVideoView.getVideoWidth();
			int height = mBVideoView.getVideoHeight();
			if (width != 0 && height != 0) {
				int maxWidth = mVideoFrame.getWidth();
				int maxHeight = mVideoFrame.getHeight();
				float widthScale = maxWidth / ((float) width);
				float heightScale = maxHeight / ((float) height);
				float scale = widthScale > heightScale ? heightScale : widthScale;
				int actualWidth = (int) (scale * width);
				int actualHeight = (int) (scale * height);
				FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(actualWidth, actualHeight);
				params.gravity = Gravity.CENTER;
				mBVideoView.setLayoutParams(params);
			}

		}
	}

	/**
	 * 时间显示的工具方法
	 */
	private String stringForTime(int timeMs) {
		// timeMs=timeMs/1000; 百度播放器的时间单位就是秒，系统的VideoView是毫秒
		int seconds = timeMs % 60;
		int minutes = (timeMs / 60) % 60;
		int hours = timeMs / 3600;

		mFormatBuilder.setLength(0);
		if (hours > 0) {
			return mFormatter.format("%d:%02d:%02d", hours, minutes, seconds).toString();
		} else {
			return mFormatter.format("%02d:%02d", minutes, seconds).toString();
		}
	}

	private String mVideoSource;

	public void attachPublicMessage(PublicMessage publicMessage) {
		mPublicMessage = publicMessage;

		// mBVideoView.setVideoPath(publicMessage.getBody().getVideos());
		mVideoSource = mPublicMessage.getFirstVideo();
		// mVideoSource =
		// "http://devimages.apple.com/iphone/samples/bipbop/gear4/prog_index.m3u8";
		String imageUrl = publicMessage.getFirstImageOriginal();
		if (!TextUtils.isEmpty(imageUrl)) {
			ImageLoader.getInstance().displayImage(imageUrl, mThumbImg);
		} else {
			AvatarHelper.getInstance().displayAvatar(publicMessage.getUserId(), mThumbImg, false);
		}

		mEventHandler.sendEmptyMessage(EVENT_PLAY);
	}

	/** VideoView的控制 */

	private EventHandler mEventHandler;
	private HandlerThread mHandlerThread;
	private final Object SYNC_Playing = new Object();
	private int mLastPos = 0;// 记录播放位置
	private PLAYER_STATUS mPlayerStatus = PLAYER_STATUS.PLAYER_IDLE;// 当前播放状态

	/**
	 * 播放状态
	 */
	private enum PLAYER_STATUS {
		PLAYER_IDLE, PLAYER_PREPARING, PLAYER_PREPARED,
	}

	private static final int EVENT_PLAY = 0;

	class EventHandler extends Handler {
		public EventHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case EVENT_PLAY:
				if (mPlayerStatus != PLAYER_STATUS.PLAYER_IDLE) {// 如果已经播放了，等待上一次播放结束
					synchronized (SYNC_Playing) {
						try {
							SYNC_Playing.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
				}
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						mPlayFrame.setVisibility(View.GONE);
						mCacheProgressTv.setText("");
						mCacheProgressLayout.setVisibility(View.VISIBLE);
					}
				});
				mBVideoView.setVideoPath(mVideoSource);// 设置播放url
				if (mLastPos > 0) {// 续播，如果需要如此
					mBVideoView.seekTo(mLastPos);
					mLastPos = 0;
				}
				mBVideoView.start();// 开始播放
				mPlayerStatus = PLAYER_STATUS.PLAYER_PREPARING;
				break;
			default:
				break;
			}
		}
	}

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
	 * Show the controller on screen. It will go away automatically after 3 seconds of inactivity.
	 */
	public void show() {
		show(sDefaultTimeout);
	}

	/**
	 * Show the controller on screen. It will go away automatically after 'timeout' milliseconds of inactivity.
	 * 
	 * @param timeout
	 *            The timeout in milliseconds. Use 0 to show the controller until hide() is called.
	 */
	public void show(int timeout) {
		mMediaControlView.setVisibility(View.VISIBLE);
		setProgress();
		mMediaControlView.updatePausePlay(mBVideoView.isPlaying());

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
				if (!mDragging && mBVideoView.isPlaying()) {
					msg = obtainMessage(SHOW_PROGRESS);
					sendMessageDelayed(msg, 1000 - (pos % 1000));
				}
				break;
			}
		}
	};

	private int setProgress() {
		if (mBVideoView == null || mDragging) {
			return 0;
		}
		int position = mBVideoView.getCurrentPosition();
		int duration = mBVideoView.getDuration();
		if (duration > 0) {
			// use long to avoid overflow
			long pos = 1000L * position / duration;
			mMediaControlView.setProgress((int) pos);
		}
		mMediaControlView.setTotalTime(stringForTime(duration));
		mMediaControlView.setPastTime(stringForTime(position));

		return position;
	}

	private void doPauseResume() {
		if (mPlayerStatus != PLAYER_STATUS.PLAYER_PREPARED) {
			mEventHandler.sendEmptyMessage(EVENT_PLAY);
		} else {
			if (mBVideoView.isPlaying()) {
				mBVideoView.pause();
			} else {
				mBVideoView.resume();
			}
		}
		mMediaControlView.updatePausePlay(mBVideoView.isPlaying());
	}

	private static final int sDefaultTimeout = 8000;

	private String AK = "z7Z1hMWvmXcDg4rVUuWGw9QD";// 您的ak
	private String SK = "W6jxAWzMXdsThf3UwzPZcvYEXkrYW6rI";// 您的sk的前16位

	private void initVideoControl() {
		BVideoView.setAKSK(AK, SK);

		mBVideoView.setOnPreparedListener(this);
		mBVideoView.setOnCompletionListener(this);
		mBVideoView.setOnErrorListener(this);
		mBVideoView.setOnInfoListener(this);
		mBVideoView.setOnPlayingBufferCacheListener(this);
		mBVideoView.setOnSeekCompleteListener(this);

		mBVideoView.showCacheInfo(false);
		mBVideoView.setDecodeMode(BVideoView.DECODE_HW);
		mCacheProgressLayout.setVisibility(View.GONE);

		mPlayFrame.setOnClickListener(mMediaControlViewVisibilityListener);
		mVideoFrame.setOnClickListener(mMediaControlViewVisibilityListener);

		mMediaControlView.setOnSeekBarChangeListener(mSeekListener);

		mMediaControlView.setOnPreClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int pos = mBVideoView.getCurrentPosition();
				// pos -= 2000; // milliseconds
				pos -= 2; // seconds
				mBVideoView.seekTo(pos);
			}
		});
		mMediaControlView.setOnNextClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				int pos = mBVideoView.getCurrentPosition();
				// pos += 2000; // milliseconds
				pos += 2; // seconds
				mBVideoView.seekTo(pos);
			}
		});
		mMediaControlView.setOnPlayPauseClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				doPauseResume();
				show(sDefaultTimeout);
			}
		});

		mMediaControlView.setOnFullClickListener(new View.OnClickListener() {
			@SuppressWarnings("deprecation")
			@Override
			public void onClick(View v) {
				Activity activity = (Activity) getContext();
				int screenwidth = activity.getWindowManager().getDefaultDisplay().getWidth();
				int screenheight = activity.getWindowManager().getDefaultDisplay().getHeight();
				// 设置屏幕为横屏
				if (screenwidth < screenheight) {
					activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
				} else {
					activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
				}
			}
		});

		/**
		 * 开启后台事件处理线程
		 */
		mHandlerThread = new HandlerThread("event handler thread", Process.THREAD_PRIORITY_BACKGROUND);
		mHandlerThread.start();
		mEventHandler = new EventHandler(mHandlerThread.getLooper());
	}

	@Override
	public void onPlayingBufferCache(final int arg0) {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mCacheProgressTv.setText("缓冲 " + arg0 + "%");
			}
		});
	}

	@Override
	public boolean onInfo(int what, int extra) {
		switch (what) {
		case BVideoView.MEDIA_INFO_BUFFERING_START:// 开始缓冲
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					mCacheProgressTv.setText("缓冲 0%");
					mCacheProgressLayout.setVisibility(View.VISIBLE);
				}
			});
			break;
		case BVideoView.MEDIA_INFO_BUFFERING_END:// 结束缓冲
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					mCacheProgressLayout.setVisibility(View.GONE);
				}
			});
			break;
		default:
			break;
		}
		return false;
	}

	@Override
	public boolean onError(int arg0, int arg1) {
		synchronized (SYNC_Playing) {
			SYNC_Playing.notify();
		}
		mPlayerStatus = PLAYER_STATUS.PLAYER_IDLE;
		return true;
	}

	@Override
	public void onCompletion() {
		synchronized (SYNC_Playing) {
			SYNC_Playing.notify();
		}
		mPlayerStatus = PLAYER_STATUS.PLAYER_IDLE;
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mPlayFrame.setVisibility(View.VISIBLE);
				mMediaControlView.setProgress(1000);
				show();
			}
		});
	}

	@Override
	public void onSeekComplete() {
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mMediaControlView.updatePausePlay(mBVideoView.isPlaying());
				show(sDefaultTimeout);

				// Ensure that progress is properly updated in the future,
				// the call to show() does not guarantee this because it is a
				// no-op if we are already showing.
				mHandler.sendEmptyMessage(SHOW_PROGRESS);
			}
		});
	}

	@Override
	public void onPrepared() {
		mPlayerStatus = PLAYER_STATUS.PLAYER_PREPARED;
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				measureVideoSize();
				mCacheProgressLayout.setVisibility(View.GONE);
				show();
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

			long duration = mBVideoView.getDuration();
			long newposition = (duration * progress) / 1000L;
			mMediaControlView.setPastTime(stringForTime((int) newposition));
		}

		public void onStopTrackingTouch(SeekBar bar) {
			mDragging = false;
			long duration = mBVideoView.getDuration();
			long newposition = (duration * bar.getProgress()) / 1000L;
			mBVideoView.seekTo((int) newposition);
		}
	};

	// @Override
	// public boolean dispatchKeyEvent(KeyEvent event) {
	// int keyCode = event.getKeyCode();
	// final boolean uniqueDown = event.getRepeatCount() == 0 &&
	// event.getAction() == KeyEvent.ACTION_DOWN;
	// if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode ==
	// KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_SPACE) {
	// if (uniqueDown) {
	// doPauseResume();
	// show(sDefaultTimeout);
	// }
	// return true;
	// } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
	// if (uniqueDown && !mBVideoView.isPlaying()) {
	// mBVideoView.start();
	// mMediaControlView.updatePausePlay(mBVideoView.isPlaying());
	// show(sDefaultTimeout);
	// }
	// return true;
	// } else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP || keyCode ==
	// KeyEvent.KEYCODE_MEDIA_PAUSE) {
	// if (uniqueDown && mBVideoView.isPlaying()) {
	// mBVideoView.pause();
	// mMediaControlView.updatePausePlay(mBVideoView.isPlaying());
	// show(sDefaultTimeout);
	// }
	// return true;
	// } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode ==
	// KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_MUTE
	// || keyCode == KeyEvent.KEYCODE_CAMERA) {
	// // don't show the controls for volume adjustment
	// return super.dispatchKeyEvent(event);
	// } else if (keyCode == KeyEvent.KEYCODE_BACK || keyCode ==
	// KeyEvent.KEYCODE_MENU) {
	// if (uniqueDown) {
	// hide();
	// }
	// return true;
	// }
	//
	// show(sDefaultTimeout);
	// return super.dispatchKeyEvent(event);
	// }

	@Override
	public void onPause() {
		if (mPlayerStatus == PLAYER_STATUS.PLAYER_PREPARED) {
			mLastPos = mBVideoView.getCurrentPosition();
			mBVideoView.stopPlayback();
		}
	}

	@Override
	public void onResume() {
		if (mLastPos > 0) {
			mEventHandler.sendEmptyMessage(EVENT_PLAY);
		}
	}

	@Override
	public void onDestory() {
		mHandlerThread.quit();
	}

	public void showHide() {
		if (isControllerShowing()) {
			mMediaControlView.setVisibility(View.GONE);
			mMediaControlView.setVisibility(View.VISIBLE);
		} else {
			mMediaControlView.setVisibility(View.VISIBLE);
			mMediaControlView.setVisibility(View.GONE);
		}
		mMediaControlView.invalidate();
	}
}
