package com.sk.weichat.ui.base;

import android.content.pm.ActivityInfo;
import android.os.Bundle;

import com.android.volley.Request;
import com.android.volley.RetryPolicy;
import com.sk.weichat.MyApplication;
import com.sk.weichat.volley.FastVolley;

/**
 * 带网络请求的Activity继承
 * 
 * @author Dean Tao
 */
public abstract class BaseActivity extends ActionBackActivity {

	private FastVolley mFastVolley;
	private String HASHCODE;

	public BaseActivity() {
		super();
		HASHCODE = Integer.toHexString(this.hashCode()) + "@";// 加上@符号，将拼在一起的两个HashCode分开
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mFastVolley = MyApplication.getInstance().getFastVolley();
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//竖屏
	}

	@Override
	protected void onDestroy() {
		// 取消所有HASHCODE包含该类名的request
		mFastVolley.cancelAll(HASHCODE);
		super.onDestroy();
	}

	public void addDefaultRequest(Request<?> request) {
		mFastVolley.addDefaultRequest(HASHCODE, request);
	}

	public void addShortRequest(Request<?> request) {
		mFastVolley.addShortRequest(HASHCODE, request);
	}

	public void addRequest(Request<?> request, RetryPolicy retryPolicy) {
		mFastVolley.addRequest(HASHCODE, request, retryPolicy);
	}

	public void cancelAll(Object tag) {
		mFastVolley.cancelAll(HASHCODE, tag);
	}

	public void cancelAll() {
		mFastVolley.cancelAll(HASHCODE);
	}

	public boolean isNetworkActive() {
		return MyApplication.getInstance().isNetworkActive();
	}

}
