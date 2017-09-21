package com.sk.weichat.ui.account;

import java.util.HashMap;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.LoginRegisterResult;
import com.sk.weichat.helper.LoginHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.util.DeviceInfoUtil;
import com.sk.weichat.util.Md5Util;
import com.sk.weichat.util.ProgressDialogUtil;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.volley.ObjectResult;
import com.sk.weichat.volley.Result;
import com.sk.weichat.volley.StringJsonObjectRequest;
import com.ymz.baselibrary.utils.L_;

/**
 * 登陆界面
 * 
 * @author Dean Tao
 * @version 1.0
 */
public class LoginActivity extends BaseActivity implements View.OnClickListener {
	private EditText mPhoneNumberEdit;
	private EditText mPasswordEdit;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
		getSupportActionBar().setTitle(R.string.user_telphone_login);
		initView();
	}

	@Override
	protected void onResume() {
		super.onResume();
		//如果没有保存用户定位信息，那么去地位用户当前位置
		if (!MyApplication.getInstance().getBdLocationHelper().isLocationUpdate()) {
			MyApplication.getInstance().getBdLocationHelper().requestLocation();
		}
	}

	private void initView() {
		mPhoneNumberEdit = (EditText) findViewById(R.id.phone_numer_edit);
		mPasswordEdit = (EditText) findViewById(R.id.password_edit);
		//忘记密码按钮点击事件
		findViewById(R.id.forget_password_btn).setOnClickListener(this);
		//注册账号
		findViewById(R.id.register_account_btn).setOnClickListener(this);
		//登陆账号
		findViewById(R.id.login_btn).setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.register_account_btn:// 注册
			startActivity(new Intent(mContext, RegisterActivity.class));
			break;
		case R.id.forget_password_btn:// 忘记密码
			// Intent intent = new Intent(mContext, FindPwdActivity.class);
			// intent.putExtra(FindPwdActivity.EXTRA_FROM_LOGIN, this.getClass().getName());
			// startActivity(intent);
			break;
		case R.id.login_btn:// 登陆
			login();
			break;
		}
	}

	private void login() {
		final String phoneNumber = mPhoneNumberEdit.getText().toString().trim();
		String password = mPasswordEdit.getText().toString().trim();
		if (TextUtils.isEmpty(phoneNumber)) {
			return;
		}
		if (TextUtils.isEmpty(password)) {
			return;
		}
		// 加密之后的密码
		final String digestPwd = new String(Md5Util.toMD5(password));

		final String requestTag = "login";

		final ProgressDialog dialog = ProgressDialogUtil.init(mContext, null, getString(R.string.please_wait), true);
		dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				cancelAll(requestTag);
			}
		});
		ProgressDialogUtil.show(dialog);

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("telephone", Md5Util.toMD5(phoneNumber));// 账号登陆的时候需要MD5加密，服务器需求
		params.put("password", digestPwd);
		// 附加信息
		params.put("model", DeviceInfoUtil.getModel());
		params.put("osVersion", DeviceInfoUtil.getOsVersion());
		params.put("serial", DeviceInfoUtil.getDeviceId(mContext));
		// 地址信息
		double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
		double longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
		if (latitude != 0)
			params.put("latitude", String.valueOf(latitude));
		if (longitude != 0)
			params.put("longitude", String.valueOf(longitude));

		StringJsonObjectRequest<LoginRegisterResult> request = new StringJsonObjectRequest<>(mConfig.USER_LOGIN,
				new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						ProgressDialogUtil.dismiss(dialog);
						ToastUtil.showErrorNet(mContext);
					}
				}, new StringJsonObjectRequest.Listener<LoginRegisterResult>() {

					@Override
					public void onResponse(ObjectResult<LoginRegisterResult> result) {


						if (result == null) {
							ProgressDialogUtil.dismiss(dialog);
							ToastUtil.showErrorData(mContext);
							return;
						}
						boolean success = false;
						if (result.getResultCode() == Result.CODE_SUCCESS) {
							success = LoginHelper.setLoginUser(mContext, phoneNumber, digestPwd, result);// 设置登陆用户信息
						}
						if (success) {// 登陆成功
							startActivity(new Intent(mContext, DataDownloadActivity.class));
						} else {// 登录失败
							String message = TextUtils.isEmpty(result.getResultMsg()) ? getString(R.string.login_failed) : result.getResultMsg();
							ToastUtil.showToast(mContext, message);
						}
						ProgressDialogUtil.dismiss(dialog);
					}
				}, LoginRegisterResult.class, params);
		request.setTag(requestTag);
		addDefaultRequest(request);
	}

}
