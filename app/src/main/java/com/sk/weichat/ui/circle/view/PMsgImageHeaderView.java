package com.sk.weichat.ui.circle.view;

import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView.ScaleType;

import com.sk.weichat.R;
import com.sk.weichat.bean.circle.PublicMessage;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.view.CarouselImageView;

public class PMsgImageHeaderView extends PMsgTypeView {
	private CarouselImageView mCarouselImageView;
	private MediaControlView mMediaControlView;
	private PublicMessage mPublicMessage;

	public PMsgImageHeaderView(Context context) {
		super(context);
		init();
	}

	public PMsgImageHeaderView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	@SuppressLint("NewApi")
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public PMsgImageHeaderView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		LayoutInflater.from(getContext()).inflate(R.layout.header_view_p_msg_image, this);
		mCarouselImageView = (CarouselImageView) findViewById(R.id.carousel_img_view);
		mCarouselImageView.setScaleType(ScaleType.FIT_CENTER);
		mMediaControlView = (MediaControlView) findViewById(R.id.media_control_view);

		mMediaControlView.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				return true;
			}
		});

		initImageControl();
	}

	private void initImageControl() {
		mCarouselImageView.setOnClickListener(mMediaControlViewVisibilityListener);
		mMediaControlView.setSeekBarEnable(false);// 不可滑动
		mMediaControlView.setOnPreClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});
		mMediaControlView.setOnNextClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});
		mMediaControlView.setOnPlayPauseClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

			}
		});

	}

	/** 控制 */
	private View.OnClickListener mMediaControlViewVisibilityListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (isControllerShowing()) {
				hide();
			} else {
				show();
			}
		}
	};

	private boolean isControllerShowing() {
		return mMediaControlView.getVisibility() == View.VISIBLE;
	}

	/**
	 * Remove the controller from the screen.
	 */
	public void hide() {
		if (isControllerShowing()) {
			mMediaControlView.setVisibility(View.GONE);
		}
	}

	private static final int sDefaultTimeout = 8000;

	public void show() {
		show(sDefaultTimeout);
	}

	public void show(int timeout) {
		mMediaControlView.setVisibility(View.VISIBLE);
		Message msg = mHandler.obtainMessage(FADE_OUT);
		if (timeout != 0) {
			mHandler.removeMessages(FADE_OUT);
			mHandler.sendMessageDelayed(msg, timeout);
		}
	}

	private static final int FADE_OUT = 1;
	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case FADE_OUT:
				hide();
				break;
			}
		}
	};

	@Override
	public void attachPublicMessage(PublicMessage message) {
		mPublicMessage = message;
		if (mPublicMessage == null) {
			return;
		}
		int type = mPublicMessage.getType();
		List<String> imageList = new ArrayList<String>();
		if (type == PublicMessage.TYPE_IMG) {
			if (mPublicMessage.getBody() != null && mPublicMessage.getBody().getImages() != null && mPublicMessage.getBody().getImages().size() > 0) {
				for (int i = 0; i < mPublicMessage.getBody().getImages().size(); i++) {
					imageList.add(mPublicMessage.getBody().getImages().get(i).getOriginalUrl());
				}
			}
		}

		if (imageList.size() <= 0) {
			imageList.add(AvatarHelper.getAvatarUrl(mPublicMessage.getUserId(), false));
		}

		String[] images=new String[imageList.size()];
		for (int i = 0; i < images.length; i++) {
			images[i]=imageList.get(i);
		}
		mCarouselImageView.setImages(images);
	}

	@Override
	public void onPause() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDestory() {
		// TODO Auto-generated method stub

	}

}
