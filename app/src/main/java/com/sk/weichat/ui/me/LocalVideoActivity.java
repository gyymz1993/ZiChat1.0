package com.sk.weichat.ui.me;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore.Video.Thumbnails;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.roamer.slidelistview.SlideBaseAdapter;
import com.roamer.slidelistview.SlideListView;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.VideoFile;
import com.sk.weichat.db.dao.VideoFileDao;
import com.sk.weichat.ui.base.ActionBackActivity;
import com.sk.weichat.ui.tool.VideoPlayActivity;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.util.ViewHolder;
import com.sk.weichat.video.VideoRecordActivity;
import com.sk.weichat.view.PullToRefreshSlideListView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地视频选择界面
 * 
 * @author Dean Tao
 * @version 1.0
 */
public class LocalVideoActivity extends ActionBackActivity {

	private PullToRefreshSlideListView mPullToRefreshListView;
	private List<VideoFile> mVideoFiles;
	private LocalVideoAdapter mAdapter;

	private int mAction = AppConstant.ACTION_NONE;
	private Handler mHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getIntent() != null) {
			mAction = getIntent().getIntExtra(AppConstant.EXTRA_ACTION, AppConstant.ACTION_NONE);
		}
		setContentView(R.layout.layout_pullrefresh_list_slide);
		mHandler = new Handler();
		mVideoFiles = new ArrayList<VideoFile>();
		mAdapter = new LocalVideoAdapter(this);
		initView();

	}
	private void initView() {
		getSupportActionBar().setTitle(R.string.local_video);
		mPullToRefreshListView = (PullToRefreshSlideListView) findViewById(R.id.pull_refresh_list);

		View emptyView = LayoutInflater.from(mContext).inflate(R.layout.layout_list_empty_view, null);
		mPullToRefreshListView.setEmptyView(emptyView);

		mPullToRefreshListView.getRefreshableView().setAdapter(mAdapter);
		mPullToRefreshListView.setShowIndicator(false);

		mPullToRefreshListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<SlideListView>() {
			@Override
			public void onRefresh(PullToRefreshBase<SlideListView> refreshView) {
				loadData();
			}
		});

		mPullToRefreshListView.getRefreshableView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (mAction == AppConstant.ACTION_SELECT) {
					VideoFile videoFile = mVideoFiles.get((int) id);
					if (TextUtils.isEmpty(videoFile.getFilePath())) {
						ToastUtil.showToast(LocalVideoActivity.this, R.string.video_file_not_exist);
						return;
					}
					File file = new File(videoFile.getFilePath());
					if (!file.exists()) {
						ToastUtil.showToast(LocalVideoActivity.this, R.string.video_file_not_exist);
						return;
					}

					Intent intent = new Intent();
					intent.putExtra(AppConstant.EXTRA_SELECT_ID, videoFile.get_id());
					intent.putExtra(AppConstant.EXTRA_FILE_PATH, videoFile.getFilePath());
					intent.putExtra(AppConstant.EXTRA_TIME_LEN, videoFile.getFileLength());
					setResult(RESULT_OK, intent);
					finish();
				}
			}
		});

		mPullToRefreshListView.setAdapter(mAdapter);

		loadData();
	}

	private void loadData() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				long startTime = System.currentTimeMillis();
				final List<VideoFile> videos = VideoFileDao.getInstance().getVideoFiles(MyApplication.getInstance().mLoginUser.getUserId());
				long delayTime = 200 - (startTime - System.currentTimeMillis());// 保证至少200ms的刷新过程
				if (delayTime < 0) {
					delayTime = 0;
				}
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						mVideoFiles.clear();
						if (videos != null && videos.size() > 0) {
							mVideoFiles.addAll(videos);
						}
						mAdapter.notifyDataSetChanged();
						mPullToRefreshListView.onRefreshComplete();
					}
				}, delayTime);
			}
		}).start();
	}

	private boolean delete(VideoFile videoFile) {
		boolean success = true;
		String filePath = videoFile.getFilePath();
		if (!TextUtils.isEmpty(filePath)) {
			File file = new File(filePath);
			if (file.exists()) {
				success = file.delete();
			}
		}
		if (success) {
			mVideoFiles.remove(videoFile);
			VideoFileDao.getInstance().deleteVideoFile(videoFile);
			mAdapter.notifyDataSetChanged();
		}
		return success;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_add_icon, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.add_item) {
			startActivityForResult(new Intent(LocalVideoActivity.this, VideoRecordActivity.class), 1);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1 && resultCode == RESULT_OK) {
			if (data != null) {
				String filePath = data.getStringExtra(VideoRecordActivity.EXTRA_RESULT_FILE_PATH);
				long timeLen = data.getLongExtra(VideoRecordActivity.EXTRA_RESULT_TIME_LEN, 0);
				if (timeLen <= 0) {
					timeLen = 10 * 1000;// 数据出错，默认给个10s
				}
				long fizeSize = data.getLongExtra(VideoRecordActivity.EXTRA_RESULT_FILE_SIZE, 0);
				if (fizeSize <= 0) {
					fizeSize = 10 * 1024;// 数据出错，给个10k
				}
				VideoFile videoFile = new VideoFile();
				videoFile.setCreateTime(TimeUtils.f_long_2_str(System.currentTimeMillis()));
				videoFile.setFileLength(timeLen);
				videoFile.setFileSize(fizeSize);
				videoFile.setFilePath(filePath);
				videoFile.setOwnerId(MyApplication.getInstance().mLoginUser.getUserId());
				VideoFileDao.getInstance().addVideoFile(videoFile);
				mVideoFiles.add(0, videoFile);
				mAdapter.notifyDataSetChanged();
			}
		}
	}

	private class LocalVideoAdapter extends SlideBaseAdapter {

		public LocalVideoAdapter(Context context) {
			super(context);
		}

		@Override
		public int getCount() {
			return mVideoFiles.size();
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
			if (convertView == null) {
				convertView = createConvertView(position);
			}
			ImageView thumbnail_img = ViewHolder.get(convertView, R.id.thumbnail_img);
			TextView des_tv = ViewHolder.get(convertView, R.id.des_tv);
			TextView create_time_tv = ViewHolder.get(convertView, R.id.create_time_tv);
			TextView length_tv = ViewHolder.get(convertView, R.id.length_tv);
			TextView size_tv = ViewHolder.get(convertView, R.id.size_tv);

			TextView delete_tv = ViewHolder.get(convertView, R.id.delete_tv);

			/* 获取缩略图显示 */
			Bitmap bitmap = null;
			String videoUrl = mVideoFiles.get(position).getFilePath();
			if (!TextUtils.isEmpty(videoUrl)) {
				bitmap = ImageLoader.getInstance().getMemoryCache().get(videoUrl);
				if (bitmap == null || bitmap.isRecycled()) {
					bitmap = ThumbnailUtils.createVideoThumbnail(videoUrl, Thumbnails.MINI_KIND);
					if (bitmap != null) {
						ImageLoader.getInstance().getMemoryCache().put(videoUrl, bitmap);
					}
				}
			}
			if (bitmap != null && !bitmap.isRecycled()) {
				thumbnail_img.setImageBitmap(bitmap);
			} else {
				thumbnail_img.setImageBitmap(null);
			}

			thumbnail_img.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					VideoFile videoFile = mVideoFiles.get(position);
					if (videoFile == null) {
						return;
					}
					if (TextUtils.isEmpty(videoFile.getFilePath())) {
						ToastUtil.showToast(LocalVideoActivity.this, R.string.video_file_not_exist);
						return;
					}
					File file = new File(videoFile.getFilePath());
					if (!file.exists()) {
						ToastUtil.showToast(LocalVideoActivity.this, R.string.video_file_not_exist);
						delete(mVideoFiles.get(position));
						return;
					}
					Intent intent = new Intent(LocalVideoActivity.this, VideoPlayActivity.class);
					intent.putExtra(AppConstant.EXTRA_FILE_PATH, videoFile.getFilePath());
					startActivity(intent);
				}
			});

			/* 其他信息 */
			String des = mVideoFiles.get(position).getDesc();
			if (TextUtils.isEmpty(des)) {
				des_tv.setVisibility(View.GONE);
			} else {
				des_tv.setVisibility(View.VISIBLE);
				des_tv.setText(des);
			}
			create_time_tv.setText(mVideoFiles.get(position).getCreateTime());
			length_tv.setText(parserTimeLength(mVideoFiles.get(position).getFileLength()));
			size_tv.setText(parserFileSize(mVideoFiles.get(position).getFileSize()));

			delete_tv.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if (!delete(mVideoFiles.get(position))) {
						ToastUtil.showToast(LocalVideoActivity.this, R.string.delete_failed);
					}
				}
			});
			return convertView;
		}

		@Override
		public int getFrontViewId(int position) {
			return R.layout.row_local_video;
		}

		@Override
		public int getLeftBackViewId(int position) {
			return 0;
		}

		@Override
		public int getRightBackViewId(int position) {
			return R.layout.row_item_delete;
		}

	}

	private static String parserFileSize(long size) {
		float temp = size / (float) 1024;
		if (temp < 1024) {
			return (int) temp + "KB";
		}
		temp = temp / 1024;
		if (temp < 1024) {
			return ((int) (temp * 100)) / (float) 100 + "M";
		}
		temp = temp / 1024;
		return ((int) (temp * 100)) / (float) 100 + "G";
	}

	private String parserTimeLength(long length) {
		int intLength = (int) (length / 1000);// 毫秒级转换为秒
		int hour = intLength / 3600;
		int temp = intLength - (hour * 3600);
		int minute = temp / 60;
		temp = temp - (minute * 60);
		int second = temp;

		StringBuilder sb = new StringBuilder();
		if (hour != 0) {
			sb.append(hour < 10 ? ("0" + hour) : hour).append(getString(R.string.hour));
		}
		if (minute != 0) {
			sb.append(minute < 10 ? ("0" + minute) : minute).append(getString(R.string.minute));
		}
		if (second != 0) {
			sb.append(second < 10 ? ("0" + second) : second).append(getString(R.string.second));
		}
		return sb.toString();
	}

}
