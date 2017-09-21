package com.sk.weichat.helper;

import java.util.HashMap;
import java.util.Map;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.sk.weichat.AppConfig;
import com.sk.weichat.MyApplication;
import com.sk.weichat.bean.LoginAuto;
import com.sk.weichat.bean.LoginRegisterResult;
import com.sk.weichat.bean.User;
import com.sk.weichat.db.dao.UserDao;
import com.sk.weichat.sp.UserSp;
import com.sk.weichat.ui.MainActivity;
import com.sk.weichat.util.DeviceInfoUtil;
import com.sk.weichat.volley.ObjectResult;
import com.sk.weichat.volley.Result;
import com.sk.weichat.volley.StringJsonObjectRequest;

/**
 * 当前登陆用户的帮助类
 * 
 * 
 */
public class LoginHelper {
	public static final String ACTION_LOGIN = AppConfig.sPackageName + ".action.login";// 登陆
	public static final String ACTION_LOGOUT = AppConfig.sPackageName + ".action.logout";// 用户手动注销登出
	public static final String ACTION_CONFLICT = AppConfig.sPackageName + ".action.conflict";// 登陆冲突（另外一个设备登陆了）
	// 用户需要重新登陆，更新本地数据（可能是STATUS_USER_TOKEN_OVERDUE，STATUS_USER_NO_UPDATE，STATUS_USER_TOKEN_CHANGE三种状态之一）
	public static final String ACTION_NEED_UPDATE = AppConfig.sPackageName + ".action.need_update";
	public static final String ACTION_LOGIN_GIVE_UP = AppConfig.sPackageName + ".action.login_give_up";// 在下载资料的时候，没下载完就放弃登陆了

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
		boolean idIsEmpty = TextUtils.isEmpty(UserSp.getInstance(context).getUserId(""));
		boolean telephoneIsEmpty = TextUtils.isEmpty(UserSp.getInstance(context).getTelephone(null));

		if (!idIsEmpty && !telephoneIsEmpty) {// 用户标识都不为空，那么就能代表一个完整的用户
			// 进入之前，加载本地已经存在的数据
			String userId = UserSp.getInstance(context).getUserId("");
			User user = UserDao.getInstance().getUserByUserId(userId);

			if (!LoginHelper.isUserValidation(user)) {// 用户数据错误,那么就认为是一个游客
				userStatus = STATUS_NO_USER;
			} else {
				MyApplication.getInstance().mLoginUser = user;
				MyApplication.getInstance().mAccessToken = UserSp.getInstance(context).getAccessToken(null);
				MyApplication.getInstance().mExpiresIn = UserSp.getInstance(context).getExpiresIn(0);

				if (LoginHelper.isTokenValidation()) {// Token未过期
					boolean isUpdate = UserSp.getInstance(context).isUpdate(true);
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
		MyApplication.getInstance().mUserStatus = userStatus;
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
		if (TextUtils.isEmpty(MyApplication.getInstance().mAccessToken)) {
			return false;
		}
		if (MyApplication.getInstance().mExpiresIn < System.currentTimeMillis()) {
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
		User user = MyApplication.getInstance().mLoginUser;
		user.setTelephone(telephone);
		user.setPassword(password);
		user.setUserId(result.getData().getUserId());
		user.setNickName(result.getData().getNickName());
		// user.setCompanyId(result.getData().getCompanyId());
		if (!LoginHelper.isUserValidation(user)) {// 请求下来的用户数据不完整有错误
			return false;
		}

		MyApplication.getInstance().mAccessToken = result.getData().getAccess_token();
		long expires_in = result.getData().getExpires_in() * 1000L + System.currentTimeMillis();
		// long expires_in = 60 * 1000L + System.currentTimeMillis();// 测试 1分钟就过期的Token
		MyApplication.getInstance().mExpiresIn = expires_in;

		if (result.getData().getLogin() != null) {
			user.setOfflineTime(result.getData().getLogin().getOfflineTime());
		}

		// 保存基本信息到数据库
		boolean saveAble = UserDao.getInstance().saveUserLogin(user);
		if (!saveAble) {
			return false;
		}

		// 保存最后一次登录的用户信息到Sp，用于免登陆
		UserSp.getInstance(MyApplication.getInstance()).setUserId(result.getData().getUserId());
		UserSp.getInstance(MyApplication.getInstance()).setTelephone(telephone);
		UserSp.getInstance(MyApplication.getInstance()).setAccessToken(result.getData().getAccess_token());
		UserSp.getInstance(MyApplication.getInstance()).setExpiresIn(expires_in);
		MyApplication.getInstance().mUserStatusChecked = true;
		MyApplication.getInstance().mUserStatus = STATUS_USER_VALIDATION;

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
	public static void checkStatusForUpdate(final MainActivity activity, final OnCheckedFailedListener listener) {
		if (MyApplication.getInstance().mUserStatusChecked) {
			return;
		}
		final int status = MyApplication.getInstance().mUserStatus;
		if (status == STATUS_NO_USER || status == STATUS_USER_SIMPLE_TELPHONE) {
			MyApplication.getInstance().mUserStatusChecked = true;
			return;
		}

		final User user = MyApplication.getInstance().mLoginUser;
		if (!isUserValidation(user)) {// 如果是用户数据不完整，那么就可能是数据错误，将状态变为STATUS_NO_USER和STATUS_USER_SIMPLE_TELPHONE
			boolean telephoneIsEmpty = TextUtils.isEmpty(UserSp.getInstance(activity).getTelephone(null));
			if (telephoneIsEmpty) {
				MyApplication.getInstance().mUserStatus = STATUS_NO_USER;
			} else {
				MyApplication.getInstance().mUserStatus = STATUS_USER_SIMPLE_TELPHONE;
			}
			return;
		}

		if (status == STATUS_USER_VALIDATION) {
			MyApplication.getInstance().mUserStatusChecked = true;
			broadcastLogin(activity);
			return;
		}

		if (status == STATUS_USER_TOKEN_CHANGE) {// Token已经变更，直接提示，不需要再检测Token是否变更
			MyApplication.getInstance().mUserStatusChecked = true;
			broadcastNeedUpdate(activity);
			return;
		}

		/**
		 * 能往下执行的只有三种状态 <br/>
		 * public static final int STATUS_USER_TOKEN_OVERDUE = 2;<br/>
		 * public static final int STATUS_USER_NO_UPDATE = 3;<br/>
		 * public static final int STATUS_USER_FULL = 5;<br/>
		 */

		Map<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("userId", user.getUserId());
		params.put("serial", DeviceInfoUtil.getDeviceId(activity));

		// 地址信息
		double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
		double longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
		if (latitude != 0)
			params.put("latitude", String.valueOf(latitude));
		if (longitude != 0)
			params.put("longitude", String.valueOf(longitude));

		StringJsonObjectRequest<LoginAuto> request = new StringJsonObjectRequest<LoginAuto>(MyApplication.getInstance().getConfig().USER_LOGIN_AUTO,
				new Response.ErrorListener() {

					@Override
					public void onErrorResponse(VolleyError arg0) {
						Log.d("wang","检查token错误");
						if (listener != null) {
							listener.onCheckFailed();
						}
					}
				}, new StringJsonObjectRequest.Listener<LoginAuto>() {
					@Override
					public void onResponse(ObjectResult<LoginAuto> result) {
						boolean success = Result.defaultParser(activity, result, false);
						if (success && result.getData() != null) {
							MyApplication.getInstance().mUserStatusChecked = true;// 检测Token成功
							int tokenExists = result.getData().getTokenExists();// 1=令牌存在、0=令牌不存在
							int serialStatus = result.getData().getSerialStatus();// 1=没有设备号、2=设备号一致、3=设备号不一致
							Log.d("wang","tokenExists"+tokenExists);
							Log.d("wang","serialStatus"+serialStatus);
							if (serialStatus == 2) {// 设备号一致，说明没有切换过设备
								if (tokenExists == 1) {// Token也存在，说明不用登陆了
									if (status == STATUS_USER_FULL) {// 本地数据完整，那么就免登陆使用
										MyApplication.getInstance().mUserStatus = STATUS_USER_VALIDATION;
									} else {
										// do no thing 依然保持其他的状态
									}
								} else {// Token 不存在
									if (status == STATUS_USER_FULL) {// 数据也完整，那么就是Token过期
										MyApplication.getInstance().mUserStatus = STATUS_USER_TOKEN_OVERDUE;
									} else {
										// do no thing 依然保持其他的状态
									}
								}
							} else {// 设备号不一致，那么就是切换过手机
								MyApplication.getInstance().mUserStatus = STATUS_USER_TOKEN_CHANGE;
							}

							// 最后判断是否要跳转弹出对话框
							if (MyApplication.getInstance().mUserStatus != STATUS_USER_VALIDATION) {
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
				}, LoginAuto.class, params);
		request.setTag("checkStatus");
		activity.addDefaultRequest(request);
	}

}
