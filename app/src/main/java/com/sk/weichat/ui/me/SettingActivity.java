package com.sk.weichat.ui.me;

import java.io.File;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.helper.LoginHelper;
import com.sk.weichat.sp.UserSp;
import com.sk.weichat.ui.base.ActionBackActivity;
import com.sk.weichat.ui.tool.WebViewActivity;
import com.sk.weichat.util.GetFileSizeUtil;
import com.sk.weichat.util.ToastUtil;

/**
 * 设置
 */
public class SettingActivity extends ActionBackActivity implements View.OnClickListener {
	private Button mExitBtn;
	private TextView mCacheTv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setting);
		getSupportActionBar().setTitle(R.string.setting);
		initView();
	}

	private void initView() {
		mExitBtn = (Button) findViewById(R.id.exit_btn);
		mExitBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showExitDialog();
			}
		});
		mCacheTv = (TextView) findViewById(R.id.cache_tv);
		findViewById(R.id.clear_cache_rl).setOnClickListener(this);
		findViewById(R.id.use_help_rl).setOnClickListener(this);
		findViewById(R.id.about_us_rl).setOnClickListener(this);

		long cacheSize = GetFileSizeUtil.getFileSize(new File(MyApplication.getInstance().mAppDir));
		mCacheTv.setText(GetFileSizeUtil.formatFileSize(cacheSize));
	}

	private void showExitDialog() {
		new AlertDialog.Builder(mContext).setTitle(R.string.app_name).setMessage(R.string.exit_tips).setNegativeButton(R.string.cancel, null)
				.setPositiveButton(R.string.sure, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						UserSp.getInstance(mContext).clearUserInfo();
						LoginHelper.broadcastLogout(mContext);
					}
				}).create().show();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.clear_cache_rl:
			clearCache();
			break;
		case R.id.use_help_rl: {// 使用帮助
			Intent intent = new Intent(mContext, WebViewActivity.class);
			intent.putExtra(WebViewActivity.EXTRA_URL, mConfig.help_url);
			intent.putExtra(WebViewActivity.EXTRA_TITLE, getString(R.string.use_help));
			startActivity(intent);
		}
			break;
		case R.id.about_us_rl:// 关于我们
			startActivity(new Intent(mContext, AboutActivity.class));
			break;
		}
	}

	private void clearCache() {
		String filePath = MyApplication.getInstance().mAppDir;
		new ClearCacheAsyncTaska(filePath).execute(true);
	}

	private class ClearCacheAsyncTaska extends AsyncTask<Boolean, String, Integer> {

		private File rootFile;
		private ProgressDialog progressDialog;

		private int filesNumber = 0;
		private boolean canceled = false;

		public ClearCacheAsyncTaska(String filePath) {
			this.rootFile = new File(filePath);
		}

		@Override
		protected void onPreExecute() {
			filesNumber = GetFileSizeUtil.getFolderSubFilesNumber(rootFile);

			progressDialog = new ProgressDialog(mContext);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setIndeterminate(false);
			progressDialog.setCancelable(false);
			progressDialog.setMessage(getString(R.string.deleteing));
			progressDialog.setMax(filesNumber);
			progressDialog.setProgress(0);
			// 设置取消按钮
			progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int i) {
					canceled = true;
				}
			});
			progressDialog.show();
		}

		/**
		 * 返回true代表删除完成，false表示取消了删除
		 */
		@Override
		protected Integer doInBackground(Boolean... params) {
			if (filesNumber == 0) {
				return 0;
			}
			boolean deleteSubFolder = params[0];// 是否删除已清空的子文件夹
			return deleteFolder(rootFile, true, deleteSubFolder, 0);
		}

		@Override
		protected void onProgressUpdate(String... values) {
			super.onProgressUpdate(values);
			// String filePath = values[0];
			int progress = Integer.parseInt(values[1]);
			// progressDialog.setMessage(filePath);
			progressDialog.setProgress(progress);
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			progressDialog.dismiss();
			if (!canceled && result == filesNumber) {
				ToastUtil.showToast(mContext, R.string.clear_completed);
			}
			long cacheSize = GetFileSizeUtil.getFileSize(rootFile);
			mCacheTv.setText(GetFileSizeUtil.formatFileSize(cacheSize));
		}

		private long notifyTime = 0;

		/**
		 * 是否删除完毕
		 * @param file
		 * @param deleteSubFolder
		 * @return
		 */
		private int deleteFolder(File file, boolean rootFolder, boolean deleteSubFolder, int progress) {
			if (file == null || !file.exists() || !file.isDirectory()) {
				return 0;
			}
			File flist[] = file.listFiles();
			for (File subFile : flist) {
				if (canceled) {
					return progress;
				}
				if (subFile.isFile()) {
					subFile.delete();
					progress++;
					long current = System.currentTimeMillis();
					if (current - notifyTime > 200) {// 200毫秒更新一次界面
						notifyTime = current;
						publishProgress(subFile.getAbsolutePath(), String.valueOf(progress));
					}
				} else {
					progress = deleteFolder(subFile, false, deleteSubFolder, progress);
					if (deleteSubFolder) {
						subFile.delete();
					}
				}
			}
			return progress;
		}
	}

}
