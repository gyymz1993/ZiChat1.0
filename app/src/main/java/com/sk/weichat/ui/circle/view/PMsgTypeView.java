package com.sk.weichat.ui.circle.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import com.sk.weichat.bean.circle.PublicMessage;

public abstract class PMsgTypeView extends LinearLayout {

	public PMsgTypeView(Context context) {
		super(context);
	}

	public PMsgTypeView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PMsgTypeView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public abstract void attachPublicMessage(PublicMessage message);

	public abstract void onPause();

	public abstract void onResume();
	
	public abstract void onDestory();
}
