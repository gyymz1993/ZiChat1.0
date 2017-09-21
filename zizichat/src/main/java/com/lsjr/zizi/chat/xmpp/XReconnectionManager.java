package com.lsjr.zizi.chat.xmpp;

import android.content.Context;
import android.text.TextUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lsjr.bean.Result;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.chat.bean.LoginAuto;
import com.lsjr.zizi.chat.helper.LoginHelper;
import com.lsjr.zizi.chat.helper.UserSp;
import com.lsjr.zizi.util.DeviceInfoUtil;
import com.ymz.baselibrary.utils.L_;

import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.StreamErrorException;
import org.jivesoftware.smack.XMPPTCPConnection;
import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.util.StringUtils;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

public class XReconnectionManager extends AbstractConnectionListener {

	// Holds the connection to the server
	private XMPPConnection mConnection;
	private boolean isReconnectionAllowed = false;

	private Thread mReconnectionThread;

	boolean mIsNetWorkActive;
	// Holds the state of the reconnection
	boolean doReconnecting = false;
	Context mContext;

	public XReconnectionManager(Context context, XMPPConnection connection, boolean reconnectionAllowed, boolean isNetWorkActive) {
		mContext = context;
		mConnection = connection;
		mConnection.addConnectionListener(this);
		isReconnectionAllowed = reconnectionAllowed;
		mIsNetWorkActive = isNetWorkActive;
	}

	/**
	 * Returns true if the reconnection mechanism is enabled.
	 * 
	 * @return true if automatic reconnections are allowed.
	 */
	private boolean isReconnectionAllowed() {
		return doReconnecting && mIsNetWorkActive && !mConnection.isConnected() && isReconnectionAllowed;
	}

	public void setNetWorkState(boolean isNetWorkActive) {
		mIsNetWorkActive = isNetWorkActive;
		if (mIsNetWorkActive) {// 网络状态变为可用
			if (isReconnectionAllowed()) {
				reconnect();
			}
		} else {
			if (mReconnectionThread != null && mReconnectionThread.isAlive()) {
				mReconnectionThread.interrupt();
			}
		}
	}

	/**
	 * Starts a reconnection mechanism if it was configured to do that. The algorithm is been executed when the first connection error is detected.
	 * <p/>
	 * The reconnection mechanism will try to reconnect periodically in this way:
	 * <ol>
	 * <li>First it will try 6 times every 10 seconds.
	 * <li>Then it will try 10 times every 1 minute.
	 * <li>Finally it will try indefinitely every 5 minutes.
	 * </ol>
	 */
	private synchronized void reconnect() {
		if (this.isReconnectionAllowed()) {
			// Since there is no thread running, creates a new one to attempt
			// the reconnection.
			// avoid to run duplicated reconnectionThread -- fd: 16/09/2010
			if (mReconnectionThread != null && mReconnectionThread.isAlive())
				return;
			mReconnectionThread = new Thread() {

				private int mRandomBase = new Random().nextInt(11) + 5; // between 5 and 15 seconds
				/**
				 * Holds the current number of reconnection attempts
				 */
				private int attempts = 0;

				/**
				 * Returns the number of seconds until the next reconnection attempt.
				 * 
				 * @return the number of seconds until the next reconnection attempt.
				 */
				private int timeDelay() {
					attempts++;
					if (attempts > 13) {
						return mRandomBase * 6 * 5; // between 2.5 and 7.5
													// minutes (~5 minutes)
					}
					if (attempts > 7) {
						return mRandomBase * 6; // between 30 and 90 seconds (~1
												// minutes)
					}
					return mRandomBase; // 10 seconds
				}

				/**
				 * The process will try the reconnection until the connection succeed or the user cancel it
				 */
				public void run() {
					// 重新连接之前，先检查Token状态
					int checkTokenStatus = 0;
					while (isReconnectionAllowed() && checkTokenStatus == 0) {
						checkTokenStatus = syncCheckToken();
						if (checkTokenStatus == 0) {// 表示检查失败，继续循环检查
							try {
								Thread.sleep(5000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						} else if (checkTokenStatus == 1) {// 表示检查成功Token过期（或出现不能继续请求Token状态的异常），停止重新登陆
							doReconnecting = false;
							conflict();
						} else if (checkTokenStatus == 2) {// 2、表示检查成功，Token没有改变，可以继续下面的重新登陆
							break;
						}
					}

					while (isReconnectionAllowed()) {

						// Makes a reconnection attempt
						try {
							if (isReconnectionAllowed()) {
								mConnection.connect();
							}
						} catch (Exception e) {
							notifyReconnectionFailed(e);// Fires the failed reconnection notification
						}

						int remainingSeconds = timeDelay();
						while (isReconnectionAllowed() && remainingSeconds > 0) {
							try {
								Thread.sleep(1000);
								remainingSeconds--;
								notifyAttemptToReconnectIn(remainingSeconds);
							} catch (InterruptedException e1) {
								// Notify the reconnection has failed
								notifyReconnectionFailed(e1);
							}
						}

					}
				}
			};
			mReconnectionThread.setName("Smack XReconnectionManager");
			mReconnectionThread.setDaemon(true);
			mReconnectionThread.start();
		}
	}

	/**
	 * 在检查Token的时候，发现冲突了
	 */
	private void conflict() {
		((CoreService) mContext).logout();
		L_.e("另外一个设备登陆了");
		LoginHelper.broadcastConflict(mContext);
	}

	/**
	 * 检查Token状态，并返回int值<br/>
	 * 0、表示检查失败，继续循环检查 <br/>
	 * 1、表示检查成功Token过期（或出现不能继续请求Token状态的异常），停止重新登陆<br/>
	 * 2、表示检查成功，Token没有改变，可以继续下面的重新登陆<br/>
	 * 
	 * @return
	 */
	private int syncCheckToken() {// 同步网络请求Token
		L_.e(CoreService.TAG, "开始重新登陆前的 Token 状态检查");
		String requestUrl = AppConfig.USER_LOGIN_AUTO;
		HttpURLConnection httpConn = null;
		DataOutputStream out = null;
		InputStream is = null;
		try {
			URL url = new URL(requestUrl);
			httpConn = (HttpURLConnection) url.openConnection();
			httpConn.setDoOutput(true);
			httpConn.setDoInput(true);
			httpConn.setConnectTimeout(5 * 1000);
			httpConn.setReadTimeout(5 * 1000);
			httpConn.setRequestMethod("POST");
			httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
			out = new DataOutputStream(httpConn.getOutputStream());
			// 参数
			String access_token = ConfigApplication.instance().mAccessToken;
			if (TextUtils.isEmpty(access_token)) {
				access_token = UserSp.getInstance().getAccessToken(null);
			}

			L_.e("access_token----->"+access_token);
			if (TextUtils.isEmpty(access_token)) {
				return 1;
			}

			String serial = DeviceInfoUtil.getDeviceId(mContext);
			if (TextUtils.isEmpty(serial)) {
				return 1;
			}
			if (CoreService.DEBUG) {
				L_.e(CoreService.TAG, "requestUrl:" + requestUrl);
				L_.e(CoreService.TAG, "access_token:" + access_token);
				L_.e(CoreService.TAG, "serial:" + serial);
			}

			StringBuilder sb = new StringBuilder();
			sb.append("access_token=" + access_token + "&");
			String user = ((XMPPTCPConnection) mConnection).getDirectUser();
			if (user == null) {
				return 1;
			}
			sb.append("userId=" + StringUtils.parseName(user) + "&");
			sb.append("serial=" + serial);
			out.write(sb.toString().getBytes("UTF-8"));
			out.flush();

			int statusCode = httpConn.getResponseCode();
			if (statusCode != 200) {
				return 0;
			}
			is = httpConn.getInputStream();
			if (is == null) {
				return 0;
			}
			StringBuffer buffer = new StringBuffer();
			int len = -1;
			byte[] data = new byte[1024];
			try {
				while ((len = is.read(data)) != -1) {
					buffer.append(new String(data, 0, len));
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			String result = buffer.toString();
			if (CoreService.DEBUG) {
				L_.e(CoreService.TAG, "检查状态result:" + result);
			}

			if (TextUtils.isEmpty(result)) {
				return 0;
			}

			try {
				JSONObject jsonObject = JSON.parseObject(result);
				int resultCode = jsonObject.getIntValue(Result.RESULT_CODE);
				if (resultCode != 1) {
					return 0;
				}

				LoginAuto loginAuto = JSON.parseObject(jsonObject.getString(Result.DATA), LoginAuto.class);
				if (loginAuto != null) {// 判断时候要继续重新登陆
					int tokenExists = loginAuto.getTokenExists();// 1=令牌存在、0=令牌不存在
					int serialStatus = loginAuto.getSerialStatus();// 1=没有设备号、2=设备号一致、3=设备号不一致
					if (serialStatus == 2) {// 设备号一致，说明没有切换过设备
						if (tokenExists == 1) {// Token存在，
							return 2;
						} else {// Token 不存在
							return 1;
						}
					} else {// 设备号不一致，那么就是切换过手机
						return 1;
					}
				} else {
					return 0;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null) {
					out.close();
				}
				if (is != null) {
					is.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (httpConn != null) {
				httpConn.disconnect();
			}
		}
		return 0;
	}

	private void notifyReconnectionFailed(Exception exception) {
		if (isReconnectionAllowed()) {
			for (ConnectionListener listener : mConnection.getConnectionListeners()) {
				listener.reconnectionFailed(exception);
			}
		}
	}

	private void notifyAttemptToReconnectIn(int seconds) {
		if (isReconnectionAllowed()) {
			for (ConnectionListener listener : mConnection.getConnectionListeners()) {
				listener.reconnectingIn(seconds);
			}
		}
	}

	void release() {
		doReconnecting = false;
		if (mReconnectionThread != null && mReconnectionThread.isAlive()) {
			mReconnectionThread.interrupt();
		}
	}

	@Override
	public void connectionClosed() {
		doReconnecting = false;
	}

	@Override
	public void connectionClosedOnError(Exception e) {
		doReconnecting = true;
		if (e instanceof StreamErrorException) {// 有人重复登陆
			StreamErrorException xmppEx = (StreamErrorException) e;
			StreamError error = xmppEx.getStreamError();
			String reason = error.getCode();

			if ("conflict".equals(reason)) {// 发出下线通知
				if (CoreService.DEBUG)
					L_.e(CoreService.TAG, "异常断开，有另外设备登陆啦");
				conflict();
				doReconnecting = false;
				return;
			}

		}
		// 因为其他原因导致下线，那么就开始重连
		if (this.isReconnectionAllowed()) {
			if (CoreService.DEBUG)
				L_.e(CoreService.TAG, "异常断开，开始重连");
			this.reconnect();
		}
	}
}
