package com.sk.weichat.adapter;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView.ScaleType;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.sk.weichat.bean.circle.PublicMessage.Resource;
import com.sk.weichat.view.SquareCenterImageView;

public class ImagesInnerGridViewAdapter extends BaseAdapter {

	private Context mContext;
	private List<Resource> mDatas;

	public ImagesInnerGridViewAdapter(Context context, List<Resource> datas) {
		mContext = context;
		mDatas = datas;
	}

	@Override
	public int getCount() {
		if (mDatas.size() >= 9) {
			return 9;
		}
		return mDatas.size();
	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final SquareCenterImageView imageView = new SquareCenterImageView(mContext);
		imageView.setScaleType(ScaleType.CENTER_CROP);
		ImageLoader.getInstance().displayImage(mDatas.get(position).getOriginalUrl(), imageView);
//		imageView.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				Intent intent = new Intent(BusinessCircleActivity.this, SpaceImageDetailActivity.class);
//				intent.putExtra("images", (ArrayList<String>) mDatas);
//				intent.putExtra("position", position);
//				int[] location = new int[2];
//				imageView.getLocationOnScreen(location);
//				intent.putExtra("locationX", location[0]);
//				intent.putExtra("locationY", location[1]);
//
//				Log.d("roamer", "screenX:" + location[0]);
//				Log.d("roamer", "screenY:" + location[1]);
//				imageView.getLocationInWindow(location);
//
//				Log.d("roamer", "windowX:" + location[0]);
//				Log.d("roamer", "windowY:" + location[1]);
//
//				intent.putExtra("width", imageView.getWidth());
//				intent.putExtra("height", imageView.getHeight());
//				startActivity(intent);
//				overridePendingTransition(0, 0);
//			}
//		});
		return imageView;
	}

}