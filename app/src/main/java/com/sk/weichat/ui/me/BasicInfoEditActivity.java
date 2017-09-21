package com.sk.weichat.ui.me;

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
import com.sk.weichat.bean.User;
import com.sk.weichat.db.dao.UserDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.helper.LoginHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.tool.SelectAreaActivity;
import com.sk.weichat.util.CameraUtil;
import com.sk.weichat.util.ProgressDialogUtil;
import com.sk.weichat.util.StringUtils;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.volley.ObjectResult;
import com.sk.weichat.volley.Result;
import com.sk.weichat.volley.StringJsonObjectRequest;

/**
 * 我的基本资料界面
 * 
 * @author Dean Tao
 * @version 1.0
 */
public class BasicInfoEditActivity extends BaseActivity implements View.OnClickListener {
	private User mUser;
	// widget
	private ImageView mAvatarImg;
	private EditText mNameEdit;
	private TextView mSexTv;
	private TextView mBirthdayTv;
	private TextView mCityTv;
	private Button mNextStepBtn;
	// Temp
	private User mTempData;
	// 选择头像的数据
	private File mCurrentFile;
	private boolean isError = false;

	private ProgressDialog mProgressDialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mUser = MyApplication.getInstance().mLoginUser;
		if (!LoginHelper.isUserValidation(mUser)) {
			isError = true;
			return;
		}
		mProgressDialog = ProgressDialogUtil.init(mContext, null, getString(R.string.please_wait));
		setContentView(R.layout.activity_basic_info_edit);
		initView();
	}

	private void initView() {
		getSupportActionBar().setTitle(R.string.basic_info);
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
		// clone一份临时数据，用来存数变化的值，返回的时候对比有无变化
		try {
			mTempData = (User) mUser.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		if (mTempData.getSex() == 1) {
			mSexTv.setText(R.string.sex_man);
		} else {
			mSexTv.setText(R.string.sex_woman);
		}
		mBirthdayTv.setText(TimeUtils.sk_time_s_long_2_str(mTempData.getBirthday()));
		mNameEdit.setText(mTempData.getNickName());
		mCityTv.setText(Area.getProvinceCityString(mTempData.getProvinceId(), mTempData.getCityId()));
		AvatarHelper.getInstance().displayAvatar(mTempData.getUserId(), mAvatarImg, true);
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
			Intent intent = new Intent(BasicInfoEditActivity.this, SelectAreaActivity.class);
			intent.putExtra(SelectAreaActivity.EXTRA_AREA_TYPE, Area.AREA_TYPE_PROVINCE);
			intent.putExtra(SelectAreaActivity.EXTRA_AREA_PARENT_ID, Area.AREA_DATA_CHINA_ID);// 直接选择中国，
			intent.putExtra(SelectAreaActivity.EXTRA_AREA_DEEP, Area.AREA_TYPE_COUNTY);
			startActivityForResult(intent, 4);
			break;
		case R.id.next_step_btn:
			next();
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
					mCurrentFile = new File(mNewPhotoUri.getPath());
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
					mCurrentFile = new File(mNewPhotoUri.getPath());
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
	}

	@SuppressWarnings("deprecation")
	private void showSelectBirthdayDialog() {
		Date date = new Date(mTempData.getBirthday() * 1000);
		DatePickerDialog dialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
			@Override
			public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
				GregorianCalendar calendar = new GregorianCalendar(year, monthOfYear, dayOfMonth);
				mTempData.setBirthday(TimeUtils.getSpecialBeginTime(mBirthdayTv, calendar.getTime().getTime() / 1000));
				long currentTime = System.currentTimeMillis() / 1000;
				long birthdayTime=calendar.getTime().getTime() / 1000;
				if (birthdayTime > currentTime) {
					ToastUtil.showToast(mContext, "亲!您的出生日期已经超过现在了哦!");
				}
			}
		}, date.getYear() + 1900, date.getMonth(), date.getDate());
		dialog.show();
	}

	private void loadPageData() {
		mTempData.setNickName(mNameEdit.getText().toString().trim());
	}

	private void next() {
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

		if (mUser != null && !mUser.equals(mTempData)) {// 数据改变了，提交数据
			updateData();
		} else if (mCurrentFile != null && mCurrentFile.exists()) {
			uploadAvatar(mCurrentFile);
		} else {
			finish();
		}
	}

	private void updateData() {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		if (!mUser.getNickName().equals(mTempData.getNickName())) {
			params.put("nickname", mTempData.getNickName());
		}
		if (mUser.getSex() != mTempData.getSex()) {
			params.put("sex", String.valueOf(mTempData.getSex()));
		}
		if (mUser.getBirthday() != mTempData.getBirthday()) {
			params.put("birthday", String.valueOf(mTempData.getBirthday()));
		}
		if (mUser.getCountryId() != mTempData.getCountryId()) {
			params.put("countryId", String.valueOf(mTempData.getCountryId()));
		}
		if (mUser.getProvinceId() != mTempData.getProvinceId()) {
			params.put("provinceId", String.valueOf(mTempData.getProvinceId()));
		}
		if (mUser.getCityId() != mTempData.getCityId()) {
			params.put("cityId", String.valueOf(mTempData.getCityId()));
		}
		if (mUser.getAreaId() != mTempData.getAreaId()) {
			params.put("areaId", String.valueOf(mTempData.getAreaId()));
		}

		ProgressDialogUtil.show(mProgressDialog);

		StringJsonObjectRequest<Void> request = new StringJsonObjectRequest<Void>(mConfig.USER_UPDATE, new ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
				ProgressDialogUtil.dismiss(mProgressDialog);
				ToastUtil.showErrorNet(BasicInfoEditActivity.this);
			}
		}, new StringJsonObjectRequest.Listener<Void>() {

			@Override
			public void onResponse(ObjectResult<Void> result) {
				boolean success = Result.defaultParser(mContext, result, true);
				if (success) {
					saveData();
					if (mCurrentFile != null && mCurrentFile.exists()) {
						uploadAvatar(mCurrentFile);
					} else {
						ProgressDialogUtil.dismiss(mProgressDialog);
						setResult(RESULT_OK);
						finish();
					}
				} else {
					ProgressDialogUtil.dismiss(mProgressDialog);
				}
			}
		}, Void.class, params);
		addDefaultRequest(request);
	}

	private void saveData() {
		if (!mUser.getNickName().equals(mTempData.getNickName())) {
			MyApplication.getInstance().mLoginUser.setNickName(mTempData.getNickName());
			UserDao.getInstance().updateNickName(mTempData.getUserId(), mTempData.getNickName());// 更新数据库
		}
		if (mUser.getSex() != mTempData.getSex()) {
			MyApplication.getInstance().mLoginUser.setSex(mTempData.getSex());
			UserDao.getInstance().updateSex(mTempData.getUserId(), mTempData.getSex() + "");// 更新数据库
		}
		if (mUser.getBirthday() != mTempData.getBirthday()) {
			MyApplication.getInstance().mLoginUser.setBirthday(mTempData.getBirthday());
			UserDao.getInstance().updateBirthday(mTempData.getUserId(), mTempData.getBirthday() + "");// 更新数据库
		}

		if (mUser.getCountryId() != mTempData.getCountryId()) {
			MyApplication.getInstance().mLoginUser.setCountryId(mTempData.getCountryId());
			UserDao.getInstance().updateCountryId(mTempData.getUserId(), mTempData.getCountryId());
		}
		if (mUser.getProvinceId() != mTempData.getProvinceId()) {
			MyApplication.getInstance().mLoginUser.setProvinceId(mTempData.getProvinceId());
			UserDao.getInstance().updateProvinceId(mTempData.getUserId(), mTempData.getProvinceId());
		}
		if (mUser.getCityId() != mTempData.getCityId()) {
			MyApplication.getInstance().mLoginUser.setCityId(mTempData.getCityId());
			UserDao.getInstance().updateCityId(mTempData.getUserId(), mTempData.getCityId());
		}
		if (mUser.getAreaId() != mTempData.getAreaId()) {
			MyApplication.getInstance().mLoginUser.setAreaId(mTempData.getAreaId());
			UserDao.getInstance().updateAreaId(mTempData.getUserId(), mTempData.getAreaId());
		}

	}

	@Override
	public void onBackPressed() {
		doBack();
	}

	@Override
	protected boolean onHomeAsUp() {
		doBack();
		return true;
	}

	private void doBack() {
		if (isError) {
			super.onBackPressed();
			return;
		}
		loadPageData();
		if ((mUser != null && !mUser.equals(mTempData)) || (mCurrentFile != null && mCurrentFile.exists())) {
			showBackDialog();
		} else {
			finish();
		}

	}

	private void showBackDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.prompt_title).setMessage(R.string.cancel_edit_prompt)
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
					ToastUtil.showToast(BasicInfoEditActivity.this, R.string.upload_avatar_success);
				} else {
					ToastUtil.showToast(BasicInfoEditActivity.this, R.string.upload_avatar_failed);
				}

				setResult(RESULT_OK);
				finish();
			}

			@Override
			public void onFailure(int arg0, Header[] arg1, byte[] arg2, Throwable arg3) {
				ProgressDialogUtil.dismiss(mProgressDialog);
				ToastUtil.showToast(BasicInfoEditActivity.this, R.string.upload_avatar_failed);
			}
		});
	}

}
