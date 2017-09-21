package com.sk.weichat.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.helper.LoginHelper;
import com.sk.weichat.sp.UserSp;
import com.sk.weichat.ui.account.LoginActivity;
import com.sk.weichat.ui.account.LoginHistoryActivity;
import com.sk.weichat.ui.base.ActionBackActivity;
import com.sk.weichat.ui.base.ActivityStack;

/**
 * 进入到此界面的Activity只可能是4中用户状态 STATUS_USER_TOKEN_OVERDUE //本地Token过期 STATUS_USER_NO_UPDATE //数据不完整
 * 
 * 
 */
public class UserCheckedActivity extends ActionBackActivity {

	private TextView mTitleTv;
	private TextView mDesTv;
	private Button mLeftBtn;
	private Button mRightBtn;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_user_checked);
		
		// Api 11之后，点击外部会使得Activity结束，禁止外部点击结束
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			setFinishOnTouchOutside(false);
		}

		initView();
	}

	private void initView() {
		mTitleTv = (TextView) findViewById(R.id.title_tv);
		mDesTv = (TextView) findViewById(R.id.des_tv);
		mLeftBtn = (Button) findViewById(R.id.left_btn);
		mRightBtn = (Button) findViewById(R.id.right_btn);
		// init status

		// 能进入此Activity的只允许三种用户状态
		int status = MyApplication.getInstance().mUserStatus;
		if (status == LoginHelper.STATUS_USER_TOKEN_OVERDUE) {
			mTitleTv.setText(R.string.overdue_title);
			mDesTv.setText(R.string.token_overdue_des);
		} else if (status == LoginHelper.STATUS_USER_NO_UPDATE) {
			mTitleTv.setText(R.string.overdue_title);
			mDesTv.setText(R.string.deficiency_data_des);
		} else if (status == LoginHelper.STATUS_USER_TOKEN_CHANGE) {
			mTitleTv.setText(R.string.logout_title);
			mDesTv.setText(R.string.logout_des);
		} else {// 其他的状态，一般不会出现，为了容错，加个判断
			loginAgain();
			return;
			// throw new IllegalStateException("用户状态错误");
		}

		mLeftBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ActivityStack.getInstance().exit();
			}
		});

		mRightBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				loginAgain();
			}
		});
	}

	private void loginAgain() {
		boolean idIsEmpty = TextUtils.isEmpty(UserSp.getInstance(this).getUserId(""));
		boolean telephoneIsEmpty = TextUtils.isEmpty(UserSp.getInstance(this).getTelephone(null));
		if (!idIsEmpty && !telephoneIsEmpty) {//
			startActivity(new Intent(this, LoginHistoryActivity.class));
		} else {
			startActivity(new Intent(this, LoginActivity.class));
		}
		finish();
		overridePendingTransition(0, 0);
	}

	@Override
	public void onBackPressed() {
		loginAgain();
	}

}
