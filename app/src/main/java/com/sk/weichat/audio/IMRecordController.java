package com.sk.weichat.audio;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class IMRecordController implements View.OnTouchListener, RecordStateListener {
	private final int UP_MOVE_CHECK_NUM = 80;
	private Context mContext;
	private RecordPopWindow mRecordPopWindow;
	private long mLastTouchUpTime = System.currentTimeMillis();
	private RecordManager mRecordManager;
	private int mLastY = 0;
	private int timeLen;
	private  boolean isRun=true;

	public IMRecordController(Context context) {
		mContext = context;
		mRecordPopWindow = new RecordPopWindow(mContext);
		mRecordManager = RecordManager.getInstance();
		mRecordManager.setVoiceVolumeListener(this);
	}

	private boolean canVoice() {
		long now = System.currentTimeMillis();
		return now - mLastTouchUpTime > 100;
	}

	private RecordListener mRecordListener;

	public void setRecordListener(RecordListener listener) {
		mRecordListener = listener;
	}

	/**
	 * 判断是否在上滑
	 *
	 * @param y
	 * @return
	 */
	private boolean upMove(int y) {
		if ((mLastY - y) > UP_MOVE_CHECK_NUM) {
			return true;
		}
		return false;
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			Log.d("wang", "按下了");
			if(isRun){

			mLastY = (int) event.getY();
			if (canVoice() && !mRecordManager.isRunning()) {
				if (mRecordListener != null) {
					mRecordListener.onRecordStart();
				}
				mRecordPopWindow.startRecord();
				mRecordManager.startRecord();
				final MotionEvent ev = event;
				ev.setAction(MotionEvent.ACTION_MOVE);
				v.dispatchTouchEvent(ev);
			}
			}
		} else if (event.getAction() == MotionEvent.ACTION_MOVE) {
			if(isRun){
			if (mRecordManager.isRunning()) {
				if (!mRecordPopWindow.isRubishVoiceImgShow()) {
					if (upMove((int) event.getY())) {
						mRecordPopWindow.setRubishTip();
					}
				} else {
					if (!upMove((int) event.getY())) {
						mRecordPopWindow.hideRubishTip();
					}
				}
			}
			}
		} else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
			mLastY = 0;
			if(isRun){
			if (mRecordManager.isRunning()) {
				mLastTouchUpTime = System.currentTimeMillis();
			}
			if (mRecordPopWindow.isRubishVoiceImgShow()) {
				mRecordManager.cancel();
			} else {
				mRecordManager.stop();
			}
			}
			isRun=true;
		}
		return true;
	}

	@Override
	public void onRecordStarting() {
		mRecordPopWindow.show();

	}

	@Override
	public void onRecordStart() {

	}

	@Override
	public void onRecordFinish(String file) {
		mRecordPopWindow.dismiss();
		if (mRecordListener != null) {
			mRecordListener.onRecordSuccess(file, timeLen);
		}
	}

	@Override
	public void onRecordCancel() {
		mRecordPopWindow.dismiss();
		if (mRecordListener != null) {
			mRecordListener.onRecordCancel();
		}
	}

	@Override
	public void onRecordVolumeChange(int v) {
		Log.d("roamer", "v:" + v);
		int level = v / 1300;

		if (level < 0) {
			level = 0;
		} else if (level > 15) {
			level = 15;
		}
		mRecordPopWindow.setVoicePercent(level);
	}

	@Override
	public void onRecordTimeChange(int seconds) {
		mRecordPopWindow.setVoiceSecond(seconds);
		if (seconds >= 60) {//在录音到60s时让其自动发送
			timeLen = 60;
			isRun=false;//设置为false,让触摸动作不可用,防止程序的OOM
			if (mRecordManager.isRunning()) {
				mLastTouchUpTime = System.currentTimeMillis();
			}
			if (mRecordPopWindow.isRubishVoiceImgShow()) {
				mRecordManager.cancel();
			} else {
				mRecordManager.stop();
			}
		} else{
			timeLen = seconds;
		}
	}



	@Override
	public void onRecordError() {
		mRecordPopWindow.dismiss();
		mRecordListener.onRecordCancel();
		Toast.makeText(mContext, "录音出错啦！", Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onRecordTooShoot() {
		mRecordPopWindow.dismiss();
		Toast.makeText(mContext, "录音时间太短啦！", Toast.LENGTH_SHORT).show();
	}

	public void cancel() {
		if (mRecordPopWindow != null) {
			mRecordPopWindow.dismiss();
		}
		if (mRecordManager != null) {
			mRecordManager.cancel();
		}
	}

}
