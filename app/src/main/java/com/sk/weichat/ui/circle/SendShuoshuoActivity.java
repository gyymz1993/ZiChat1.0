package com.sk.weichat.ui.circle;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.Area;
import com.sk.weichat.bean.UploadFileResult;
import com.sk.weichat.helper.LoginHelper;
import com.sk.weichat.helper.UploadService;
import com.sk.weichat.ui.account.LoginHistoryActivity;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.tool.MultiImagePreviewActivity;
import com.sk.weichat.util.CameraUtil;
import com.sk.weichat.util.DeviceInfoUtil;
import com.sk.weichat.util.ProgressDialogUtil;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.MyGridView;
import com.sk.weichat.view.SquareCenterImageView;
import com.sk.weichat.volley.ObjectResult;
import com.sk.weichat.volley.Result;
import com.sk.weichat.volley.StringJsonObjectRequest;
import com.ymz.baselibrary.utils.L_;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 发布一条说说的Activity
 * 
 * 
 */
public class SendShuoshuoActivity extends BaseActivity {
	private static final int REQUEST_CODE_CAPTURE_PHOTO = 1;// 拍照
	private static final int REQUEST_CODE_PICK_PHOTO = 2;// 图库
	private Uri mNewPhotoUri;// 拍照和图库 获得图片的URI

	private EditText mTextEdit;
	private TextView mSelectImagePromptTv;
	private View mSelectImgLayout;
	private MyGridView mGridView;
	private Button mReleaseBtn;

	private ArrayList<String> mPhotoList;
	private GridViewAdapter mAdapter;
	private String mImageData;

	private int mType;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_send_shuoshuo);
		if (getIntent() != null) {
			mType = getIntent().getIntExtra("type", 0);
		}
		mPhotoList = new ArrayList<String>();
		mAdapter = new GridViewAdapter();
		mProgressDialog = ProgressDialogUtil.init(this, null, getString(R.string.please_wait));
		initView();
	}

	private void initView() {
		if (mType == 0) {
			getSupportActionBar().setTitle(R.string.send_text);
		} else {
			getSupportActionBar().setTitle(R.string.send_image);
		}
		mTextEdit = (EditText) findViewById(R.id.text_edit);
		mSelectImagePromptTv = (TextView) findViewById(R.id.select_img_prompt_tv);
		mSelectImgLayout = findViewById(R.id.select_img_layout);
		mGridView = (MyGridView) findViewById(R.id.grid_view);
		mReleaseBtn = (Button) findViewById(R.id.release_btn);
//       ToastUtil.addEditTextNumChanged(SendShuoshuoActivity.this, mTextEdit, 200);//这里复制粘贴过多字数会在有些机型上出现bug
		mGridView.setAdapter(mAdapter);

		if (mType == 0) {
			mSelectImagePromptTv.setVisibility(View.GONE);
			mSelectImgLayout.setVisibility(View.GONE);
		}

		mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				int viewType = mAdapter.getItemViewType(position);

				if (viewType == 1) {
					showSelectPictureDialog();
				} else {
					showPictureActionDialog(position);
				}
			}
		});

		mReleaseBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mPhotoList.size() <= 0 && TextUtils.isEmpty(mTextEdit.getText().toString())) {// 没有照片，也没有说说，直接返回
					return;
				}
				if (mPhotoList.size() <= 0) {// 发文字
					sendShuoshuo();
				} else {// 布图片+文字
					new UploadPhpto().execute();
				}

			}
		});
	}

	private void showPictureActionDialog(final int position) {
		String[] items = new String[] { getString(R.string.look_over), getString(R.string.delete) };
		AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.pictures)
				.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == 0) {// 查看
							Intent intent = new Intent(SendShuoshuoActivity.this, MultiImagePreviewActivity.class);
							intent.putExtra(AppConstant.EXTRA_IMAGES, mPhotoList);
							intent.putExtra(AppConstant.EXTRA_POSITION, position);
							intent.putExtra(AppConstant.EXTRA_CHANGE_SELECTED, false);
							startActivity(intent);
						} else {// 删除
							deletePhoto(position);
						}
						dialog.dismiss();
					}
				});
		builder.show();
	}

	private void deletePhoto(final int position) {
		mPhotoList.remove(position);
		mAdapter.notifyDataSetInvalidated();
	}

	private void showSelectPictureDialog() {
		String[] items = new String[] { getString(R.string.c_take_picture), getString(R.string.c_photo_album) };
		AlertDialog.Builder builder = new AlertDialog.Builder(this).setSingleChoiceItems(items, 0,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == 0) {
							takePhoto();
						} else {
							selectPhoto();
						}
						dialog.dismiss();
					}
				});
		builder.show();
	}

	private void takePhoto() {
		mNewPhotoUri = CameraUtil.getOutputMediaFileUri(this, CameraUtil.MEDIA_TYPE_IMAGE);
		CameraUtil.captureImage(this, mNewPhotoUri, REQUEST_CODE_CAPTURE_PHOTO);
	}

	private void selectPhoto() {
		CameraUtil.pickImageSimple(this, REQUEST_CODE_PICK_PHOTO);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE_CAPTURE_PHOTO) {// 拍照返回
			if (resultCode == Activity.RESULT_OK) {
				if (mNewPhotoUri != null) {
					mPhotoList.add(mNewPhotoUri.getPath());
					mAdapter.notifyDataSetInvalidated();
				} else {
					ToastUtil.showToast(this, R.string.c_take_picture_failed);
				}
			}
		} else if (requestCode == REQUEST_CODE_PICK_PHOTO) {// 选择一张图片,然后立即调用裁减
			if (resultCode == Activity.RESULT_OK) {
				if (data != null && data.getData() != null) {
					String path = CameraUtil.getImagePathFromUri(this, data.getData());
					mPhotoList.add(path);
					mAdapter.notifyDataSetInvalidated();
				} else {
					ToastUtil.showToast(this, R.string.c_photo_album_failed);
				}
			}
		}

	}

	// 发布一条说说
	public void sendShuoshuo() {
		Map<String, String> params = new HashMap<String, String>();

		params.put("access_token", MyApplication.getInstance().mAccessToken);

		// 消息类型：1=文字消息；2=图文消息；3=语音消息；4=视频消息；
		if (TextUtils.isEmpty(mImageData)) {
			params.put("type", "1");
		} else {
			params.put("type", "2");
		}

		// 消息标记：1：求职消息；2：招聘消息；3：普通消息；
		params.put("flag", "3");

		// 消息隐私范围 0=不可见；1=朋友可见；2=粉丝可见；3=广场
		params.put("visible", "3");

		params.put("text", mTextEdit.getText().toString());// 消息内容

		if (!TextUtils.isEmpty(mImageData)) {
			params.put("images", mImageData);
		}

		// 附加信息
		params.put("model", DeviceInfoUtil.getModel());
		params.put("osVersion", DeviceInfoUtil.getOsVersion());
		params.put("serialNumber", DeviceInfoUtil.getDeviceId(mContext));

		double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
		double longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();

		if (latitude != 0)
			params.put("latitude", String.valueOf(latitude));
		if (longitude != 0)
			params.put("longitude", String.valueOf(longitude));

		String address = MyApplication.getInstance().getBdLocationHelper().getAddress();
		if (!TextUtils.isEmpty(address))
			params.put("location", address);

		Area area = Area.getDefaultCity();
		if (area != null) {
			params.put("cityId", String.valueOf(area.getId()));// 城市Id
		} else {
			params.put("cityId", "0");
		}

		ProgressDialogUtil.show(mProgressDialog);
		StringJsonObjectRequest<String> request = new StringJsonObjectRequest<String>(mConfig.MSG_ADD_URL,
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						ToastUtil.showErrorNet(SendShuoshuoActivity.this);
						ProgressDialogUtil.dismiss(mProgressDialog);
					}
				}, new StringJsonObjectRequest.Listener<String>() {
					@Override
					public void onResponse(ObjectResult<String> result) {
						boolean parserResult = Result.defaultParser(SendShuoshuoActivity.this, result, true);
						if (parserResult) {
							Intent intent = new Intent();
							intent.putExtra(AppConstant.EXTRA_MSG_ID, result.getData());
							setResult(RESULT_OK, intent);
							finish();
						}
						ProgressDialogUtil.dismiss(mProgressDialog);
					}
				}, String.class, params);
		addDefaultRequest(request);
	}

	private ProgressDialog mProgressDialog;

	private class UploadPhpto extends AsyncTask<Void, Integer, Integer> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			ProgressDialogUtil.show(mProgressDialog);
		}

		/**
		 * 上传的结果： <br/>
		 * return 1 Token过期，请重新登陆 <br/>
		 * return 2 上传出错<br/>
		 * return 3 上传成功<br/>
		 */
		@Override
		protected Integer doInBackground(Void... params) {
			if (!LoginHelper.isTokenValidation()) {
				return 1;
			}
			Map<String, String> mapParams = new HashMap<String, String>();
			mapParams.put("userId", MyApplication.getInstance().mLoginUser.getUserId() + "");
			mapParams.put("access_token", MyApplication.getInstance().mAccessToken);

			String result = new UploadService().uploadFile(mConfig.UPLOAD_URL, mapParams, mPhotoList);
			Log.d("roamer", "上传图片消息：" + result);
			if (TextUtils.isEmpty(result)) {
				return 2;
			}

			UploadFileResult recordResult = JSON.parseObject(result, UploadFileResult.class);
			boolean success = Result.defaultParser(SendShuoshuoActivity.this, recordResult, true);
			if (success) {
				if (recordResult.getSuccess() != recordResult.getTotal()) {// 上传丢失了某些文件
					return 2;
				}
				if (recordResult.getData() != null) {
					L_.e("------------->"+recordResult.getData());
					UploadFileResult.Data data = recordResult.getData();
					if (data.getImages() != null && data.getImages().size() > 0) {
						mImageData = JSON.toJSONString(data.getImages(), UploadFileResult.sImagesFilter);
					}

					Log.d("roamer", "mImageData:" + mImageData);
					return 3;
				} else {// 没有文件数据源，失败
					return 2;
				}
			} else {
				return 2;
			}
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			if (result == 1) {
				ProgressDialogUtil.dismiss(mProgressDialog);
				startActivity(new Intent(SendShuoshuoActivity.this, LoginHistoryActivity.class));
			} else if (result == 2) {
				ProgressDialogUtil.dismiss(mProgressDialog);
				ToastUtil.showToast(SendShuoshuoActivity.this, getString(R.string.upload_failed));
			} else {
				sendShuoshuo();
			}
		}

	}

	private class GridViewAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			if (mPhotoList.size() >= 9) {
				return 9;
			}
			return mPhotoList.size() + 1;
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
		public int getViewTypeCount() {
			return 2;
		}

		@Override
		public int getItemViewType(int position) {
			if (mPhotoList.size() == 0) {
				return 1;// View Type 1代表添加更多的视图
			} else if (mPhotoList.size() < 9) {
				if (position < mPhotoList.size()) {
					return 0;// View Type 0代表普通的ImageView视图
				} else {
					return 1;
				}
			} else {
				return 0;
			}
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			if (getItemViewType(position) == 0) {// 普通的视图
				SquareCenterImageView imageView = new SquareCenterImageView(SendShuoshuoActivity.this);
				imageView.setScaleType(ScaleType.CENTER_CROP);
				String url = mPhotoList.get(position);
				if (url == null) {
					url = "";
				}
				ImageLoader.getInstance().displayImage(Uri.fromFile(new File(url)).toString(), imageView);
				return imageView;
			} else {
				View view = LayoutInflater.from(SendShuoshuoActivity.this).inflate(R.layout.layout_circle_add_more_item,
						parent, false);
				ImageView iconImageView = (ImageView) view.findViewById(R.id.icon_image_view);
				TextView voiceTextTv = (TextView) view.findViewById(R.id.text_tv);
				iconImageView.setBackgroundResource(R.drawable.add_picture);
				voiceTextTv.setText(R.string.circle_add_image);
				return view;
			}
		}

	}

}
