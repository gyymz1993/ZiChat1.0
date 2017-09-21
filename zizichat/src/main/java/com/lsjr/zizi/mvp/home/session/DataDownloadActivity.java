package com.lsjr.zizi.mvp.home.session;

import android.annotation.SuppressLint;
import android.os.Handler;

import com.lsjr.bean.ArrayResult;
import com.lsjr.bean.ObjectResult;
import com.lsjr.bean.Result;
import com.lsjr.callback.ChatArrayCallBack;
import com.lsjr.callback.ChatObjectCallBack;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.chat.bean.AttentionUser;
import com.lsjr.zizi.chat.bean.MucRoom;
import com.lsjr.zizi.chat.bean.ResultCode;
import com.lsjr.zizi.chat.dao.CircleMessageDao;
import com.lsjr.zizi.chat.dao.FriendDao;
import com.lsjr.zizi.chat.dao.UserDao;
import com.lsjr.zizi.chat.db.CircleMessage;
import com.lsjr.zizi.chat.db.MyPhoto;
import com.lsjr.zizi.chat.db.User;
import com.lsjr.zizi.chat.helper.UserSp;
import com.ymz.baselibrary.utils.L_;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;

/**
 *  数据更新界面 ,下载的数据： 1、我的商务圈最新数据 2、我的通讯录 3、更新用户基本资料 4、我的相册下载
 */
@SuppressLint("Registered")
public abstract class DataDownloadActivity {
	private static final int STATUS_NO_RESULT = 0;// 请求中，尚未返回
	private static final int STATUS_FAILED = 1;// 已经返回，失败了
	private static final int STATUS_SUCCESS = 2;// 已经返回，成功了
	public static int circle_msg_download_status = STATUS_NO_RESULT;// 商务圈ids下载
	public static int address_user_download_status = STATUS_NO_RESULT;// 通讯录下载
	public static int user_info_download_status = STATUS_NO_RESULT;// 个人基本资料下载
	public static int user_photo_download_status = STATUS_SUCCESS;// 我的相册下载
	public static int room_download_status = STATUS_SUCCESS;// 我的房间下载
	private String mLoginUserId;
	private Handler mHandler;


	public void onStartDown() {
		UserSp.getInstance().setUpdate(false);// 进入下载资料界面，就将该值赋值false
		mLoginUserId = ConfigApplication.instance().mLoginUser.getUserId();
		//checkUserDb(mLoginUserId);
		mHandler = new Handler();
		startDownload();
	}


	private void checkUserDb(final String userId) {
		// 检测用户基本数据库信息的完整性
		//ThreadFactory.
		new Thread(() -> {
			FriendDao.getInstance().checkSystemFriend(userId);
		}).start();
	}



	private void startDownload() {
	   //  showProgressDialogWithText("数据下载中...");

		if (circle_msg_download_status != STATUS_SUCCESS) {// 没有成功，就下载
			circle_msg_download_status = STATUS_NO_RESULT;// 初始化下载状态
			downloadCircleMessage();
		}

		if (address_user_download_status != STATUS_SUCCESS) {// 没有成功，就下载
			address_user_download_status = STATUS_NO_RESULT;// 初始化下载状态
			downloadAddressBook();
		}

		if (user_info_download_status != STATUS_SUCCESS) {// 没有成功，就下载
			user_info_download_status = STATUS_NO_RESULT;// 初始化下载状态
			downloadUserInfo();
		}

		if (user_photo_download_status != STATUS_SUCCESS) {// 没有成功，就下载
			user_photo_download_status = STATUS_NO_RESULT;// 初始化下载状态
			downloadUserPhoto();
		}
		if (room_download_status != STATUS_SUCCESS) {// 没有成功，就下载
			room_download_status = STATUS_NO_RESULT;// 初始化下载状态
			downloadRoom();
		}

	}



	private void endDownload() {
		// 只有有一个下载没返回，那么就继续等待
		if (circle_msg_download_status == STATUS_NO_RESULT || address_user_download_status == STATUS_NO_RESULT
				|| user_info_download_status == STATUS_NO_RESULT || user_photo_download_status == STATUS_NO_RESULT
				|| room_download_status == STATUS_NO_RESULT) {
			return;
		}
		// 只要有一个下载失败，那么显示更新失败。就继续下载
		if (circle_msg_download_status == STATUS_FAILED || address_user_download_status == STATUS_FAILED
				|| user_info_download_status == STATUS_FAILED || user_photo_download_status == STATUS_FAILED
				|| room_download_status == STATUS_FAILED) {

			onStartDown();
			//dismissProgressDialog();
			//showErrorView();
		} else {// 所有数据加载完毕,跳转回用户操作界面
			//dismissProgressDialog();
			//showContentView();
			//openActivity(HomeActivity.class);

			//jumpToActivityAndClearTask(HomeActivity.class);
			downloadOver();
		}
	}

	protected abstract void downloadOver();

	protected abstract void resetLogin();


	public void cleanAllStatus(){
		 circle_msg_download_status = STATUS_NO_RESULT;// 商务圈ids下载
		 address_user_download_status = STATUS_NO_RESULT;// 通讯录下载
		 user_info_download_status = STATUS_NO_RESULT;// 个人基本资料下载
		 user_photo_download_status = STATUS_SUCCESS;// 我的相册下载
		 room_download_status = STATUS_SUCCESS;// 我的房间下载
	}

	/**
	 * 下载商务圈消息
	 */
	private void downloadCircleMessage() {
		if (circle_msg_download_status ==STATUS_SUCCESS)return;
		L_.e("开始下载商务圈");
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", ConfigApplication.instance().getmAccessToken());
		HttpUtils.getInstance().postServiceData(AppConfig.DOWNLOAD_CIRCLE_MESSAGE, params, new ChatArrayCallBack<CircleMessage>(CircleMessage.class) {
			@Override
			protected void onXError(String exception) {
				L_.e("商务圈下载失败" + exception);
				circle_msg_download_status = STATUS_FAILED;// 失败
				endDownload();
			}

			@Override
			protected void onSuccess(ArrayResult<CircleMessage> result) {
				L_.e("商务圈下载成功");
				if (result.getResultCode() == Result.CODE_TOKEN_ERROR) {// 注册成功
					resetLogin();
					return;
				}
				boolean success = ResultCode.defaultParser(result, true);
				if (success) {
					CircleMessageDao.getInstance().addMessages(mHandler, mLoginUserId, result.getData(),
							() -> {
                                circle_msg_download_status = STATUS_SUCCESS;// 成功
                                endDownload();
                            });
				} else {
					circle_msg_download_status = STATUS_FAILED;// 失败
					endDownload();
				}
				//endDownload();
			}
		});
	}


	/**
	 * 下载我的关注，包括我的好友
	 */
	private void downloadAddressBook() {
		if (address_user_download_status ==STATUS_SUCCESS)return;
		L_.e("开始下载我的关注");
		HashMap<String, String> params = new HashMap<>();
		params.put("access_token", ConfigApplication.instance().getmAccessToken());
		HttpUtils.getInstance().postServiceData(AppConfig.FRIENDS_ATTENTION_LIST, params, new ChatArrayCallBack<AttentionUser>(AttentionUser.class) {

			@Override
			protected void onXError(String exception) {
				address_user_download_status = STATUS_FAILED;// 失败
				endDownload();
			}

			@Override
			protected void onSuccess(ArrayResult<AttentionUser> result) {
				if (result.getResultCode() == Result.CODE_TOKEN_ERROR) {// 注册成功
					resetLogin();
					return;
				}
				L_.e("我的关注下载成功"+result.getData().toString());
				boolean success = ResultCode.defaultParser(result, true);
				if (success) {
					FriendDao.getInstance().addAttentionUsers(mHandler, mLoginUserId, result.getData(),
							() -> {
                                address_user_download_status = STATUS_SUCCESS;// 成功
                                endDownload();
                            });
				} else {
					address_user_download_status = STATUS_FAILED;// 失败
					endDownload();
				}
			}
		});
	}

	/**
	 * 下载个人基本资料
	 */
	private void downloadUserInfo() {
		if (user_info_download_status ==STATUS_SUCCESS)return;
		L_.e("开始下载个人基本");
		HashMap<String, String> params = new HashMap<>();
		params.put("access_token", ConfigApplication.instance().getmAccessToken());
		HttpUtils.getInstance().postServiceData(AppConfig.USER_GET_URL, params, new ChatObjectCallBack<User>(User.class) {
			@Override
			protected void onXError(String exception) {
				user_info_download_status = STATUS_FAILED;// 失败
				endDownload();
			}
			@Override
			protected void onSuccess(ObjectResult<User> result) {
				boolean updateSuccess;
				user_info_download_status = STATUS_SUCCESS;// 成功
				if (result.getResultCode() == Result.CODE_TOKEN_ERROR) {// 注册成功
					resetLogin();
					return;
				}
				L_.e("个人基本资料下载成功");
				if (ResultCode.defaultParser(result, true)) {
					// 设置登陆用户信息
					User user = result.getData();
					updateSuccess = UserDao.getInstance().updateByUser(user);
					// 设置登陆用户信息
					if (updateSuccess) {// 如果成功，那么就将User的详情赋值给全局变量
						ConfigApplication.instance().mLoginUser =  result.getData();
					}
					if (updateSuccess) {
						user_info_download_status = STATUS_SUCCESS;// 成功
					} else {
						user_info_download_status = STATUS_FAILED;// 失败
						endDownload();
					}
				}

			}
		});

	}

	/**
	 * 下载我的相册
	 */
	private void downloadUserPhoto() {
		if (user_photo_download_status ==STATUS_SUCCESS)return;
		L_.e("开始下载我的相册");
		HashMap<String, String> params = new HashMap<>();
		params.put("access_token", ConfigApplication.instance().getmAccessToken());
		HttpUtils.getInstance().postServiceData(AppConfig.USER_PHOTO_LIST, params, new ChatArrayCallBack<MyPhoto>(MyPhoto.class) {
			@Override
			protected void onXError(String exception) {
				user_photo_download_status = STATUS_FAILED;// 失败
				endDownload();
			}

			@Override
			protected void onSuccess(ArrayResult<MyPhoto> result) {
				if (result.getResultCode() == Result.CODE_TOKEN_ERROR) {// 注册成功
					resetLogin();
					return;
				}
				L_.e("我的相册下载成功"+result.getData());
				boolean success = ResultCode.defaultParser(result, true);
				if (success) {
					user_photo_download_status = STATUS_SUCCESS;// 成功
				} else {
					user_photo_download_status = STATUS_SUCCESS;// 失败
				}
				endDownload();
			}
		});
	}

	/**
	 * 下载我的房间
	 */
	private void downloadRoom() {
		if (room_download_status ==STATUS_SUCCESS)return;
		L_.e("开始下载我的房间");
		HashMap<String, String> params = new HashMap<>();
		params.put("access_token", ConfigApplication.instance().getmAccessToken());
		params.put("type", "0");
		params.put("pageIndex", "0");
		params.put("pageSize", "200");// 给一个尽量大的值
		HttpUtils.getInstance().postServiceData(AppConfig.ROOM_LIST_HIS, params, new ChatArrayCallBack<MucRoom>(MucRoom.class) {
			@Override
			protected void onXError(String exception) {
				room_download_status = STATUS_FAILED;// 失败
				endDownload();
			}

			@Override
			protected void onSuccess(ArrayResult<MucRoom> result) {

				if (result.getResultCode() == Result.CODE_TOKEN_ERROR) {// 注册成功
					resetLogin();
					return;
				}
				L_.e("我的房间下载成功"+result.getData().toString());
				boolean success = ResultCode.defaultParser(result, true);
				List<MucRoom> data = result.getData();
				for (int i=0;i<data.size();i++){
					L_.e(data.get(i).toString());
				}
				if (success) {
					FriendDao.getInstance().addRooms(mHandler, mLoginUserId, result.getData(),
							() -> {
                                room_download_status = STATUS_SUCCESS;// 成功
                                endDownload();
                            });
				} else {
					room_download_status = STATUS_FAILED;// 失败
					endDownload();
				}
			}
		});

	}

}
