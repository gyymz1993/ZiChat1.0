package com.sk.weichat.ui.account;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.apache.http.Header;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.Area;
import com.sk.weichat.bean.LoginRegisterResult;
import com.sk.weichat.bean.User;
import com.sk.weichat.helper.LoginHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.tool.SelectAreaActivity;
import com.sk.weichat.util.CameraUtil;
import com.sk.weichat.util.DeviceInfoUtil;
import com.sk.weichat.util.ProgressDialogUtil;
import com.sk.weichat.util.StringUtils;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.volley.ObjectResult;
import com.sk.weichat.volley.Result;
import com.sk.weichat.volley.StringJsonObjectRequest;

/**
 * 注册公司基本资料界面
 * 
 * @author Dean Tao
 * @version 1.0
 */
public class RegisterUserBasicInfoActivity extends BaseActivity implements View.OnClickListener {
	private ImageView mAvatarImg;
	private EditText mNameEdit;
	private TextView mSexTv;
	private TextView mBirthdayTv;
	private TextView mCityTv;
	private Button mNextStepBtn;

	/* 前面页面传递进来的四个参数，都是必填 */
	private String mPhoneNum;
	private String mPassword;
	// Temp
	private User mTempData;
	// 选择头像的数据
	private File mCurrentFile;
	protected ProgressDialog mProgressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (getIntent() != null) {
			mPhoneNum = getIntent().getStringExtra(RegisterActivity.EXTRA_PHONE_NUMBER);
			mPassword = getIntent().getStringExtra(RegisterActivity.EXTRA_PASSWORD);
		}
		setContentView(R.layout.activity_register_user_basic_info);
		getSupportActionBar().setTitle(R.string.register_step_three);
		mProgressDialog = ProgressDialogUtil.init(mContext, null, getString(R.string.please_wait));
		initView();
	}

	private void initView() {
		mAvatarImg = (ImageView) findViewById(R.id.avatar_img);
		mNameEdit = (EditText) findViewById(R.id.name_edit);
		mSexTv = (TextView) findViewById(R.id.sex_tv);
		mBirthdayTv = (TextView) findViewById(R.id.birthday_tv);
		mCityTv = (TextView) findViewById(R.id.city_tv);
		mNextStepBtn = (Button) findViewById(R.id.next_step_btn);

		mAvatarImg.setOnClickListener(this);
		findViewById(R.id.sex_select_rl).setOnClickListener(this);
		findViewById(R.id.birthday_select_rl).setOnClickListener(this);
		findViewById(R.id.city_select_rl).setOnClickListener(this);
		mNextStepBtn.setOnClickListener(this);

		updateUI();
	}

	private void updateUI() {
		mTempData = new User();
		mTempData.setSex(1);
		mTempData.setBirthday(TimeUtils.sk_time_current_time());
		if (mTempData.getSex() == 1) {
			mSexTv.setText(R.string.sex_man);
		} else {
			mSexTv.setText(R.string.sex_woman);
		}
		mBirthdayTv.setText(TimeUtils.sk_time_s_long_2_str(mTempData.getBirthday()));
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.avatar_img:
			showSelectAvatarDialog();
			break;
		case R.id.sex_select_rl:
			showSelectSexDialog();
			break;
		case R.id.birthday_select_rl:
			showSelectBirthdayDialog();
			break;
		case R.id.city_select_rl:
			Intent intent = new Intent(RegisterUserBasicInfoActivity.this, SelectAreaActivity.class);
			intent.putExtra(SelectAreaActivity.EXTRA_AREA_TYPE, Area.AREA_TYPE_PROVINCE);
			intent.putExtra(SelectAreaActivity.EXTRA_AREA_PARENT_ID, Area.AREA_DATA_CHINA_ID);// 直接选择中国，
			intent.putExtra(SelectAreaActivity.EXTRA_AREA_DEEP, Area.AREA_TYPE_COUNTY);
			startActivityForResult(intent, 4);
			break;
		case R.id.next_step_btn:
			register();
			break;
		}

	}

	private void showSelectAvatarDialog() {
		String[] items = new String[] { getString(R.string.c_take_picture), getString(R.string.c_photo_album) };
		AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.select_avatar).setSingleChoiceItems(items, 0,
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

	private static final int REQUEST_CODE_CAPTURE_CROP_PHOTO = 1;
	private static final int REQUEST_CODE_PICK_CROP_PHOTO = 2;
	private static final int REQUEST_CODE_CROP_PHOTO = 3;
	private Uri mNewPhotoUri;

	private void takePhoto() {
		mNewPhotoUri = CameraUtil.getOutputMediaFileUri(this, CameraUtil.MEDIA_TYPE_IMAGE);
		CameraUtil.captureImage(this, mNewPhotoUri, REQUEST_CODE_CAPTURE_CROP_PHOTO);
	}

	private void selectPhoto() {
		CameraUtil.pickImageSimple(this, REQUEST_CODE_PICK_CROP_PHOTO);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE_CAPTURE_CROP_PHOTO) {// 拍照返回再去裁减
			if (resultCode == Activity.RESULT_OK) {
				if (mNewPhotoUri != null) {
					Uri o = mNewPhotoUri;
					mNewPhotoUri = CameraUtil.getOutputMediaFileUri(this, CameraUtil.MEDIA_TYPE_IMAGE);
					CameraUtil.cropImage(this, o, mNewPhotoUri, REQUEST_CODE_CROP_PHOTO, 1, 1, 300, 300);
				} else {
					ToastUtil.showToast(this, R.string.c_photo_album_failed);
				}
			}
		} else if (requestCode == REQUEST_CODE_PICK_CROP_PHOTO) {// 选择一张图片,然后立即调用裁减
			if (resultCode == Activity.RESULT_OK) {
				if (data != null && data.getData() != null) {
					String path = CameraUtil.getImagePathFromUri(this, data.getData());
					Uri o = Uri.fromFile(new File(path));
					mNewPhotoUri = CameraUtil.getOutputMediaFileUri(this, CameraUtil.MEDIA_TYPE_IMAGE);
					CameraUtil.cropImage(this, o, mNewPhotoUri, REQUEST_CODE_CROP_PHOTO, 1, 1, 300, 300);
				} else {
					ToastUtil.showToast(this, R.string.c_photo_album_failed);
				}
			}
		} else if (requestCode == REQUEST_CODE_CROP_PHOTO) {
			if (resultCode == Activity.RESULT_OK) {
				if (mNewPhotoUri != null) {
					mCurrentFile = new File(mNewPhotoUri.getPath());
					ImageLoader.getInstance().displayImage(mNewPhotoUri.toString(), mAvatarImg);
				} else {
					ToastUtil.showToast(this, R.string.c_crop_failed);
				}
			}

		} else if (requestCode == 4) {// 选择城市
			if (resultCode == RESULT_OK && data != null) {
				int countryId = data.getIntExtra(SelectAreaActivity.EXTRA_COUNTRY_ID, 0);
				int provinceId = data.getIntExtra(SelectAreaActivity.EXTRA_PROVINCE_ID, 0);
				int cityId = data.getIntExtra(SelectAreaActivity.EXTRA_CITY_ID, 0);
				int countyId = data.getIntExtra(SelectAreaActivity.EXTRA_COUNTY_ID, 0);

				// String country_name = data.getStringExtra(Constant.EXTRA_COUNTRY_NAME);
				String province_name = data.getStringExtra(SelectAreaActivity.EXTRA_PROVINCE_NAME);
				String city_name = data.getStringExtra(SelectAreaActivity.EXTRA_CITY_NAME);
				mCityTv.setText(province_name + "-" + city_name);

				mTempData.setCountryId(countryId);
				mTempData.setProvinceId(provinceId);
				mTempData.setCityId(cityId);
				mTempData.setAreaId(countyId);
			}

		}

	}

	private void showSelectSexDialog() {
		String[] sexs = new String[] { getString(R.string.sex_man), getString(R.string.sex_woman) };
		new AlertDialog.Builder(this).setTitle(getString(R.string.select_sex))
				.setSingleChoiceItems(sexs, mTempData.getSex() == 1 ? 0 : 1, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (which == 0) {
							mTempData.setSex(1);
							mSexTv.setText(R.string.sex_man);
						} else {
							mTempData.setSex(0);
							mSexTv.setText(R.string.sex_woman);
						}
						dialog.dismiss();
					}
				}).setCancelable(true).create().show();
		;
	}

	@SuppressWarnings("deprecation")
	private void showSelectBirthdayDialog() {
		Date date = new Date(mTempData.getBirthday() * 1000);
		DatePickerDialog dialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
			@Override
			public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
				GregorianCalendar calendar = new GregorianCalendar(year, monthOfYear, dayOfMonth);
				mTempData.setBirthday(TimeUtils.getSpecialBeginTime(mBirthdayTv, calendar.getTime().getTime() / 1000));
			}
		}, date.getYear() + 1900, date.getMonth(), date.getDate());
		dialog.show();
	}

	private void loadPageData() {
		mTempData.setNickName(mNameEdit.getText().toString().trim());
	}

	private void register() {
		if (!MyApplication.getInstance().isNetworkActive()) {
			ToastUtil.showToast(this, R.string.net_exception);
			return;
		}
		loadPageData();

		if (TextUtils.isEmpty(mTempData.getNickName())) {
			mNameEdit.requestFocus();
			mNameEdit.setError(StringUtils.editTextHtmlErrorTip(this, R.string.name_empty_error));
			return;
		}
		if (!StringUtils.isNickName(mTempData.getNickName())) {
			mNameEdit.requestFocus();
			mNameEdit.setError(StringUtils.editTextHtmlErrorTip(this, R.string.nick_name_format_error));
			return;
		}

		if (mTempData.getCityId() <= 0) {
			ToastUtil.showToast(mContext, R.string.live_address_empty_error);
			return;
		}

		HashMap<String, String> params = new HashMap<String, String>();
		// 前面页面传递的信息
		params.put("userType", "1");
		params.put("telephone", mPhoneNum);
		params.put("password", mPassword);
		// params.put("countryId", mAreaCode);//TODO AreaCode 区号暂时不带
		// 本页面信息
		params.put("nickname", mTempData.getNickName());
		params.put("sex", String.valueOf(mTempData.getSex()));
		params.put("birthday", String.valueOf(mTempData.getBirthday()));

		params.put("countryId", String.valueOf(mTempData.getCountryId()));
		params.put("provinceId", String.valueOf(mTempData.getProvinceId()));
		params.put("cityId", String.valueOf(mTempData.getCityId()));
		params.put("areaId", String.valueOf(mTempData.getAreaId()));

		// 附加信息
		params.put("apiVersion", DeviceInfoUtil.getVersionCode(mContext) + "");
		params.put("model", DeviceInfoUtil.getModel());
		params.put("osVersion", DeviceInfoUtil.getOsVersion());
		params.put("serial", DeviceInfoUtil.getDeviceId(mContext));
		// 地址信息
		double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
		double longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
		String location = MyApplication.getInstance().getBdLocationHelper().getAddress();
		if (latitude != 0)
			params.put("latitude", String.valueOf(latitude));
		if (longitude != 0)
			params.put("longitude", String.valueOf(longitude));
		if (!TextUtils.isEmpty(location))
			params.put("location", location);

		ProgressDialogUtil.show(mProgressDialog);

		StringJsonObjectRequest<LoginRegisterResult> request = new StringJsonObjectRequest<>(mConfig.USER_REGISTER,
				new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						ProgressDialogUtil.dismiss(mProgressDialog);
						ToastUtil.showErrorNet(RegisterUserBasicInfoActivity.this);
					}
				}, new StringJsonObjectRequest.Listener<LoginRegisterResult>() {

					@Override
					public void onResponse(ObjectResult<LoginRegisterResult> result) {
						if (result == null) {
							ProgressDialogUtil.dismiss(mProgressDialog);
							ToastUtil.showToast(RegisterUserBasicInfoActivity.this, R.string.register_error);
							return;
						}
						if (result.getResultCode() == Result.CODE_SUCCESS) {// 注册成功
							boolean success = LoginHelper.setLoginUser(RegisterUserBasicInfoActivity.this, mPhoneNum, mPassword, result);
							if (success) {
								if (mCurrentFile != null && mCurrentFile.exists()) {// 选择了头像，那么先上传头像
									uploadAvatar(mCurrentFile);
									return;
								} else {// 没有选择头像，直接进入程序主页
									ProgressDialogUtil.dismiss(mProgressDialog);
									startActivity(new Intent(RegisterUserBasicInfoActivity.this, DataDownloadActivity.class));
									finish();
								}
								ToastUtil.showToast(RegisterUserBasicInfoActivity.this, R.string.register_success);
							} else {// 失败
								ProgressDialogUtil.dismiss(mProgressDialog);
								if (TextUtils.isEmpty(result.getResultMsg())) {
									ToastUtil.showToast(RegisterUserBasicInfoActivity.this, R.string.register_error);
								} else {
									ToastUtil.showToast(RegisterUserBasicInfoActivity.this, result.getResultMsg());
								}
							}
						} else {// 失败
							ProgressDialogUtil.dismiss(mProgressDialog);
							if (TextUtils.isEmpty(result.getResultMsg())) {
								ToastUtil.showToast(RegisterUserBasicInfoActivity.this, R.string.register_error);
							} else {
								ToastUtil.showToast(RegisterUserBasicInfoActivity.this, result.getResultMsg());
							}
						}

					}
				}, LoginRegisterResult.class, params);
		addDefaultRequest(request);
	}

	@Override
	protected boolean onHomeAsUp() {
		doBack();
		return true;
	}

	@Override
	public void onBackPressed() {
		doBack();
	}

	private void doBack() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.prompt_title).setMessage(R.string.cancel_register_prompt)
				.setNegativeButton(getString(R.string.no), null).setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						finish();
					}
				});
		builder.create().show();
	}

	private void uploadAvatar(File file) {
		if (!file.exists()) {// 文件不存在
			return;
		}
		// 显示正在上传的ProgressDialog
		ProgressDialogUtil.show(mProgressDialog, getString(R.string.upload_avataring));

		RequestParams params = new RequestParams();
		String loginUserId = MyApplication.getInstance().mLoginUser.getUserId();
		params.put("userId", loginUserId);
		try {
			params.put("file1", file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		AsyncHttpClient client = new AsyncHttpClient();

		client.post(mConfig.AVATAR_UPLOAD_URL, params, new AsyncHttpResponseHandler() {
			@Override
			public void onSuccess(int arg0, Header[] arg1, byte[] arg2) {
				boolean success = false;
				if (arg0 == 200) {
					Result result = null;
					try {
						result = JSON.parseObject(new String(arg2), Result.class);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (result != null && result.getResultCode() == Result.CODE_SUCCESS) {
						success = true;
					}
				}

				ProgressDialogUtil.dismiss(mProgressDialog);
				if (success) {
					ToastUtil.showToast(RegisterUserBasicInfoActivity.this, R.string.upload_avatar_success);
				} else {
					ToastUtil.showToast(RegisterUserBasicInfoActivity.this, R.string.upload_avatar_failed);
				}

				startActivity(new Intent(RegisterUserBasicInfoActivity.this, DataDownloadActivity.class));
				finish();
			}

			@Override
			public void onFailure(int arg0, Header[] arg1, byte[] arg2, Throwable arg3) {
				ProgressDialogUtil.dismiss(mProgressDialog);
				ToastUtil.showToast(RegisterUserBasicInfoActivity.this, R.string.upload_avatar_failed);
				startActivity(new Intent(RegisterUserBasicInfoActivity.this, DataDownloadActivity.class));
				finish();
			}
		});
	}

}
