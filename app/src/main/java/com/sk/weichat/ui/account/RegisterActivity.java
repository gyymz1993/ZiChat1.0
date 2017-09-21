package com.sk.weichat.ui.account;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.sk.weichat.R;
import com.sk.weichat.bean.AuthCode;
import com.sk.weichat.db.dao.AuthCodeDao;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.util.ProgressDialogUtil;
import com.sk.weichat.util.StringUtils;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.volley.ObjectResult;
import com.sk.weichat.volley.StringJsonObjectRequest;

/**
 * 注册的第一个页面（1、输入手机号）
 * 
 * @author Dean Tao
 * @version 1.0
 */
public class RegisterActivity extends BaseActivity {
	public static final String EXTRA_AUTH_CODE = "auth_code";
	public static final String EXTRA_PHONE_NUMBER = "phone_number";
	public static final String EXTRA_PASSWORD = "password";

	private EditText mPhoneNumEdit;
	private EditText mAuthCodeEdit;
	private Button mSendAgainBtn;
	private Button mNextStepBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_register);
		getSupportActionBar().setTitle(R.string.register_step_one);
		initView();
	}

	private void initView() {
		mPhoneNumEdit = (EditText) findViewById(R.id.phone_numer_edit);
		mAuthCodeEdit = (EditText) findViewById(R.id.auth_code_edit);
		mSendAgainBtn = (Button) findViewById(R.id.send_again_btn);
		mNextStepBtn = (Button) findViewById(R.id.next_step_btn);

		mSendAgainBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String phoneNumber = mPhoneNumEdit.getText().toString().trim();
				if (TextUtils.isEmpty(phoneNumber)) {
					return;
				}
				if (!StringUtils.isMobileNumber(phoneNumber)) {
					mPhoneNumEdit.requestFocus();
					mPhoneNumEdit.setError(StringUtils.editTextHtmlErrorTip(
							mContext, R.string.phone_number_format_error));
					return;
				}

				verifyTelephone(phoneNumber);
			}
		});

		mNextStepBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// 验证码先屏蔽了
				// nextStep();
				final String phoneNumber = mPhoneNumEdit.getText().toString()
						.trim();
				if (phoneNumber.isEmpty()) {
					ToastUtil.showToast(mContext, "手机号码不能为空");
				} else {
					Intent intent = new Intent(RegisterActivity.this,
							RegisterPasswordActivity.class);
					intent.putExtra(RegisterActivity.EXTRA_PHONE_NUMBER,
							phoneNumber);
					startActivity(intent);
					finish();
				}
			}
		});
	}

	/* 验证该号码有没有注册 */
	private void verifyTelephone(final String phoneNumber) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("telephone", phoneNumber);

		final String requestTag = "verifyTelephone";
		final ProgressDialog dialog = ProgressDialogUtil.init(mContext, null,
				getString(R.string.please_wait), true);
		dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				cancelAll(requestTag);
			}
		});
		ProgressDialogUtil.show(dialog);

		StringJsonObjectRequest<Void> request = new StringJsonObjectRequest<Void>(
				mConfig.VERIFY_TELEPHONE, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						Log.e("error", "网络不通");
						ToastUtil.showErrorNet(RegisterActivity.this);
						ProgressDialogUtil.dismiss(dialog);
					}
				}, new StringJsonObjectRequest.Listener<Void>() {

					@Override
					public void onResponse(ObjectResult<Void> result) {
						ProgressDialogUtil.dismiss(dialog);
						if (result == null) {
							ToastUtil.showToast(RegisterActivity.this,
									R.string.data_exception);
							return;
						}
						if (result.getResultCode() == 1) {// 手机号没有被注册,那么就发送验证码

							mSendAgainBtn.setEnabled(false);
							mReckonHandler.sendEmptyMessage(0x1);
							sendAuthcode(phoneNumber);

						} else if (result.getResultCode() == 0) {// 手机号已经被注册
							if (!TextUtils.isEmpty(result.getResultMsg())) {
								ToastUtil.showToast(RegisterActivity.this,
										result.getResultMsg());
							} else {
								ToastUtil.showToast(RegisterActivity.this,
										R.string.telphone_already_rigister);
							}
						} else {// 错误
							if (!TextUtils.isEmpty(result.getResultMsg())) {
								ToastUtil.showToast(RegisterActivity.this,
										result.getResultMsg());
							} else {
								ToastUtil.showToast(RegisterActivity.this,
										R.string.data_exception);
							}
						}
					}
				}, Void.class, params);
		request.setTag(requestTag);
		addDefaultRequest(request);
	}

	/**
	 * 是否请求了验证码
	 * 
	 * @return
	 */
	private void sendAuthcode(final String phoneNumber) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("telephone", phoneNumber);
		params.put("token", "abcdefg");// 无效的参数。

		StringJsonObjectRequest<AuthCode> request = new StringJsonObjectRequest<AuthCode>(
				mConfig.SEND_AUTH_CODE, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						// 发送失败，使其能重新发送
						ToastUtil.showToast(mContext,
								R.string.get_auth_code_failed);
						mReckonHandler.removeCallbacksAndMessages(null);
						mReckonHandler.sendEmptyMessage(0x2);
					}
				}, new StringJsonObjectRequest.Listener<AuthCode>() {
					@Override
					public void onResponse(ObjectResult<AuthCode> result) {
						if (result != null && result.getResultCode() == 1) {// 发送成功
							if (result.getData() != null) {
								AuthCode authCode = result.getData();
								authCode.setPhoneNumber(phoneNumber);
								// 过期时间为收到验证码之后的两分钟
								authCode.setOverdueTime(TimeUtils
										.sk_time_current_time() + 2 * 60);
								AuthCodeDao.getInstance()
										.saveAuthCode(authCode);
							} else {
								mReckonHandler.removeCallbacksAndMessages(null);
								mReckonHandler.sendEmptyMessage(0x2);
								ToastUtil.showToast(mContext,
										R.string.get_auth_code_failed);
							}
						} else {
							mReckonHandler.removeCallbacksAndMessages(null);
							mReckonHandler.sendEmptyMessage(0x2);
							ToastUtil.showToast(mContext, result.getResultMsg());
						}
					}
				}, AuthCode.class, params);
		addDefaultRequest(request);
	}

	private int reckonTime = 60;
	private Handler mReckonHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == 0x1) {
				mSendAgainBtn.setText("(" + reckonTime + ")");
				reckonTime--;
				if (reckonTime < 0) {
					mReckonHandler.sendEmptyMessage(0x2);
				} else {
					mReckonHandler.sendEmptyMessageDelayed(0x1, 1000);
				}
			} else if (msg.what == 0x2) {// 60秒结束
				mSendAgainBtn.setText(R.string.send);
				mSendAgainBtn.setEnabled(true);
				reckonTime = 60;
			}
		}
	};

	private void nextStep() {
		final String phoneNumber = mPhoneNumEdit.getText().toString().trim();
		if (TextUtils.isEmpty(phoneNumber)) {
			return;
		}

		if (!StringUtils.isMobileNumber(phoneNumber)) {
			mPhoneNumEdit.requestFocus();
			mPhoneNumEdit.setError(StringUtils.editTextHtmlErrorTip(this,
					R.string.phone_number_format_error));
			return;
		}

		// TODO 打开下面注释
		String authCode = mAuthCodeEdit.getText().toString().trim();
		if (TextUtils.isEmpty(authCode)) {
			// mAuthCodeEdit.requestFocus();
			// mAuthCodeEdit.setError(Utils.editTextHtmlErrorTip(this,
			// "请填写验证码"));
			return;
		}

		List<AuthCode> codesInDb = AuthCodeDao.getInstance().getAuthCode(
				phoneNumber);
		if (codesInDb == null || codesInDb.size() <= 0) {
			mAuthCodeEdit.requestFocus();
			mAuthCodeEdit.setError(StringUtils.editTextHtmlErrorTip(this,
					R.string.auth_code_error));
			return;
		}

		boolean match = false;
		int overdueTime = 0;
		for (int i = 0; i < codesInDb.size(); i++) {
			if (authCode.equals(codesInDb.get(i).getRandcode())) {
				match = true;
				overdueTime = codesInDb.get(i).getOverdueTime();
				break;
			}
		}

		if (!match) {
			mAuthCodeEdit.requestFocus();
			mAuthCodeEdit.setError(StringUtils.editTextHtmlErrorTip(this,
					R.string.auth_code_error));
			return;
		}

		if (overdueTime < TimeUtils.sk_time_current_time()) {// 过期了
			mAuthCodeEdit.requestFocus();
			mAuthCodeEdit.setError(StringUtils.editTextHtmlErrorTip(this,
					R.string.auth_code_overdue));
			return;
		}

		// 验证码匹配成功，清除数据库的所有该手机的验证码
		AuthCodeDao.getInstance().clearAuthCode(phoneNumber);

		Intent intent = new Intent(this, RegisterPasswordActivity.class);
		intent.putExtra(RegisterActivity.EXTRA_PHONE_NUMBER, phoneNumber);
		startActivity(intent);
		finish();

	}

}
