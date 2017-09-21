package com.sk.weichat.ui.account;

import java.util.HashMap;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.IntentCompat;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.LoginRegisterResult;
import com.sk.weichat.bean.LoginRegisterResult.Login;
import com.sk.weichat.bean.User;
import com.sk.weichat.db.dao.UserDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.helper.LoginHelper;
import com.sk.weichat.sp.UserSp;
import com.sk.weichat.ui.MainActivity;
import com.sk.weichat.ui.base.ActivityStack;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.util.DeviceInfoUtil;
import com.sk.weichat.util.Md5Util;
import com.sk.weichat.util.ProgressDialogUtil;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.volley.ObjectResult;
import com.sk.weichat.volley.Result;
import com.sk.weichat.volley.StringJsonObjectRequest;

/**
 * 历史登陆界面
 * 
 * @author Dean Tao
 * @version 1.0
 */
public class LoginHistoryActivity extends BaseActivity implements View.OnClickListener {

	private ImageView mAvatarImgView;
	private TextView mNickNameTv;
	private EditText mPasswordEdit;

	private User mLastLoginUser;
	private int mOldLoginStatus;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mOldLoginStatus = MyApplication.getInstance().mUserStatus;

		String userId = UserSp.getInstance(this).getUserId("");
		mLastLoginUser = UserDao.getInstance().getUserByUserId(userId);
		if (!LoginHelper.isUserValidation(mLastLoginUser)) {
			Intent intent = new Intent(this, LoginActivity.class);
			overridePendingTransition(0, 0);
			startActivity(intent);
			finish();
			return;
		}
		setContentView(R.layout.activity_login_history);

		getSupportActionBar().setIcon(0);
		getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		initView();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_login_history, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.switch_account) {
			Intent intent = new Intent(this, LoginActivity.class);
			startActivity(intent);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onBackPressed() {
		ActivityStack.getInstance().exit();
	}

	private void initView() {
		mAvatarImgView = (ImageView) findViewById(R.id.avatar_img);
		mNickNameTv = (TextView) findViewById(R.id.nick_name_tv);
		mPasswordEdit = (EditText) findViewById(R.id.password_edit);
		findViewById(R.id.register_account_btn).setOnClickListener(this);
		findViewById(R.id.forget_password_btn).setOnClickListener(this);
		findViewById(R.id.login_btn).setOnClickListener(this);
		AvatarHelper.getInstance().displayAvatar(mLastLoginUser.getUserId(), mAvatarImgView, true);
		mNickNameTv.setText(mLastLoginUser.getNickName());
	}

	private void login() {
		String password = mPasswordEdit.getText().toString().trim();
		if (TextUtils.isEmpty(password)) {
			return;
		}
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
		params.put("telephone", Md5Util.toMD5(mLastLoginUser.getTelephone()));// 账号登陆的时候需要MD5以下，服务器需求
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

		StringJsonObjectRequest<LoginRegisterResult> request = new StringJsonObjectRequest<LoginRegisterResult>(mConfig.USER_LOGIN,
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
							success = LoginHelper.setLoginUser(mContext, mLastLoginUser.getTelephone(), digestPwd, result);// 设置登陆用户信息
						}
						if (success) {// 登陆成功
							Login login = result.getData().getLogin();
							if (login != null && login.getSerial() != null && login.getSerial().equals(DeviceInfoUtil.getDeviceId(mContext))
									&& mOldLoginStatus != LoginHelper.STATUS_USER_NO_UPDATE && mOldLoginStatus != LoginHelper.STATUS_NO_USER) {// 如果Token没变，上次更新也是完整更新，那么直接进入Main程序
								// 其他的登陆地方都需进入DataDownloadActivity，在DataDownloadActivity里发送此广播
								LoginHelper.broadcastLogin(mContext);
								Intent intent = new Intent(mContext, MainActivity.class);
								intent.setFlags(IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
								intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
								startActivity(intent);
							} else {// 否则，进入数据下载界面
								startActivity(new Intent(mContext, DataDownloadActivity.class));
							}
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

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.register_account_btn:
			startActivity(new Intent(LoginHistoryActivity.this, RegisterActivity.class));
			break;
		case R.id.forget_password_btn:
			// Intent intent2 = new Intent(LoginHistoryActivity.this,
			// FindPwdActivity.class);
			// intent2.putExtra(FindPwdActivity.EXTRA_FROM_LOGIN,
			// this.getClass().getName());
			// startActivity(intent2);
			break;
		case R.id.login_btn:
			login();
			break;
		}
	}
}
