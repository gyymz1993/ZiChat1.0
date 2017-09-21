package com.sk.weichat.ui.tool;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.WebSettings.ZoomDensity;
import android.webkit.WebView;

import com.sk.weichat.R;
import com.sk.weichat.ui.base.ActionBackActivity;

/**
 * 查看WebView，传入网页地址和该界面的Title
 * 
 */
public class WebViewActivity extends ActionBackActivity {
	public static final String EXTRA_URL = "url";
	public static final String EXTRA_TITLE = "title";

	private WebView mWebView;

	private String mUrl;
	private String mTitle;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getIntent() != null) {
			mUrl = getIntent().getStringExtra(EXTRA_URL);
			mTitle = getIntent().getStringExtra(EXTRA_TITLE);
		}
		setContentView(R.layout.activity_web_view);
		if (!TextUtils.isEmpty(mTitle)) {
			getSupportActionBar().setTitle(mTitle);
		}
		initView();
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("SetJavaScriptEnabled")
	private void initView() {
		mWebView = (WebView) findViewById(R.id.web_view);
		mWebView.getSettings().setJavaScriptEnabled(true);
		mWebView.getSettings().setDisplayZoomControls(true);
		mWebView.getSettings().setSupportZoom(true);
		mWebView.getSettings().setDefaultZoom(ZoomDensity.CLOSE);// 设置默认缩放模式
		mWebView.getSettings().setBuiltInZoomControls(true);
		if (!TextUtils.isEmpty(mUrl)) {
			mWebView.loadUrl(mUrl);
		}
	}

}
