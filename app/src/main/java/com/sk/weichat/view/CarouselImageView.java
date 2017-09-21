package com.sk.weichat.view;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.assist.ImageLoadingListener;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.util.AnimationUtil;

public class CarouselImageView extends ImageSwitcher {

	private ScaleType mScaleType = ScaleType.CENTER_CROP;
	private ImageSwitcherAware mImageAware;

	public CarouselImageView(Context context) {
		this(context, null);
	}
	
	
	public CarouselImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mImageAware = new ImageSwitcherAware(this);
		mImageAware.setViewScaleType(ImageSwitcherAware.fromImageViewScaleType(mScaleType));
		setFactory(new ViewFactory() {
			@Override
			public View makeView() {
				ImageView imageView = new ImageView(getContext());
				imageView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
				imageView.setScaleType(mScaleType);
				return imageView;
			}
		});
	}

	private String[] mImages;

	public void onResume() {
		mDownLoadHander.removeCallbacksAndMessages(null);
		mShowBitmapHander.removeCallbacksAndMessages(null);
		mDownLoadHander.sendEmptyMessage(0x1);
		mShowBitmapHander.sendEmptyMessage(0x1);
	}

	public void onStop() {
		mDownLoadHander.removeCallbacksAndMessages(null);
		mShowBitmapHander.removeCallbacksAndMessages(null);
	}

	public void setScaleType(ScaleType type){
		mScaleType=type;
		mImageAware.setViewScaleType(ImageSwitcherAware.fromImageViewScaleType(mScaleType));
		removeAllViews();
		setFactory(new ViewFactory() {
			@Override
			public View makeView() {
				ImageView imageView = new ImageView(getContext());
				imageView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
				imageView.setScaleType(mScaleType);
				return imageView;
			}
		});
	}
	
	public void setImages(String[] images) {
		mImages = images;
		if (mImages == null || mImages.length <= 0) {
			return;
		}
		mDownLoadHander.removeCallbacksAndMessages(null);
		mShowBitmapHander.removeCallbacksAndMessages(null);
		ImageLoader.getInstance().cancelDisplayTask(mImageAware);
		this.clearAnimation();

		mLoadBmpIndexs.clear();
		mCurrentIndex = 0;
		mLoadIndex = 0;

		mDownLoadHander.sendEmptyMessage(0x1);
		mShowBitmapHander.sendEmptyMessage(0x1);
	}

	private String mUserId;

	public void setUserId(String userId) {
		mUserId = userId;
	}

	private int mCurrentIndex = 0;
	private int mLoadIndex = 0;
	private List<Integer> mLoadBmpIndexs = new ArrayList<Integer>();

	private Handler mDownLoadHander = new Handler() {
		public void handleMessage(Message msg) {
			if (mImages == null || mLoadIndex >= mImages.length) {
				return;
			}
			String url = mImages[mLoadIndex];
			if (TextUtils.isEmpty(url)) {
				mLoadIndex++;
				mDownLoadHander.sendEmptyMessageDelayed(0x1, 100);
			}
			ImageLoader.getInstance().loadImage(url, new ImageLoadingListener() {
				@Override
				public void onLoadingStarted(String arg0, View arg1) {
				}

				@Override
				public void onLoadingFailed(String arg0, View arg1, FailReason arg2) {
					mLoadIndex++;
					mDownLoadHander.sendEmptyMessageDelayed(0x1, 100);
				}

				@Override
				public void onLoadingComplete(String arg0, View arg1, Bitmap arg2) {
					if (arg2 != null) {
						mLoadBmpIndexs.add(Integer.valueOf(mLoadIndex));
					}
					if (mLoadBmpIndexs.size() == 1) {// 完成第一张图加载的时候，立即显示
						mShowBitmapHander.removeCallbacksAndMessages(null);
						mShowBitmapHander.sendEmptyMessage(0x1);
					}
					mLoadIndex++;
					mDownLoadHander.sendEmptyMessageDelayed(0x1, 100);

				}

				@Override
				public void onLoadingCancelled(String arg0, View arg1) {
				}
			});

		}
	};

	// private int mCurrentAnimationInIndex = 1;

	private Handler mShowBitmapHander = new Handler() {
		public void handleMessage(Message msg) {
			Log.d("roamer", "mShowBitmapHander");
			if (mLoadBmpIndexs.size() == 0) {// 如果暂时没图片，那么就显示他的头像
				if (!TextUtils.isEmpty(mUserId)) {
					AvatarHelper.getInstance().displayAvatar(mUserId, mImageAware, false);
				}
				return;
			}
			
			if (mLoadBmpIndexs.size() == 1) {// 如果只有一张图
				clearAnimation();
				ImageLoader.getInstance().displayImage(mImages[mLoadBmpIndexs.get(mCurrentIndex)], mImageAware);
				mShowBitmapHander.sendEmptyMessageDelayed(0x1, 10000);
				return;
			}

			if (mCurrentIndex >= mLoadBmpIndexs.size()) {
				mCurrentIndex = 0;
				mShowBitmapHander.sendEmptyMessageDelayed(0x1, 5000);
				return;
			} else {
				if (msg.what == 0x1) {// 显示xiayiz
					setOutAnimation(AnimationUtil.getAnimation(AnimationUtil.getNextOutAnimationIndex()));
					setInAnimation(AnimationUtil.getAnimation(AnimationUtil.getNextInAnimationIndex()));
				} else if (msg.what == 0x2) {
					setOutAnimation(AnimationUtil.getAnimation(AnimationUtil.getPreviousOutAnimationIndex()));
					setInAnimation(AnimationUtil.getAnimation(AnimationUtil.getPreviousInAnimationIndex()));
				}
				ImageLoader.getInstance().displayImage(mImages[mLoadBmpIndexs.get(mCurrentIndex)], mImageAware);
				mCurrentIndex++;
				mShowBitmapHander.sendEmptyMessageDelayed(0x1, 10000);
			}
		}
	};
	

	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		mDownLoadHander.removeCallbacksAndMessages(null);
		mShowBitmapHander.removeCallbacksAndMessages(null);
	}

	public void displayNext() {
		if (mLoadBmpIndexs.size() <= 1) {
			return;
		}
		mShowBitmapHander.removeCallbacksAndMessages(null);
		mShowBitmapHander.sendEmptyMessage(0x1);
	}

	public void displayPrevious() {
		if (mLoadBmpIndexs.size() <= 1) {
			return;
		}
		mShowBitmapHander.removeCallbacksAndMessages(null);
		mCurrentIndex=mCurrentIndex-2;
		if (mCurrentIndex < 0) {
			mCurrentIndex = mLoadBmpIndexs.size() - 1;
		}
		mShowBitmapHander.sendEmptyMessage(0x2);
	}

}
