package com.sk.weichat.view;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageSwitcher;
import android.widget.ImageView.ScaleType;

import com.nostra13.universalimageloader.core.assist.ViewScaleType;

public class ImageSwitcherAware extends ViewAware {

	private ViewScaleType mScaleType;

	public ImageSwitcherAware(ImageSwitcher imageSwitcher) {
		super(imageSwitcher);
	}

	public ImageSwitcherAware(ImageSwitcher imageSwitcher, boolean checkActualViewSize) {
		super(imageSwitcher, checkActualViewSize);
	}

	@Override
	public ViewScaleType getScaleType() {
		if (mScaleType == null) {
			return super.getScaleType();
		}
		return mScaleType;
	}

	public void setViewScaleType(ViewScaleType viewScaleType) {
		mScaleType = viewScaleType;
	}

	@Override
	public ImageSwitcher getWrappedView() {
		return (ImageSwitcher) super.getWrappedView();
	}

	@Override
	protected void setImageDrawableInto(Drawable drawable, View view) {
		if (drawable == null) {
			return;
		}
		((ImageSwitcher) view).setImageDrawable(drawable);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void setImageBitmapInto(Bitmap bitmap, View view) {
		if (bitmap == null) {
			return;
		}
		((ImageSwitcher) view).setImageDrawable(new BitmapDrawable(bitmap));
	}

	public static ViewScaleType fromImageViewScaleType(ScaleType scaleType) {
		switch (scaleType) {
		case FIT_CENTER:
		case FIT_XY:
		case FIT_START:
		case FIT_END:
		case CENTER_INSIDE:
			return ViewScaleType.FIT_INSIDE;
		case MATRIX:
		case CENTER:
		case CENTER_CROP:
		default:
			return ViewScaleType.CROP;
		}
	}

}