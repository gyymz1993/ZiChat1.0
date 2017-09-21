package com.sk.weichat.ui.circle;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
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
import com.sk.weichat.util.DeviceInfoUtil;
import com.sk.weichat.util.ProgressDialogUtil;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.volley.ObjectResult;
import com.sk.weichat.volley.Result;
import com.sk.weichat.volley.StringJsonObjectRequest;

/**
 * 发送音频的界面
 * 
 * 
 */
public class SendAudioActivity extends BaseActivity {

	private EditText mTextEdit;
	// Voice Item
	private ImageView mImageView;
	private ImageView mIconImageView;
	private TextView mVoiceTextTv;
	private View mFloatLayout;
	private Button mReleaseBtn;
	// data
	private String mImageFilePath;
	private String mAudioFilePath;
	private int mTimeLen;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_send_audio);
		mProgressDialog = ProgressDialogUtil.init(this, null, getString(R.string.please_wait));
		initView();
	}

	private void initView() {
		getSupportActionBar().setTitle(R.string.send_audio);
		// find view
		mTextEdit = (EditText) findViewById(R.id.text_edit);
		mImageView = (ImageView) findViewById(R.id.image_view);
		mIconImageView = (ImageView) findViewById(R.id.icon_image_view);
		mVoiceTextTv = (TextView) findViewById(R.id.text_tv);
		mFloatLayout = findViewById(R.id.float_layout);
		mReleaseBtn = (Button) findViewById(R.id.release_btn);

		// init status
		mIconImageView.setBackgroundResource(R.drawable.add_voice);
		mVoiceTextTv.setText(R.string.circle_add_voice);

		// set event
		mFloatLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(SendAudioActivity.this, CircleAudioRecordActivity.class);
				if (!TextUtils.isEmpty(mAudioFilePath) && mTimeLen > 0) {
					intent.putExtra(AppConstant.EXTRA_FILE_PATH, mAudioFilePath);
					intent.putExtra(AppConstant.EXTRA_TIME_LEN, mTimeLen);
				}
				if (!TextUtils.isEmpty(mImageFilePath)) {
					intent.putExtra(AppConstant.EXTRA_IMAGE_FILE_PATH, mImageFilePath);
				}
				startActivityForResult(intent, 1);
			}
		});
		mReleaseBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (TextUtils.isEmpty(mAudioFilePath) || mTimeLen <= 0) {
					return;
				}
				new UploadTask().execute();
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1 && resultCode == Activity.RESULT_OK && data != null) {// 拍照返回
			mAudioFilePath = data.getStringExtra(AppConstant.EXTRA_FILE_PATH);
			mTimeLen = data.getIntExtra(AppConstant.EXTRA_TIME_LEN, 0);
			mImageFilePath = data.getStringExtra(AppConstant.EXTRA_IMAGE_FILE_PATH);

			if (!TextUtils.isEmpty(mImageFilePath)) {
				ImageLoader.getInstance().displayImage(Uri.fromFile(new File(mImageFilePath)).toString(), mImageView,
						MyApplication.mAvatarRoundImageOptions);
			}
			if (!TextUtils.isEmpty(mAudioFilePath) && mTimeLen > 0) {
				mVoiceTextTv.setText(mTimeLen + "s");
			}
		}

	}

	private ProgressDialog mProgressDialog;
	private String mRecordData;
	private String mImageData;

	private class UploadTask extends AsyncTask<Void, Integer, Integer> {
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			ProgressDialogUtil.show(mProgressDialog);
		}

		/**
		 * 上传的结果： <br/>
		 * return 1 Token过期，请重新登陆 <br/>
		 * return 2 语音为空，请重新录制 <br/>
		 * return 3 上传出错<br/>
		 * return 4 上传成功<br/>
		 */
		@Override
		protected Integer doInBackground(Void... params) {
			if (!LoginHelper.isTokenValidation()) {
				return 1;
			}
			if (TextUtils.isEmpty(mAudioFilePath)) {
				return 2;
			}
			Map<String, String> mapParams = new HashMap<String, String>();
			mapParams.put("userId", MyApplication.getInstance().mLoginUser.getUserId() + "");
			mapParams.put("access_token", MyApplication.getInstance().mAccessToken);

			List<String> dataList = new ArrayList<>();
			dataList.add(mAudioFilePath);
			if (!TextUtils.isEmpty(mImageFilePath)) {
				dataList.add(mImageFilePath);
			}

			String result = new UploadService().uploadFile(mConfig.UPLOAD_URL, mapParams, dataList);

			Log.d("roamer", "UploadRecordResult:" + result);

			if (TextUtils.isEmpty(result)) {
				return 3;
			}

			UploadFileResult recordResult = JSON.parseObject(result, UploadFileResult.class);
			boolean success = Result.defaultParser(SendAudioActivity.this, recordResult, true);
			if (success) {
				if (recordResult.getSuccess() != recordResult.getTotal()) {// 上传丢失了某些文件
					return 3;
				}
				if (recordResult.getData() != null) {
					UploadFileResult.Data data = recordResult.getData();
					if (data.getAudios() != null && data.getAudios().size() > 0) {
						while (data.getAudios().size() > 1) {// 因为正确情况下只有一个语音，所以要保证只有一个语音
							data.getAudios().remove(data.getAudios().size() - 1);
						}
						data.getAudios().get(0).setSize(new File(mAudioFilePath).length());
						data.getAudios().get(0).setLength(mTimeLen);
						mRecordData = JSON.toJSONString(data.getAudios(), UploadFileResult.sAudioVideosFilter);
					} else {
						return 3;
					}

					if (data.getImages() != null && data.getImages().size() > 0) {
						mImageData = JSON.toJSONString(data.getImages(), UploadFileResult.sImagesFilter);
					}

					Log.d("roamer", "mRecordData:" + mRecordData);
					Log.d("roamer", "mImageData:" + mImageData);
					return 4;
				} else {// 没有文件数据源，失败
					return 3;
				}
			} else {
				return 3;
			}
		}

		@Override
		protected void onPostExecute(Integer result) {
			super.onPostExecute(result);
			if (result == 1) {
				ProgressDialogUtil.dismiss(mProgressDialog);
				startActivity(new Intent(SendAudioActivity.this, LoginHistoryActivity.class));
			} else if (result == 2) {
				ProgressDialogUtil.dismiss(mProgressDialog);
				ToastUtil.showToast(SendAudioActivity.this, R.string.audio_file_not_exist);
			} else if (result == 3) {
				ProgressDialogUtil.dismiss(mProgressDialog);
				ToastUtil.showToast(SendAudioActivity.this, R.string.upload_failed);
			} else {
				sendAudio();
			}
		}
	}

	// 发布一条说说
	public void sendAudio() {
		Map<String, String> params = new HashMap<String, String>();

		params.put("access_token", MyApplication.getInstance().mAccessToken);

		// 消息类型：1=文字消息；2=图文消息；3=语音消息；4=视频消息；
		params.put("type", "3");

		// 消息标记：1：求职消息；2：招聘消息；3：普通消息；
		params.put("flag", "3");

		// 消息隐私范围 0=不可见；1=朋友可见；2=粉丝可见；3=广场
		params.put("visible", "3");

		params.put("text", mTextEdit.getText().toString());// 消息内容

		params.put("audios", mRecordData);// 消息内容

		if (!TextUtils.isEmpty(mImageData) && !mImageData.equals("{}") && !mImageData.equals("[{}]")) {
			params.put("images", mImageData);
		}

		ProgressDialogUtil.show(mProgressDialog);
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

		StringJsonObjectRequest<String> request = new StringJsonObjectRequest<String>(mConfig.MSG_ADD_URL, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
				ProgressDialogUtil.dismiss(mProgressDialog);
				ToastUtil.showErrorNet(SendAudioActivity.this);
			}
		}, new StringJsonObjectRequest.Listener<String>() {
			@Override
			public void onResponse(ObjectResult<String> result) {
				boolean parserResult = Result.defaultParser(SendAudioActivity.this, result, true);
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

}
