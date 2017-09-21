package com.sk.weichat.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.sk.weichat.AppConfig;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.ConfigBean;
import com.sk.weichat.helper.LoginHelper;
import com.sk.weichat.ui.account.LoginActivity;
import com.sk.weichat.ui.account.LoginHistoryActivity;
import com.sk.weichat.ui.account.RegisterActivity;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.volley.ObjectResult;
import com.sk.weichat.volley.Result;
import com.sk.weichat.volley.StringJsonObjectRequest;

/**
 * 启动页
 * 
 * @author Dean Tao
 * @version 1.0
 */
public class SplashActivity extends BaseActivity {
	private RelativeLayout mSelectLv;
	private Button mSelectLoginBtn;
	private Button mSelectRegBtn;

	private long mStartTimeMs;// 记录进入该界面时间，保证至少在该界面停留3秒
	private boolean mConfigReady = false;// 配置获取成功

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);

		mStartTimeMs = System.currentTimeMillis();

		mSelectLv = (RelativeLayout) findViewById(R.id.select_lv);
		mSelectLoginBtn = (Button) findViewById(R.id.select_login_btn);
		mSelectRegBtn = (Button) findViewById(R.id.select_register_btn);
		mSelectLoginBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				startActivity(new Intent(mContext, LoginActivity.class));
			}
		});
		mSelectRegBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				 startActivity(new Intent(mContext, RegisterActivity.class));
			}
		});
		mSelectLv.setVisibility(View.INVISIBLE);

		initConfig();// 初始化配置
	}

	/**
	 * 配置参数初始化
	 */
	private void initConfig() {
		if (!MyApplication.getInstance().isNetworkActive()) {// 没有网络的情况下
			setConfig(new ConfigBean());
			return;
		}
		StringJsonObjectRequest<ConfigBean> request = new StringJsonObjectRequest<>(AppConfig.CONFIG_URL, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
				setConfig(new ConfigBean());// 读取网络配置失败，使用默认配置
				Log.e("读取网关配置","读取失败");
			}
		}, new StringJsonObjectRequest.Listener<ConfigBean>() {
			@Override
			public void onResponse(ObjectResult<ConfigBean> result) {
				ConfigBean configBean = null;
				if (result == null || result.getResultCode() != Result.CODE_SUCCESS || result.getData() == null) {
					configBean = new ConfigBean();// 读取网络配置失败，使用默认配置
					Log.e("读取网关配置",configBean.toString());
				} else {
					configBean = result.getData();
					Log.e("读取网关配置",configBean.toString());
				}
				setConfig(configBean);
			}
		}, ConfigBean.class, null);
		addShortRequest(request);
	}

	private void setConfig(ConfigBean configBean) {
		MyApplication.getInstance().setConfig(AppConfig.initConfig(this, configBean));// 初始化配置
		mConfigReady = true;
		ready();
	}

	private void ready() {
		if (!mConfigReady) {
			return;
		}
		long currentTimeMs = System.currentTimeMillis();
		int useTime = (int) (currentTimeMs - mStartTimeMs);
		int delayTime = useTime > 2000 ? 0 : 2000 - useTime;
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				jump();
			}
		}, delayTime);
	}

	@SuppressLint("NewApi")
	private void jump() {
		if (isDestroyed()) {
			return;
		}
		int userStatus = LoginHelper.prepareUser(mContext);
		Intent intent = new Intent();
		switch (userStatus) {
		case LoginHelper.STATUS_USER_FULL:
		case LoginHelper.STATUS_USER_NO_UPDATE:
		case LoginHelper.STATUS_USER_TOKEN_OVERDUE:
			intent.setClass(mContext, MainActivity.class);
			break;
		case LoginHelper.STATUS_USER_SIMPLE_TELPHONE:
			intent.setClass(mContext, LoginHistoryActivity.class);
			break;
		case LoginHelper.STATUS_NO_USER:
		default:
			stay();
			return;// must return
		}
		startActivity(intent);
		finish();
	}

	// 停留在此界面
	private void stay() {
		mSelectLv.setVisibility(View.VISIBLE);
		Animation anim = AnimationUtils.loadAnimation(this, R.anim.alpha_in);
		mSelectLv.startAnimation(anim);
	}
}
