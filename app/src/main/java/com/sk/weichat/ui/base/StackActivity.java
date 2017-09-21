package com.sk.weichat.ui.base;

import android.content.pm.ActivityInfo;
import android.os.Bundle;

import com.sk.weichat.MyApplication;

/**
 * 加入了堆栈的Activity
 * 
 * @author Dean Tao
 * 
 */
public class StackActivity extends DefaultResourceActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);//竖屏
		ActivityStack.getInstance().push(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		ActivityStack.getInstance().pop(this);
		if (isFinishing()) {
			if (!ActivityStack.getInstance().has()) {
				MyApplication.getInstance().destory();
			}
		}
	}
}
