package com.sk.weichat.ui.tool;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.sk.weichat.AppConstant;
import com.sk.weichat.R;
import com.sk.weichat.ui.base.ActionBackActivity;
import com.sk.weichat.util.Scheme;

/**
 * 图片集的预览
 * 
 * @author Dean Tao
 * @version 1.0
 */
public class MultiImagePreviewActivity extends ActionBackActivity {

	private ArrayList<String> mImages;
	private int mPosition;
	private boolean mChangeSelected;

	private ViewPager mViewPager;
	private CheckBox mCheckBox;
	private TextView mIndexCountTv;
	private List<Integer> mRemovePosition = new ArrayList<Integer>();

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getIntent() != null) {
			mImages = (ArrayList<String>) getIntent().getSerializableExtra(AppConstant.EXTRA_IMAGES);
			mPosition = getIntent().getIntExtra(AppConstant.EXTRA_POSITION, 0);
			mChangeSelected = getIntent().getBooleanExtra(AppConstant.EXTRA_CHANGE_SELECTED, false);
		}
		if (mImages == null) {
			mImages = new ArrayList<String>();
		}
		getSupportActionBar().hide();
		setContentView(R.layout.activity_images_preview);
		initView();
	}

	@Override
	public void onBackPressed() {
		doFinish();
	}

	@Override
	protected boolean onHomeAsUp() {
		doFinish();
		return true;
	}

	private void doFinish() {
		if (mChangeSelected) {
			Intent intent = new Intent();
			ArrayList<String> resultImages = null;
			if (mRemovePosition.size() == 0) {
				resultImages = mImages;
			} else {
				resultImages = new ArrayList<String>();
				for (int i = 0; i < mImages.size(); i++) {
					if (!isInRemoveList(i)) {
						resultImages.add(mImages.get(i));
					}
				}
			}
			intent.putExtra(AppConstant.EXTRA_IMAGES, resultImages);
			setResult(RESULT_OK, intent);
		}
		finish();
	}

	private void initView() {
		mViewPager = (ViewPager) findViewById(R.id.view_pager);
		mIndexCountTv = (TextView) findViewById(R.id.index_count_tv);
		mCheckBox = (CheckBox) findViewById(R.id.check_box);
		mViewPager.setPageMargin(10);

		mViewPager.setAdapter(new ImagesAdapter());

		updateSelectIndex(mPosition);

		if (mPosition < mImages.size()) {
			mViewPager.setCurrentItem(mPosition);
		}

		mViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageSelected(int arg0) {
				updateSelectIndex(arg0);
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});

	}

	public void updateSelectIndex(final int index) {
		if (mPosition >= mImages.size()) {
			mIndexCountTv.setText(null);
		} else {
			mIndexCountTv.setText((index + 1) + "/" + mImages.size());
		}

		if (!mChangeSelected) {
			mCheckBox.setVisibility(View.GONE);
			return;
		}

		mCheckBox.setOnCheckedChangeListener(null);
		boolean removed = isInRemoveList(index);
		if (removed) {
			mCheckBox.setChecked(false);
		} else {
			mCheckBox.setChecked(true);
		}
		mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					removeFromRemoveList(index);
				} else {
					addInRemoveList(index);
				}
			}
		});
	}

	SparseArray<View> mViews = new SparseArray<View>();

	void addInRemoveList(int position) {
		if (!isInRemoveList(position)) {
			mRemovePosition.add(Integer.valueOf(position));
		}
	}

	void removeFromRemoveList(int position) {
		if (isInRemoveList(position)) {
			mRemovePosition.remove(Integer.valueOf(position));
		}
	}

	boolean isInRemoveList(int position) {
		return mRemovePosition.indexOf(Integer.valueOf(position)) != -1;
	}

	class ImagesAdapter extends PagerAdapter {

		@Override
		public int getCount() {
			return mImages.size();
		}

		@Override
		public boolean isViewFromObject(View view, Object object) {
			return view == object;
		}

		@Override
		public Object instantiateItem(ViewGroup container, final int position) {
			View view = mViews.get(position);
			if (view == null) {
				view = new ImageView(MultiImagePreviewActivity.this);
				mViews.put(position, view);
				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						doFinish();
					}
				});
			}

			// init status
			String imageUrl = mImages.get(position);
			Scheme scheme = Scheme.ofUri(imageUrl);
			switch (scheme) {
			case HTTP:
			case HTTPS:// 需要网络加载的
				ImageLoader.getInstance().displayImage(imageUrl, (ImageView) view);
				break;
			case UNKNOWN:// 如果不知道什么类型，且不为空，就当做是一个本地文件的路径来加载
				if (!TextUtils.isEmpty(imageUrl)) {
					ImageLoader.getInstance().displayImage(Uri.fromFile(new File(imageUrl)).toString(), (ImageView) view);
				}
				break;
			default:
				// 其他 drawable asset类型不处理
				break;
			}
			container.addView(view);
			return view;
		}

		@Override
		public void destroyItem(ViewGroup container, int position, Object object) {
			View view = mViews.get(position);
			if (view == null) {
				super.destroyItem(container, position, object);
			} else {
				container.removeView(view);
			}
		}

	}

}
