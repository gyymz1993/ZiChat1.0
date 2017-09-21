package com.sk.weichat.ui.me;

import android.os.Bundle;
import android.widget.TextView;

import com.sk.weichat.R;
import com.sk.weichat.ui.base.ActionBackActivity;
import com.sk.weichat.util.DeviceInfoUtil;

public class AboutActivity extends ActionBackActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);
		getSupportActionBar().setTitle(R.string.about_us);
		TextView versionTv = (TextView) findViewById(R.id.version_tv);
		String appName = getString(R.string.app_name);
		String versionName = DeviceInfoUtil.getVersionName(mContext);
		versionTv.setText(appName + " " + versionName);
	}

}
