package com.lsjr.zizi.mvp.home.photo;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.chat.utils.Scheme;
import com.lsjr.zizi.loader.ImageLoader;
import com.ymz.baselibrary.mvp.BasePresenter;

import java.io.File;

import butterknife.BindView;

/**
 * 单张图片预览
 *
 */
public class SingleImagePreviewActivity extends MvpActivity {

	String mImageUri;
	@BindView(R.id.image_view)
	ImageView mImageView;
	@BindView(R.id.progress_bar)
	ProgressBar mProgressBar;
	@BindView(R.id.progress_text_tv)
	TextView mProgressTextTv;
	@Override
	protected BasePresenter createPresenter() {
		return null;
	}


	@Override
	protected void initView() {
		// init status
		Scheme scheme = Scheme.ofUri(mImageUri);
		switch (scheme) {
			case HTTP:
			case HTTPS:// 需要网络加载的
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

		mProgressBar.setVisibility(View.VISIBLE);
		ImageLoader.getInstance().showRealWHImage(mImageUri, mImageView,mProgressBar);
	}

	@Override
	protected int getLayoutId() {
		return R.layout.activity_imageshow;
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		if (getIntent() != null) {
			mImageUri = getIntent().getStringExtra(AppConfig.EXTRA_IMAGE_URI);
		}
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void afterCreate(Bundle savedInstanceState) {

	}

}
