package com.sk.weichat.ui.nearby;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sk.weichat.R;
import com.sk.weichat.ui.base.ActionBackActivity;
import com.sk.weichat.ui.tool.SelectConstantActivity;
import com.sk.weichat.ui.tool.SelectDateActivity;
import com.sk.weichat.util.ToastUtil;

/**
 * 搜索职位筛选界面
 * 
 * @author Dean Tao
 * 
 */
public class UserSearchActivity extends ActionBackActivity implements View.OnClickListener {

	private int mSex;
	private int mMinAge;
	private int mMaxAge;
	private int mShowTime;

	private EditText mKeyWordEdit;
	private TextView mSexTv;
	private EditText mMinAgeEdit;
	private EditText mMaxAgeEdit;
	private TextView mShowTimeTv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setTitle(R.string.search);
		setContentView(R.layout.activity_user_search);
		initView();
	}

	private void initView() {
		mKeyWordEdit = (EditText) findViewById(R.id.keyword_edit);
		mSexTv = (TextView) findViewById(R.id.sex_tv);
		mMinAgeEdit = (EditText) findViewById(R.id.min_age_edit);
		mMaxAgeEdit = (EditText) findViewById(R.id.max_age_edit);
		mShowTimeTv = (TextView) findViewById(R.id.show_time_tv);

		findViewById(R.id.sex_rl).setOnClickListener(this);
		findViewById(R.id.show_time_rl).setOnClickListener(this);
		findViewById(R.id.search_btn).setOnClickListener(this);
		reset();
	}

	private void reset() {
		mSex = 0;
		mMinAge = 0;
		mMaxAge = 200;
		mShowTime = 0;
		mKeyWordEdit.setText(null);
		mSexTv.setText(R.string.all);
		mMinAgeEdit.setText(String.valueOf(mMinAge));
		mMaxAgeEdit.setText(String.valueOf(mMaxAge));
		mShowTimeTv.setText(R.string.all_date);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_user_search, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.reset) {
			reset();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.sex_rl:// 点击性别
			showSelectSexDialog();
			break;
		case R.id.show_time_rl:
			startActivityForResult(new Intent(mContext, SelectDateActivity.class), 1);
			break;
		case R.id.search_btn: {
			mMinAge=Integer.parseInt(mMinAgeEdit.getText().toString());
			mMaxAge=Integer.parseInt(mMaxAgeEdit.getText().toString());
			if(mMinAge>mMaxAge){
				Toast.makeText(getApplicationContext(), "您输入的年龄范围不合法!",Toast.LENGTH_LONG).show();
				return;
			}
			Intent intent = new Intent(mContext, UserListActivity.class);
			intent.putExtra("key_word", mKeyWordEdit.getText().toString());
			intent.putExtra("sex", mSex);
			intent.putExtra("min_age", mMinAge);
			intent.putExtra("max_age", mMaxAge);
			intent.putExtra("show_time", mShowTime);
			startActivity(intent);
		}
			break;
		}
	}

	private void showSelectSexDialog() {
		// 1是男，0是女，2是全部
		String[] sexs = new String[] { getString(R.string.all), getString(R.string.sex_man), getString(R.string.sex_woman) };
		int checkItem = 0;
		if (mSex == 2) {
			checkItem = 0;
		} else if (mSex == 1) {
			mSex = 1;
		} else if (mSex == 0) {
			mSex = 2;
		}
		new AlertDialog.Builder(this).setTitle(getString(R.string.select_sex))
				.setSingleChoiceItems(sexs, checkItem, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == 0) {
							mSex = 2;
							mSexTv.setText(R.string.all);
						} else if (which == 1) {
							mSex = 1;
							mSexTv.setText(R.string.sex_man);
						} else {
							mSex = 0;
							mSexTv.setText(R.string.sex_woman);
						}
						dialog.dismiss();
					}
				}).setCancelable(true).create().show();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 1) {// 日期选择
			if (resultCode == RESULT_OK && data != null) {
				int id = data.getIntExtra(SelectConstantActivity.EXTRA_CONSTANT_ID, 0);
				String name = data.getStringExtra(SelectConstantActivity.EXTRA_CONSTANT_NAME);
				mShowTime = id;
				mShowTimeTv.setText(name);
			}
		}
	}
}
