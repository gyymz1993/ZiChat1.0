package com.sk.weichat.ui.tool;

import java.io.File;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.sk.weichat.AppConstant;
import com.sk.weichat.R;
import com.sk.weichat.ui.base.ActionBackActivity;
import com.sk.weichat.util.Scheme;

/**
 * 单张图片预览
 * 
 * @author Dean Tao
 * @version 1.0
 */
public class SingleImagePreviewActivity extends ActionBackActivity {

	private String mImageUri;

	private ImageView mImageView;
	private ProgressBar mProgressBar;
	@SuppressWarnings("unused")
	private TextView mProgressTextTv;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getIntent() != null) {
			mImageUri = getIntent().getStringExtra(AppConstant.EXTRA_IMAGE_URI);
		}
		getSupportActionBar().hide();
		setContentView(R.layout.activity_single_image_preview);

		initView();
	}

	private void initView() {
		mImageView = (ImageView) findViewById(R.id.image_view);
		mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);
		mProgressTextTv = (TextView) findViewById(R.id.progress_text_tv);

		boolean showProgress = false;
		// init status
		Scheme scheme = Scheme.ofUri(mImageUri);
		switch (scheme) {
		case HTTP:
		case HTTPS:// 需要网络加载的
			Bitmap bitmap = ImageLoader.getInstance().getMemoryCache().get(mImageUri);
			if (bitmap == null || bitmap.isRecycled()) {
				File file = ImageLoader.getInstance().getDiscCache().get(mImageUri);
				if (file == null || !file.exists()) {// 文件不存在，那么就表示需要重新下载
					showProgress = true;
				}
			}
			break;
		case UNKNOWN:// 如果不知道什么类型，且不为空，就当做是一个本地文件的路径来加载
			if (TextUtils.isEmpty(mImageUri)) {
				mImageUri = "";
			} else {
				mImageUri = Uri.fromFile(new File(mImageUri)).toString();
			}
			break;
		default:
			// 其他 drawable asset类型不处理
			break;
		}

		if (showProgress) {
			ImageLoader.getInstance().displayImage(mImageUri, mImageView, mImageLoadingListener);
		} else {
			ImageLoader.getInstance().displayImage(mImageUri, mImageView);
		}

		mImageView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
				overridePendingTransition(0, R.anim.alpha_scale_out);
			}
		});
	}

	@Override
	public void onBackPressed() {
		finish();
		overridePendingTransition(0, R.anim.alpha_scale_out);
	}

	@Override
	protected boolean onHomeAsUp() {
		finish();
		overridePendingTransition(0, R.anim.alpha_scale_out);
		return true;
	}

	private ImageLoadingListener mImageLoadingListener = new ImageLoadingListener() {
		@Override
		public void onLoadingStarted(String arg0, View arg1) {
			mProgressBar.setVisibility(View.VISIBLE);
		}

		@Override
		public void onLoadingFailed(String arg0, View arg1, FailReason arg2) {
			mProgressBar.setVisibility(View.GONE);
		}

		@Override
		public void onLoadingComplete(String arg0, View arg1, Bitmap arg2) {
			mProgressBar.setVisibility(View.GONE);
		}

		@Override
		public void onLoadingCancelled(String arg0, View arg1) {
			mProgressBar.setVisibility(View.GONE);
		}
	};

}
