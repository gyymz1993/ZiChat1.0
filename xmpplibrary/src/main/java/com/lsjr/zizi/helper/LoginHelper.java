package com.lsjr.zizi.helper;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;

import com.lsjr.bean.ObjectResult;
import com.lsjr.bean.Result;
import com.lsjr.callback.ChatObjectCallBack;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.AppConfig;

import com.lsjr.zizi.ConfigApplication;
import com.lsjr.zizi.bean.LoginAuto;
import com.lsjr.zizi.bean.LoginRegisterResult;
import com.lsjr.zizi.bean.ResultCode;
import com.lsjr.zizi.dao.UserDao;
import com.lsjr.zizi.db.User;
import com.lsjr.zizi.util.DeviceInfoUtil;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.UIUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 当前登陆用户的帮助类
 * 
 * 
 */
public class LoginHelper {
	public static final String ACTION_LOGIN = UIUtils.getPackageName() + ".action.login";// 登陆
	public static final String ACTION_LOGOUT = UIUtils.getPackageName() + ".action.logout";// 用户手动注销登出
	public static final String ACTION_CONFLICT = UIUtils.getPackageName()+ ".action.conflict";// 登陆冲突（另外一个设备登陆了）
	// 用户需要重新登陆，更新本地数据（可能是STATUS_USER_TOKEN_OVERDUE，STATUS_USER_NO_UPDATE，STATUS_USER_TOKEN_CHANGE三种状态之一）
	public static final String ACTION_NEED_UPDATE = UIUtils.getPackageName() + ".action.need_update";
	public static final String ACTION_LOGIN_GIVE_UP = UIUtils.getPackageName() + ".action.login_give_up";// 在下载资料的时候，没下载完就放弃登陆了

	// 获取登陆和登出的action filter
	public static IntentFilter getLogInOutActionFilter() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(ACTION_LOGIN);
		intentFilter.addAction(ACTION_LOGOUT);
		intentFilter.addAction(ACTION_CONFLICT);
		intentFilter.addAction(ACTION_NEED_UPDATE);
		intentFilter.addAction(ACTION_LOGIN_GIVE_UP);
		return intentFilter;
	}

	// 登陆广播,且登陆的用户为MyApplication.getInstance().mLoginUser
	public static void broadcastLogin(Context context) {
		Intent intent = new Intent(ACTION_LOGIN);
		context.sendBroadcast(intent);
	}

	// 登出广播
	public static void broadcastLogout(Context context) {
		Intent intent = new Intent(ACTION_LOGOUT);
		context.sendBroadcast(intent);
	}

	// 放弃登陆
	public static void broadcastLoginGiveUp(Context context) {
		Intent intent = new Intent(ACTION_LOGIN_GIVE_UP);
		context.sendBroadcast(intent);
	}

	// 登陆冲突（另外一个设备登陆了）
	public static void broadcastConflict(Context context) {
		L_.e("另外一个设备登陆了");
		Intent intent = new Intent(ACTION_CONFLICT);
		context.sendBroadcast(intent);
	}

	public static void broadcastNeedUpdate(Context context) {
		Intent intent = new Intent(ACTION_NEED_UPDATE);
		context.sendBroadcast(intent);
	}

	/* 信息完整程度由低到高，从第2级别开始，MyApplication中的mLoginUser是有值得 */
	/* 没有用户，即游客（不需要进行其他操作） */
	public static final int STATUS_NO_USER = 0;
	/* 有用户，但是不完整，只有手机号，可能是之前注销过（不需要进行其他操作） */
	public static final int STATUS_USER_SIMPLE_TELPHONE = 1;
	/* 有用户，但是本地Token已经过期了（需要弹出对话框提示：本地Token已经过期，重新登陆） */
	public static final int STATUS_USER_TOKEN_OVERDUE = 2;
	/*
	 * 有用户，本地Token未过期，但是可能信息不是最新的，即在上次登陆之后，没有更新完数据就退出了app （需要检测Token是否变更，变更即提示登陆，未变更则提示更新资料）
	 */
	public static final int STATUS_USER_NO_UPDATE = 3;

	/*
	 * 用户资料全部完整，但是Token已经变更了，提示重新登陆
	 */
	public static final int STATUS_USER_TOKEN_CHANGE = 4;

	/*
	 * 用户资料全部完整，但是还要检测Token是否变更 （需要检测Token是否变更，变更即提示登陆） 此状态比较特殊，因为有可能Token没有变更，不需要在进行登陆操作。<br/> 在检测Token接口调用失败的情况下，默认为一个不需要重新登陆的用户。
	 * 在检测Token接口调用成功的情况下，此状态会立即过度到STATUS_USER_VALIDATION状态。
	 */
	public static final int STATUS_USER_FULL = 5;

	/*
	 * 用户资料全部完整，并且已经检测Token没有变更，或者新登录更新完成 （不需要进行其他操作）
	 */
	public static final int STATUS_USER_VALIDATION = 6;//

	/* 进入MainActivity，判断当前是游客，还是之前已经登陆过的用户 */
	public static int prepareUser(Context context) {
		int userStatus = STATUS_NO_USER;
		boolean idIsEmpty = TextUtils.isEmpty(UserSp.getInstance().getUserId());
		boolean telephoneIsEmpty = TextUtils.isEmpty(UserSp.getInstance().getTelephone());

		if (!idIsEmpty && !telephoneIsEmpty) {// 用户标识都不为空，那么就能代表一个完整的用户
			// 进入之前，加载本地已经存在的数据
			String userId = UserSp.getInstance().getUserId();
			User user = UserDao.getInstance().getUserByUserId(userId);
			if (!LoginHelper.isUserValidation(user)) {// 用户数据错误,那么就认为是一个游客
				userStatus = STATUS_NO_USER;
			} else {
				ConfigApplication.instance().mLoginUser = user;
				ConfigApplication.instance().mAccessToken = UserSp.getInstance().getAccessToken(null);
				ConfigApplication.instance().mExpiresIn = UserSp.getInstance().getExpiresIn(0);

				if (LoginHelper.isTokenValidation()) {// Token未过期
					boolean isUpdate = UserSp.getInstance().isUpdate(true);
					if (isUpdate) {
						userStatus = STATUS_USER_FULL;
					} else {
						userStatus = STATUS_USER_NO_UPDATE;
					}
				} else {// Token过期
					userStatus = STATUS_USER_TOKEN_OVERDUE;
				}
			}
		} else if (!idIsEmpty) {// （适用于切换账号之后的操作）手机号不为空
			userStatus = STATUS_USER_SIMPLE_TELPHONE;
		} else {
			userStatus = STATUS_NO_USER;
		}
		ConfigApplication.instance().mUserStatus = userStatus;
		return userStatus;
	}

	// User数据是否能代表一个有效的用户
	public static boolean isUserValidation(User user) {
		if (user == null) {
			return false;
		}
		if (TextUtils.isEmpty(user.getUserId())) {
			return false;
		}
		if (TextUtils.isEmpty(user.getTelephone())) {
			return false;
		}
		if (TextUtils.isEmpty(user.getPassword())) {
			return false;
		}
		if (TextUtils.isEmpty(user.getNickName())) {
			return false;
		}
		return true;
	}

	/**
	 * AccessToken 是否是有效的
	 * 
	 * @return
	 */
	public static boolean isTokenValidation() {
		if (TextUtils.isEmpty(ConfigApplication.instance().mAccessToken)) {
			return false;
		}
		if (ConfigApplication.instance().mExpiresIn < System.currentTimeMillis()) {
			return false;
		}
		return true;
	}

	public static boolean setLoginUser(Context context, String telephone, String password, ObjectResult<LoginRegisterResult> result) {
		if (result == null) {
			return false;
		}
		if (result.getResultCode() != Result.CODE_SUCCESS) {
			return false;
		}
		if (result.getData() == null) {
			return false;
		}
		// 保存当前登陆的用户信息和Token信息作为全局变量，方便调用
		User user = ConfigApplication.instance().mLoginUser;
		user.setTelephone(telephone);
		user.setPassword(password);
		user.setUserId(result.getData().getUserId());
		user.setNickName(result.getData().getNickName());
		if (!LoginHelper.isUserValidation(user)) {// 请求下来的用户数据不完整有错误
			return false;
		}

		ConfigApplication.instance().mAccessToken = result.getData().getAccess_token();
		long expires_in = result.getData().getExpires_in() * 1000L + System.currentTimeMillis();
		// long expires_in = 60 * 1000L + System.currentTimeMillis();// 测试 1分钟就过期的Token
		ConfigApplication.instance().mExpiresIn = expires_in;
		if (result.getData().getLogin() != null) {
			user.setOfflineTime(result.getData().getLogin().getOfflineTime());
		}

		// 保存基本信息到数据库
		boolean saveAble = UserDao.getInstance().saveUserLogin(user);
		if (!saveAble) {
			return false;
		}
		// 保存最后一次登录的用户信息到Sp，用于免登陆
		UserSp.getInstance().setUserId(result.getData().getUserId());
		UserSp.getInstance().setTelephone(telephone);
		UserSp.getInstance().setAccessToken(result.getData().getAccess_token());
		UserSp.getInstance().setExpiresIn(expires_in);
		ConfigApplication.instance().mUserStatusChecked = true;
		ConfigApplication.instance().mUserStatus = STATUS_USER_VALIDATION;
		return true;
	}

	public static interface OnCheckedFailedListener {// 检查Token失败才回调，然后外部在继续下一次检测
		void onCheckFailed();
	}

	/**
	 * 检测Status，是否要弹出更新用户状态的对话框
	 * 
	 * @param activity
	 *            是否弹出对话框有本方法内部逻辑决定。<br/>
	 *            是否继续循环检测（检测未成功的情况下），<br/>
	 *            由 MyApplication.getInstance().mUserStatusChecked值决定，（ 因为方法内部有异步操作，不能直接返回值来判断） 在MainActivity中进行重复检测的判定
	 */
	public static void checkStatusForUpdate(final AppCompatActivity activity, final OnCheckedFailedListener listener) {
		if (ConfigApplication.instance().mUserStatusChecked) {
			return;
		}
		final int status = ConfigApplication.instance().mUserStatus;
		if (status == STATUS_NO_USER || status == STATUS_USER_SIMPLE_TELPHONE) {
			ConfigApplication.instance().mUserStatusChecked = true;
			return;
		}

		final User user = ConfigApplication.instance().mLoginUser;
		if (!isUserValidation(user)) {// 如果是用户数据不完整，那么就可能是数据错误，将状态变为STATUS_NO_USER和STATUS_USER_SIMPLE_TELPHONE
			boolean telephoneIsEmpty = TextUtils.isEmpty(UserSp.getInstance().getTelephone());
			if (telephoneIsEmpty) {
				ConfigApplication.instance().mUserStatus = STATUS_NO_USER;
			} else {
				ConfigApplication.instance().mUserStatus = STATUS_USER_SIMPLE_TELPHONE;
			}
			return;
		}

		if (status == STATUS_USER_VALIDATION) {
			ConfigApplication.instance().mUserStatusChecked = true;
			broadcastLogin(activity);
			return;
		}

		if (status == STATUS_USER_TOKEN_CHANGE) {// Token已经变更，直接提示，不需要再检测Token是否变更
			ConfigApplication.instance().mUserStatusChecked = true;
			broadcastNeedUpdate(activity);
			return;
		}

		/**
		 * 能往下执行的只有三种状态 <br/>
		 * public static final int STATUS_USER_TOKEN_OVERDUE = 2;<br/>
		 * public static final int STATUS_USER_NO_UPDATE = 3;<br/>
		 * public static final int STATUS_USER_FULL = 5;<br/>
		 */

		Map<String, String> params = new HashMap<>();
		params.put("access_token", ConfigApplication.instance().mAccessToken);
		params.put("userId", user.getUserId());
		params.put("serial", DeviceInfoUtil.getDeviceId(activity));

		// 地址信息
		//double latitude = ConfigApplication.instance().getBdLocationHelper().getLatitude();
		//double longitude = ConfigApplication.instance().getBdLocationHelper().getLongitude();
//		if (latitude != 0)
			params.put("latitude", String.valueOf(100));
//		if (longitude != 0)
			params.put("longitude", String.valueOf(100));

		HttpUtils.getInstance().postServiceData(AppConfig.USER_LOGIN_AUTO, params, new ChatObjectCallBack<LoginAuto>(LoginAuto.class) {
			@Override
			protected void onXError(String exception) {

			}

			@Override
			protected void onSuccess(ObjectResult<LoginAuto> result) {

				boolean success = ResultCode.defaultParser(result, false);
				if (success && result.getData() != null) {
					ConfigApplication.instance().mUserStatusChecked = true;// 检测Token成功
					int tokenExists = result.getData().getTokenExists();// 1=令牌存在、0=令牌不存在
					int serialStatus = result.getData().getSerialStatus();// 1=没有设备号、2=设备号一致、3=设备号不一致
					Log.d("wang","tokenExists"+tokenExists);
					Log.d("wang","serialStatus"+serialStatus);
					if (serialStatus == 2) {// 设备号一致，说明没有切换过设备
						if (tokenExists == 1) {// Token也存在，说明不用登陆了
							if (status == STATUS_USER_FULL) {// 本地数据完整，那么就免登陆使用
								ConfigApplication.instance().mUserStatus = STATUS_USER_VALIDATION;
							} else {
								// do no thing 依然保持其他的状态
							}
						} else {// Token 不存在
							if (status == STATUS_USER_FULL) {// 数据也完整，那么就是Token过期
								ConfigApplication.instance().mUserStatus = STATUS_USER_TOKEN_OVERDUE;
							} else {
								// do no thing 依然保持其他的状态
							}
						}
					} else {// 设备号不一致，那么就是切换过手机
						ConfigApplication.instance().mUserStatus = STATUS_USER_TOKEN_CHANGE;
					}

					// 最后判断是否要跳转弹出对话框
					if (ConfigApplication.instance().mUserStatus != STATUS_USER_VALIDATION) {
						broadcastNeedUpdate(activity);
					} else {// 如果是用户已经完整验证，那么发出用户登录的广播
						broadcastLogin(activity);
					}
				} else {
					if (listener != null) {
						listener.onCheckFailed();
					}
				}
			}
		});
	}

}
