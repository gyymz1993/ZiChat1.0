package com.sk.weichat.ui.account;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.sk.weichat.R;
import com.sk.weichat.ui.base.ActionBackActivity;
import com.sk.weichat.util.Md5Util;
import com.sk.weichat.util.StringUtils;

/**
 * 注册输入密码界面
 * 
 * @author Dean Tao
 * @version 1.0
 */
public class RegisterPasswordActivity extends ActionBackActivity {

	private EditText mPasswordEdit;
	private EditText mConfirmPasswordEdit;
	private Button mNextStepBtn;

	private String mPhoneNum;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getIntent() != null) {
			mPhoneNum = getIntent().getStringExtra(RegisterActivity.EXTRA_PHONE_NUMBER);
		}
		setContentView(R.layout.activity_register_password);
		getSupportActionBar().setTitle(R.string.register_step_two);
		initView();
	}

	private void initView() {
		mPasswordEdit = (EditText) findViewById(R.id.password_edit);
		mConfirmPasswordEdit = (EditText) findViewById(R.id.confirm_password_edit);
		mNextStepBtn = (Button) findViewById(R.id.next_step_btn);

		mNextStepBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				nextStep();
			}
		});
	}

	private void nextStep() {
		final String password = mPasswordEdit.getText().toString().trim();
		String confirmPassword = mConfirmPasswordEdit.getText().toString().trim();
		if (TextUtils.isEmpty(password) || password.length() < 6) {
			mPasswordEdit.requestFocus();
			mPasswordEdit.setError(StringUtils.editTextHtmlErrorTip(this, R.string.password_empty_error));
			return;
		}
		if (TextUtils.isEmpty(confirmPassword) || confirmPassword.length() < 6) {
			mConfirmPasswordEdit.requestFocus();
			mConfirmPasswordEdit.setError(StringUtils.editTextHtmlErrorTip(this, R.string.confirm_password_empty_error));
			return;
		}
		if (!confirmPassword.equals(password)) {
			mConfirmPasswordEdit.requestFocus();
			mConfirmPasswordEdit.setError(StringUtils.editTextHtmlErrorTip(this, R.string.password_confirm_password_not_match));
			return;
		}

		Intent intent = new Intent();
		intent.setClass(this, RegisterUserBasicInfoActivity.class);
		intent.putExtra(RegisterActivity.EXTRA_PHONE_NUMBER, mPhoneNum);
		intent.putExtra(RegisterActivity.EXTRA_PASSWORD, Md5Util.toMD5(password));
		startActivity(intent);
		finish();
	}

	@Override
	public void onBackPressed() {
		doBack();
	}

	private void doBack() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.prompt_title).setMessage(R.string.cancel_register_prompt)
				.setNegativeButton(getString(R.string.no), null).setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				});
		builder.create().show();
	}

}
