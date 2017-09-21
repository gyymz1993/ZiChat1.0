package com.sk.weichat.ui.circle.view;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.sk.weichat.R;

/**
 * 控制界面(纯显示)
 * 
 * 
 */
public class MediaControlView extends FrameLayout {

	private static final String LOVE_TAG_NO = "no";
	private static final String LOVE_TAG_YES = "yes";
	private static final String PLAYING_TAG_NO = "no";
	private static final String PLAYING_TAG_YES = "yes";

	private ImageView mLoveImg;
	private ImageView mBackImg;
	private ImageView mActionImg;
	private ImageView mForwardImg;
	private ImageView mFullImg;
	private TextView mPastTimeTv;
	private TextView mTotalTimeTv;
	private SeekBar mSeekBar;

	public MediaControlView(Context context) {
		super(context);
		init();
	}

	public MediaControlView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	@SuppressLint("NewApi")
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public MediaControlView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		LayoutInflater.from(getContext()).inflate(R.layout.layout_media_control_view, this);
		mLoveImg = (ImageView) findViewById(R.id.control_love_img);
		mBackImg = (ImageView) findViewById(R.id.control_back_img);
		mActionImg = (ImageView) findViewById(R.id.control_action_img);
		mForwardImg = (ImageView) findViewById(R.id.control_forward_img);
		mFullImg = (ImageView) findViewById(R.id.control_full_img);

		mPastTimeTv = (TextView) findViewById(R.id.past_time_tv);
		mTotalTimeTv = (TextView) findViewById(R.id.total_time_tv);
		mSeekBar = (SeekBar) findViewById(R.id.play_seekbar);
		mSeekBar.setMax(1000);

		mLoveImg.setTag(LOVE_TAG_NO);
		mActionImg.setTag(PLAYING_TAG_NO);
	}

	public void setSeekBarEnable(boolean enabled){
		mSeekBar.setEnabled(enabled);
	}
	
	/**
	 * 更新界面上显示的Icon
	 * 
	 * @param isPlaying
	 */
	public void updatePausePlay(boolean isPlaying) {
		if (isPlaying) {
			mActionImg.setTag(PLAYING_TAG_YES);
			mActionImg.setBackgroundResource(R.drawable.video_contrl_pause);
		} else {
			mActionImg.setTag(LOVE_TAG_NO);
			mActionImg.setBackgroundResource(R.drawable.video_contrl_start);
		}
	}

	/**
	 * 更新收藏Icon状态
	 * 
	 * @param checked
	 */
	public void updateLoveStatus(boolean checked) {
		if (checked) {
			mLoveImg.setTag(LOVE_TAG_YES);
			mLoveImg.setBackgroundResource(R.drawable.video_contrl_love_press);
		} else {
			mLoveImg.setTag(LOVE_TAG_NO);
			mLoveImg.setBackgroundResource(R.drawable.video_contrl_love);
		}
	}

	/**
	 * 
	 * @return 是否是播放状态
	 */
	public boolean getPlayingStatus() {
		return mActionImg.getTag().equals(PLAYING_TAG_YES);
	}

	/**
	 * 
	 * @return 是否是选中状态
	 */
	public boolean getLoveStatus() {
		return mLoveImg.getTag().equals(LOVE_TAG_YES);
	}

	public void setProgress(int progress) {
		mSeekBar.setProgress(progress);
	}

	public void setTotalTime(String time) {
		mTotalTimeTv.setText(time);
	}

	public void setPastTime(String time) {
		mPastTimeTv.setText(time);
	}

	// SeekBar改变的点击
	public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
		mSeekBar.setOnSeekBarChangeListener(listener);
	}

	// 播放和暂停按钮的点击
	public void setOnPlayPauseClickListener(OnClickListener listener) {
		mActionImg.setOnClickListener(listener);
	}

	// 上一个按钮的点击
	public void setOnPreClickListener(OnClickListener listener) {
		mBackImg.setOnClickListener(listener);
	}

	// 下一个按钮的点击
	public void setOnNextClickListener(OnClickListener listener) {
		mForwardImg.setOnClickListener(listener);
	}

	// 收藏按钮的点击
	public void setOnLoveClickListener(OnClickListener listener) {
		mLoveImg.setOnClickListener(listener);
	}

	// 全屏按钮的点击
	public void setOnFullClickListener(OnClickListener listener) {
		mFullImg.setOnClickListener(listener);
	}

}
