package com.sk.weichat.ui.account;import java.util.HashMap;import android.app.AlertDialog;import android.app.Dialog;import android.content.DialogInterface;import android.content.Intent;import android.os.Bundle;import android.os.Handler;import android.support.v4.content.IntentCompat;import com.android.volley.Response.ErrorListener;import com.android.volley.VolleyError;import com.sk.weichat.MyApplication;import com.sk.weichat.R;import com.sk.weichat.bean.AttentionUser;import com.sk.weichat.bean.MyPhoto;import com.sk.weichat.bean.User;import com.sk.weichat.bean.circle.CircleMessage;import com.sk.weichat.bean.message.MucRoom;import com.sk.weichat.db.dao.CircleMessageDao;import com.sk.weichat.db.dao.FriendDao;import com.sk.weichat.db.dao.MyPhotoDao;import com.sk.weichat.db.dao.OnCompleteListener;import com.sk.weichat.db.dao.UserDao;import com.sk.weichat.helper.LoginHelper;import com.sk.weichat.sp.UserSp;import com.sk.weichat.ui.MainActivity;import com.sk.weichat.ui.base.BaseActivity;import com.sk.weichat.util.ToastUtil;import com.sk.weichat.view.DataLoadView;import com.sk.weichat.volley.ArrayResult;import com.sk.weichat.volley.ObjectResult;import com.sk.weichat.volley.Result;import com.sk.weichat.volley.StringJsonArrayRequest;import com.sk.weichat.volley.StringJsonArrayRequest.Listener;import com.sk.weichat.volley.StringJsonObjectRequest;/** * @项目名称: SkWeiChat-Baidu * @包名: com.sk.weichat.ui.account * @作者:王阳 * @创建时间: 2015年10月27日 下午3:03:06 * @描述: 数据更新界面 ,下载的数据： 1、我的商务圈最新数据 2、我的通讯录 3、更新用户基本资料 4、我的相册下载 * @SVN版本号: $Rev$ * @修改人: $Author$ * @修改时间: $Date$ * @修改的内容: TODO */public class DataDownloadActivity extends BaseActivity {	private DataLoadView mDataLoadView;	private String mLoginUserId;	private Handler mHandler;	@Override	protected void onCreate(Bundle savedInstanceState) {		super.onCreate(savedInstanceState);		setContentView(R.layout.activity_data_download);		UserSp.getInstance(DataDownloadActivity.this).setUpdate(false);// 进入下载资料界面，就将该值赋值false		mLoginUserId = MyApplication.getInstance().mLoginUser.getUserId();		mHandler = new Handler();		getSupportActionBar().setTitle(R.string.data_update);		initView();		startDownload();	}	private void initView() {		mDataLoadView = (DataLoadView) findViewById(R.id.data_load_view);		mDataLoadView.setLoadingEvent(new DataLoadView.LoadingEvent() {			@Override			public void load() {				startDownload();			}		});	}	private final int STATUS_NO_RESULT = 0;// 请求中，尚未返回	private final int STATUS_FAILED = 1;// 已经返回，失败了	private final int STATUS_SUCCESS = 2;// 已经返回，成功了	private int circle_msg_download_status = STATUS_NO_RESULT;// 商务圈ids下载	private int address_user_download_status = STATUS_NO_RESULT;// 通讯录下载	private int user_info_download_status = STATUS_NO_RESULT;// 个人基本资料下载	private int user_photo_download_status = STATUS_SUCCESS;// 我的相册下载	private int room_download_status = STATUS_NO_RESULT;// 我的房间下载	private void startDownload() {		mDataLoadView.showLoading();		if (circle_msg_download_status != STATUS_SUCCESS) {// 没有成功，就下载			circle_msg_download_status = STATUS_NO_RESULT;// 初始化下载状态			downloadCircleMessage();		}		if (address_user_download_status != STATUS_SUCCESS) {// 没有成功，就下载			address_user_download_status = STATUS_NO_RESULT;// 初始化下载状态			downloadAddressBook();		}		if (user_info_download_status != STATUS_SUCCESS) {// 没有成功，就下载			user_info_download_status = STATUS_NO_RESULT;// 初始化下载状态			downloadUserInfo();		}//		if (user_photo_download_status != STATUS_SUCCESS) {// 没有成功，就下载//			user_photo_download_status = STATUS_NO_RESULT;// 初始化下载状态//			downloadUserPhoto();//		}		if (room_download_status != STATUS_SUCCESS) {// 没有成功，就下载			room_download_status = STATUS_NO_RESULT;// 初始化下载状态			downloadRoom();		}	}	private void endDownload() {		// 只有有一个下载没返回，那么就继续等待		if (circle_msg_download_status == STATUS_NO_RESULT || address_user_download_status == STATUS_NO_RESULT				|| user_info_download_status == STATUS_NO_RESULT || user_photo_download_status == STATUS_NO_RESULT				|| room_download_status == STATUS_NO_RESULT) {			return;		}		// 只要有一个下载失败，那么显示更新失败。就继续下载		if (circle_msg_download_status == STATUS_FAILED || address_user_download_status == STATUS_FAILED				|| user_info_download_status == STATUS_FAILED || user_photo_download_status == STATUS_FAILED				|| room_download_status == STATUS_FAILED) {			mDataLoadView.showFailed();		} else {// 所有数据加载完毕,跳转回用户操作界面			if (mBackDialog != null && mBackDialog.isShowing()) {				mBackDialog.dismiss();			}			UserSp.getInstance(DataDownloadActivity.this).setUpdate(true);			LoginHelper.broadcastLogin(mContext);			// 此处BUG：如果MainActivity不存在，那么这个之前的界面都不能销毁，后面可以加个广播销毁他们			Intent intent = new Intent(mContext, MainActivity.class);			intent.setFlags(IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);			startActivity(intent);			finish();		}	}	/**	 * 下载商务圈消息	 */	private void downloadCircleMessage() {		HashMap<String, String> params = new HashMap<String, String>();		params.put("access_token", MyApplication.getInstance().mAccessToken);		StringJsonArrayRequest<CircleMessage> request = new StringJsonArrayRequest<CircleMessage>(				mConfig.DOWNLOAD_CIRCLE_MESSAGE, new ErrorListener() {					@Override					public void onErrorResponse(VolleyError arg0) {						ToastUtil.showErrorNet(mContext);						circle_msg_download_status = STATUS_FAILED;// 失败						endDownload();					}				}, new Listener<CircleMessage>() {					@Override					public void onResponse(ArrayResult<CircleMessage> result) {						boolean success = Result.defaultParser(mContext, result, true);						if (success) {							CircleMessageDao.getInstance().addMessages(mHandler, mLoginUserId, result.getData(),									new OnCompleteListener() {								@Override								public void onCompleted() {									circle_msg_download_status = STATUS_SUCCESS;// 成功									endDownload();								}							});						} else {							circle_msg_download_status = STATUS_FAILED;// 失败							endDownload();						}					}				}, CircleMessage.class, params);		addDefaultRequest(request);	}	/**	 * 下载我的关注，包括我的好友	 */	private void downloadAddressBook() {		HashMap<String, String> params = new HashMap<String, String>();		params.put("access_token", MyApplication.getInstance().mAccessToken);		StringJsonArrayRequest<AttentionUser> request = new StringJsonArrayRequest<AttentionUser>(				mConfig.FRIENDS_ATTENTION_LIST, new ErrorListener() {					@Override					public void onErrorResponse(VolleyError arg0) {						ToastUtil.showErrorNet(mContext);						address_user_download_status = STATUS_FAILED;// 失败						endDownload();					}				}, new Listener<AttentionUser>() {					@Override					public void onResponse(ArrayResult<AttentionUser> result) {						boolean success = Result.defaultParser(mContext, result, true);						if (success) {							FriendDao.getInstance().addAttentionUsers(mHandler, mLoginUserId, result.getData(),									new OnCompleteListener() {								@Override								public void onCompleted() {									address_user_download_status = STATUS_SUCCESS;// 成功									endDownload();								}							});						} else {							address_user_download_status = STATUS_FAILED;// 失败							endDownload();						}					}				}, AttentionUser.class, params);		addDefaultRequest(request);	}	/**	 * 下载个人基本资料	 */	private void downloadUserInfo() {		HashMap<String, String> params = new HashMap<String, String>();		params.put("access_token", MyApplication.getInstance().mAccessToken);		StringJsonObjectRequest<User> request = new StringJsonObjectRequest<User>(mConfig.USER_GET_URL,				new ErrorListener() {					@Override					public void onErrorResponse(VolleyError arg0) {						ToastUtil.showErrorNet(mContext);						user_info_download_status = STATUS_FAILED;// 失败						endDownload();					}				}, new StringJsonObjectRequest.Listener<User>() {					@Override					public void onResponse(ObjectResult<User> result) {						boolean updateSuccess = false;						if (Result.defaultParser(mContext, result, true)) {							User user = result.getData();							updateSuccess = UserDao.getInstance().updateByUser(user);							// 设置登陆用户信息							if (updateSuccess) {// 如果成功，那么就将User的详情赋值给全局变量								MyApplication.getInstance().mLoginUser = user;							}						}						if (updateSuccess) {							user_info_download_status = STATUS_SUCCESS;// 成功						} else {							user_info_download_status = STATUS_FAILED;// 失败						}						endDownload();					}				}, User.class, params);		addDefaultRequest(request);	}	/**	 * 下载我的相册	 */	private void downloadUserPhoto() {		HashMap<String, String> params = new HashMap<String, String>();		params.put("access_token", MyApplication.getInstance().mAccessToken);		StringJsonArrayRequest<MyPhoto> request = new StringJsonArrayRequest<MyPhoto>(mConfig.USER_PHOTO_LIST,				new ErrorListener() {					@Override					public void onErrorResponse(VolleyError arg0) {						ToastUtil.showErrorNet(mContext);						user_photo_download_status = STATUS_FAILED;// 失败						endDownload();					}				}, new StringJsonArrayRequest.Listener<MyPhoto>() {					@Override					public void onResponse(ArrayResult<MyPhoto> result) {						boolean success = Result.defaultParser(mContext, result, true);						if (success) {							MyPhotoDao.getInstance().addPhotos(mHandler, mLoginUserId, result.getData(),									new OnCompleteListener() {								@Override								public void onCompleted() {									user_photo_download_status = STATUS_SUCCESS;// 成功									endDownload();								}							});						} else {							user_photo_download_status = STATUS_FAILED;// 失败							endDownload();						}					}				}, MyPhoto.class, params);		addDefaultRequest(request);	}	/**	 * 下载我的房间	 */	private void downloadRoom() {		HashMap<String, String> params = new HashMap<String, String>();		params.put("access_token", MyApplication.getInstance().mAccessToken);		params.put("type", "0");		params.put("pageIndex", "0");		params.put("pageSize", "200");// 给一个尽量大的值		StringJsonArrayRequest<MucRoom> request = new StringJsonArrayRequest<MucRoom>(mConfig.ROOM_LIST_HIS,				new ErrorListener() {					@Override					public void onErrorResponse(VolleyError arg0) {						ToastUtil.showErrorNet(mContext);						room_download_status = STATUS_FAILED;// 失败						endDownload();					}				}, new Listener<MucRoom>() {					@Override					public void onResponse(ArrayResult<MucRoom> result) {						boolean success = Result.defaultParser(mContext, result, true);						if (success) {							FriendDao.getInstance().addRooms(mHandler, mLoginUserId, result.getData(),									new OnCompleteListener() {								@Override								public void onCompleted() {									room_download_status = STATUS_SUCCESS;// 成功									endDownload();								}							});						} else {							room_download_status = STATUS_FAILED;// 失败							endDownload();						}					}				}, MucRoom.class, params);		addDefaultRequest(request);	}	@Override	public void onBackPressed() {		doBack();	}	@Override	protected boolean onHomeAsUp() {		doBack();		return true;	}	private Dialog mBackDialog;	private void doBack() {		if (mBackDialog == null) {			AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.prompt_title)					.setMessage(R.string.data_not_update_exit).setNegativeButton(getString(R.string.no), null)					.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {						@Override						public void onClick(DialogInterface dialog, int which) {							LoginHelper.broadcastLoginGiveUp(DataDownloadActivity.this);							finish();						}					});			mBackDialog = builder.create();		}		mBackDialog.show();	}}