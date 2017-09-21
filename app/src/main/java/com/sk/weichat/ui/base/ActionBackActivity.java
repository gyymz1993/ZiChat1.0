package com.sk.weichat.ui.base;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.view.MenuItem;

import com.sk.weichat.AppConfig;
import com.sk.weichat.MyApplication;

public class ActionBackActivity extends StackActivity {
	protected Context mContext;
	public AppConfig mConfig;
	private boolean isDestroyed = false;
	protected String TAG;// 获取Tag，用于日志输出等标志

	public ActionBackActivity() {
		TAG = this.getClass().getSimpleName();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//竖屏
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayShowHomeEnabled(true);
			actionBar.setDisplayHomeAsUpEnabled(true);
		}
		mConfig = MyApplication.getInstance().getConfig();
		mContext = this;
		if (AppConfig.DEBUG) {
			Log.e(AppConfig.TAG, TAG + " onCreate");
		}
	}

	@Override
	public boolean isDestroyed() {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
			return super.isDestroyed();
		}
		return isDestroyed;
	}

	@Override
	protected void onDestroy() {
		isDestroyed = true;
		mConfig = null;
		mContext = null;
		if (AppConfig.DEBUG) {
			Log.e(AppConfig.TAG, TAG + " onDestroy");
		}
		super.onDestroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == android.R.id.home) {
			return onHomeAsUp();
		}
		return super.onOptionsItemSelected(item);
	}

	protected boolean onHomeAsUp() {
		finish();
		return true;
	}

}
