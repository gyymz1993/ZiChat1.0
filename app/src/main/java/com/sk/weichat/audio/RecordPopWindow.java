package com.sk.weichat.audio;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.sk.weichat.R;

public class RecordPopWindow {
	private Context mContext;
	private PopupWindow mPopup;
	private View mRootView;
	private ImageView mRubishVoiceImg;
	private TextView mVoiceTipTv;
	private TextView mVoiceSecondsTv;
	private ImageView mMicrophoneImageView;

	@SuppressWarnings("deprecation")
	public RecordPopWindow(Context context) {
		mContext = context;
		// mPopup.setFocusable(true);
		mRootView = LayoutInflater.from(mContext).inflate(R.layout.chat_voice_window, null);
		mRubishVoiceImg = (ImageView) mRootView.findViewById(R.id.rubish_voice);
		mVoiceTipTv = (TextView) mRootView.findViewById(R.id.voice_tip);
		mVoiceSecondsTv = (TextView) mRootView.findViewById(R.id.voice_seconds);
		mMicrophoneImageView = (ImageView) mRootView
				.findViewById(R.id.microphone_image_view);
		mMicrophoneImageView.setImageLevel(0);
		// 推出声音窗口
		mPopup = new PopupWindow(mRootView);
		mPopup.setFocusable(true);
//		mPopup.setBackgroundDrawable(new BitmapDrawable());
		mPopup.setOutsideTouchable(false);
		mPopup.setAnimationStyle(android.R.style.Animation_Dialog);
		mPopup.setHeight(LayoutParams.WRAP_CONTENT);
		mPopup.setWidth(LayoutParams.WRAP_CONTENT);
	}

	public void startRecord() {
		mMicrophoneImageView.setVisibility(View.VISIBLE);
		mMicrophoneImageView.setImageLevel(0);
		mRubishVoiceImg.setVisibility(View.GONE);
		mVoiceTipTv.setText(R.string.motalk_voice_chat_tip_3);
		mVoiceSecondsTv.setText("0''");
	}

	public boolean isRubishVoiceImgShow() {
		return mRubishVoiceImg.getVisibility() == View.VISIBLE;
	}

	public void setRubishTip() {
		mMicrophoneImageView.setVisibility(View.GONE);
		mRubishVoiceImg.setVisibility(View.VISIBLE);
		mVoiceTipTv.setText(R.string.motalk_voice_chat_tip_4);
	}
	
	public void hideRubishTip() {
		mMicrophoneImageView.setVisibility(View.VISIBLE);
		mRubishVoiceImg.setVisibility(View.GONE);
		mVoiceTipTv.setText(R.string.motalk_voice_chat_tip_3);
	}

	public boolean isShowing() {
		return mPopup.isShowing();
	}

	public void dismiss() {
		if (mPopup.isShowing()) {
			mPopup.dismiss();
		}
	}

	public void show() {
		if (!mPopup.isShowing()) {
			mPopup.showAtLocation(((Activity)mContext).getWindow().getDecorView(), Gravity.CENTER, 0, 0);
		}
	}
	
	public void setVoicePercent(int voicePercenet){
		mMicrophoneImageView.setImageLevel(voicePercenet);
	}
	
	public void setVoiceSecond(int seconds){
		mVoiceSecondsTv.setText(seconds + "s");
	}

}
