package com.sk.weichat.xmpp;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.baidu.cyberplayer.utils.L;
import com.sk.weichat.MyApplication;
import com.ymz.baselibrary.utils.L_;

import org.apache.harmony.javax.security.sasl.SaslException;
import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.XMPPTCPConnection;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.offline.OfflineMessageManager;
import org.jivesoftware.smackx.ping.PingManager;

import java.io.IOException;
import java.util.Random;


public class XmppConnectionManager {
	private static final String TAG = XmppConnectionManager.class.getSimpleName();
	private Context mContext;
	private XMPPConnection mConnection;
	private XReconnectionManager mReconnectionManager;
	/* 因为连接不再同一个线程，所以要用Handler和回调完成 */
	private NotifyConnectionListener mNotifyConnectionListener;

	/* Handler */
	private static final int MSG_CONNECTTING = 0;
	private static final int MSG_CONNECTED = 1;
	private static final int MSG_AUTHENTICATED = 2;
	private static final int MSG_CONNECTION_CLOSED = 3;
	private static final int MSG_CONNECTION_CLOSED_ON_ERROR = 4;

	@SuppressLint("HandlerLeak")
	private Handler mNotifyConnectionHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == MSG_CONNECTTING) {
				if (mNotifyConnectionListener != null) {
					mNotifyConnectionListener.notifyConnectting();
				}
			} else if (msg.what == MSG_CONNECTED) {
				XMPPConnection connection = (XMPPConnection) msg.obj;
				if (mNotifyConnectionListener != null) {
					mNotifyConnectionListener.notifyConnected(connection);
				}
			} else if (msg.what == MSG_AUTHENTICATED) {
				XMPPConnection connection = (XMPPConnection) msg.obj;
				if (mNotifyConnectionListener != null) {
					mNotifyConnectionListener.notifyAuthenticated(connection);
				}
			} else if (msg.what == MSG_CONNECTION_CLOSED) {
				if (mNotifyConnectionListener != null) {
					mNotifyConnectionListener.notifyConnectionClosed();
				}
			} else if (msg.what == MSG_CONNECTION_CLOSED_ON_ERROR) {
				if (mNotifyConnectionListener != null) {
					Exception e = (Exception) msg.obj;
					mNotifyConnectionListener.notifyConnectionClosedOnError(e);
				}
			}
		}
	};

	public XmppConnectionManager(Context context, NotifyConnectionListener listener) {
		mContext = context;
		mNotifyConnectionListener = listener;
		mConnection = new XMPPTCPConnection(getConnectionConfiguration());
		mConnection.addConnectionListener(mAbstractConnectionListener);
		initNetWorkStatusReceiver();
		mReconnectionManager = new XReconnectionManager(mContext, mConnection, true, mIsNetWorkActive);
	}

	public XMPPConnection getConnection() {
		return mConnection;
	}

	public boolean isAuthenticated() {
		return mConnection != null && mConnection.isConnected() && mConnection.isAuthenticated();
	}

	private ConnectivityManager mConnectivityManager;
	private boolean mIsNetWorkActive;// 当前网络是否连接上

	/********************* 网络连接状态 ***************/
	private void initNetWorkStatusReceiver() {
		// 获取程序启动时的网络状态
		mConnectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		mIsNetWorkActive = isGprsOrWifiConnected();
		// 注册网络监听广播
		IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
		mContext.registerReceiver(mNetWorkChangeReceiver, intentFilter);
	}

	private boolean isGprsOrWifiConnected() {
		NetworkInfo gprs = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		NetworkInfo wifi = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		boolean isConnectedGprs = gprs != null && gprs.isConnected();
		boolean isConnectedWifi = wifi != null && wifi.isConnected();
		return isConnectedGprs || isConnectedWifi;
	}

	private BroadcastReceiver mNetWorkChangeReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (!intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
				return;
			}
			final boolean isConnected = isGprsOrWifiConnected();
			if (mIsNetWorkActive != isConnected) {// 和之前的状态不同

				mIsNetWorkActive = isConnected;
				L_.e("XmppConnectionManager  -----网络状态改变了"+mIsNetWorkActive);
				// 网络状态改变了
				if (!mIsNetWorkActive) {// 由有网变为没网
					if (mLoginThread != null && mLoginThread.isAlive()) {
						mLoginThread.interrupt();
					}
				} else {
					if (isLoginAllowed()) {
						L_.e("XmppConnectionManager  -----网络状态改变了 login");
						login(mLoginUserId, mLoginPassword);
					}
				}
				mReconnectionManager.setNetWorkState(mIsNetWorkActive);
			}
		}
	};
	private AbstractConnectionListener mAbstractConnectionListener = new AbstractConnectionListener() {
		// 在连接成功时回调一次，在登陆成功时，也回调一次。
		@Override
		public void connected(XMPPConnection arg0) {
			Log.e(TAG, "Xmpp connected");
			Message msg = mNotifyConnectionHandler.obtainMessage(MSG_CONNECTED);
			msg.obj = arg0;
			msg.sendToTarget();
		}

		/*
		 * authenticated回调不到,因为aSmack内部并未实现该回调，在authenticated成功的情况下，aSmack依然回调connected方法。
		 */
		@Override
		public void authenticated(XMPPConnection arg0) {
			Log.e(TAG, "Xmpp authenticated");
			Message msg = mNotifyConnectionHandler.obtainMessage(MSG_AUTHENTICATED);
			msg.obj = arg0;
			msg.sendToTarget();
		}

		@Override
		public void connectionClosed() {
			Log.e(TAG, "Xmpp connectionClosed");
			mNotifyConnectionHandler.sendEmptyMessage(MSG_CONNECTION_CLOSED);
		}

		@Override
		public void connectionClosedOnError(Exception arg0) {
			Log.e(TAG, "Xmpp connectionClosedOnError");
			Message msg = mNotifyConnectionHandler.obtainMessage(MSG_CONNECTION_CLOSED_ON_ERROR);
			msg.obj = arg0;
			msg.sendToTarget();
		}

	};

	private String mXmppHost;
	private int mXmppPort;

	String getHost() {
		return mXmppHost;
	}

	int getPort() {
		return mXmppPort;
	}

	private ConnectionConfiguration getConnectionConfiguration() {
		mXmppHost = MyApplication.getInstance().getConfig().XMPPHost;
		mXmppPort = MyApplication.getInstance().getConfig().XMPP_PORT;
		ConnectionConfiguration configuration = new ConnectionConfiguration(mXmppHost, mXmppPort);
		configuration.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
		configuration.setCompressionEnabled(false);
		configuration.setSendPresence(false);
		configuration.setReconnectionAllowed(false);// 自己做重连操作
		configuration.setDebuggerEnabled(false);//是否调试模式
		return configuration;
	}

	private boolean doLogining = false;

	private boolean isLoginAllowed() {
		return doLogining && mIsNetWorkActive && (!mConnection.isConnected() || !mConnection.isAuthenticated());
	}

	private LoginThread mLoginThread;

	private String mLoginUserId;// 仅用于登陆失败，重新登陆用
	private String mLoginPassword;// 仅用于登陆失败，重新登陆用

	public void logout() {
		doLogining = false;
		if (mLoginThread != null && mLoginThread.isAlive()) {
			mLoginThread.interrupt();
		}
		mReconnectionManager.release();
		if (mConnection == null) {
			return;
		}

		if (mConnection.isConnected() && mConnection.isAuthenticated()) {
			presenceOffline();
		}
		if (mConnection.isConnected()) {
			mConnection.disconnect();
		}

	}

	public synchronized void login(final String userId, final String password) {
		Log.d("roamer", "login start");
		if (mConnection.isAuthenticated()) {// 如果已经登陆
			Log.d("roamer", "isAuthenticated true");
			if (StringUtils.parseName(mConnection.getUser()).equals(userId)) {// 如果登陆的用户和需要在登陆的是同一个用户，赋予可能改变的用户名和密码，返回
				return;
			} else {
				mConnection.disconnect();
			}
		}

		// 正在进行上一个用户的登陆中，或者用户密码变更，但是还在登陆中
		if (mLoginThread != null && mLoginThread.isAlive()) {
			Log.d("roamer", "mLoginThread isAlive");
			if (mLoginThread.isSameUser(userId, password)) {
				if (mLoginThread.getAttempts() > 13) {// 当尝试次数大于13的时候，尝试的时间变得太长，果断结束点，开始一次新的尝试
					mLoginThread.interrupt();
					doLogining = false;
				} else {
					return;
				}
			} else {// 和之前在尝试登陆的用户属性一致，结束这个登陆的线程
				mLoginThread.interrupt();
				doLogining = false;
			}
		}

		// 等待上一个登陆线程的结束，才开始下一个
		long time = System.currentTimeMillis();
		while (mLoginThread != null && mLoginThread.isAlive()) {
			if (System.currentTimeMillis() - time > 3000) {// 防止结束线程时异常了，卡住主线程
				break;
			}
		}

		doLogining = true;
		mLoginUserId = userId;
		mLoginPassword = password;
		if (!mIsNetWorkActive) {// 没有网，直接不尝试了
			return;
		}

		mLoginThread = new LoginThread(userId, password);
		mLoginThread.start();
	}

	void release() {
		mContext.unregisterReceiver(mNetWorkChangeReceiver);
		doLogining = false;
		if (mLoginThread != null && mLoginThread.isAlive()) {
			mLoginThread.interrupt();
		}
		mReconnectionManager.release();
		if (mConnection != null && mConnection.isConnected()) {
			mConnection.disconnect();
		}
	}

	private class LoginThread extends Thread {
		private String loginUserId;
		private String loginPassword;
		private int attempts;
		private int randomBase = new Random().nextInt(11) + 5; // between 5 and 15seconds

		public LoginThread(String loginUserId, String loginPassword) {
			this.loginUserId = loginUserId;
			this.loginPassword = loginPassword;
			this.setName("Xmpp Login Thread" + loginUserId);
		}

		public boolean isSameUser(String userId, String password) {
			return loginUserId.equals(userId) && loginPassword.equals(password);
		}

		public int getAttempts() {
			return attempts;
		}

		/**
		 * Returns the number of seconds until the next reconnection attempt.
		 * 
		 * @return the number of seconds until the next reconnection attempt.
		 */
		private int timeDelay() {
			attempts++;
			if (attempts > 13) {
				return randomBase * 6 * 5; // between 2.5 and 7.5 minutes
			}
			if (attempts > 7) {
				return randomBase * 6; // between 30 and 90 seconds (~1 minutes)
			}
			return randomBase; // 10 seconds
		}

		public void run() {
			while (isLoginAllowed()) {
				mNotifyConnectionHandler.sendEmptyMessage(MSG_CONNECTTING);
				Log.d("roamer", "login try");
				try {
					if (!mConnection.isConnected()) {
						((XMPPTCPConnection) mConnection).connectWithoutLogin();
					}
					if (mConnection.isConnected()) {
						PingManager.getInstanceFor(mConnection).setPingInterval(60);
						Log.e("LoginThread  :","登陆成功");
						mConnection.login(loginUserId, loginPassword, "youjob");
						presenceOnline();
					}
				} catch (SaslException e) {
					e.printStackTrace();
				} catch (SmackException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (XMPPException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}

				if (mConnection.isAuthenticated()) {
					if (!StringUtils.parseName(mConnection.getUser()).equals(loginUserId)) {
						mConnection.disconnect();
					} else {
						doLogining = false;
						mAbstractConnectionListener.authenticated(mConnection);
					}
				} else {
					// Find how much time we should wait until the next try
					int remainingSeconds = timeDelay();
					Log.d("roamer", "login try delay：remainingSeconds：" + remainingSeconds);
					while (isLoginAllowed() && remainingSeconds > 0) {
						Log.d("roamer", "login try delay");
						try {
							Thread.sleep(1000);
							remainingSeconds--;
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
					}
				}

			}
		}
	}


	public void handOfflineMessage() {
					Log.d("roamer","这里是去获取离线消息handofflineMessage");
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {

					Log.d("roamer","开始解析离线消息");
					OfflineMessageManager manager = new OfflineMessageManager(mConnection);
					manager.getMessages();
					manager.deleteMessages();
					presenceOnline();// 取得离线消息后，发送在线消息状态
				} catch (NoResponseException e) {
					Log.d("roamer", "NoResponseException");
					e.printStackTrace();
				} catch (XMPPErrorException e) {
					Log.d("roamer", "XMPPErrorException");
					Log.d("roamer","XMPPErrorException:[["+e.toString()+"]]");
					e.printStackTrace();
				} catch (NotConnectedException e) {
					Log.d("roamer","NotConnectedException");
					e.printStackTrace();
				}
			}
		}).start();
	}

	public void presenceOnline() {
		Presence presence = new Presence(Presence.Type.available);
		try {
			mConnection.sendPacket(presence);
		} catch (NotConnectedException e) {
			e.printStackTrace();
		}
	}

	public void presenceOffline() {
		Presence presence = new Presence(Presence.Type.unavailable);
		try {
			mConnection.sendPacket(presence);
		} catch (NotConnectedException e) {
			e.printStackTrace();
		}
	}

}
