package com.sk.weichat.ui;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PersistableBundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;
import com.sk.weichat.MyApplication;
import com.sk.weichat.NetWorkObservable.NetWorkObserver;
import com.sk.weichat.R;
import com.sk.weichat.bean.User;
import com.sk.weichat.bean.message.NewFriendMessage;
import com.sk.weichat.broadcast.CardcastUiUpdateUtil;
import com.sk.weichat.broadcast.MsgBroadcast;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.db.dao.UserDao;
import com.sk.weichat.helper.LoginHelper;
import com.sk.weichat.ui.account.LoginHistoryActivity;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.cardcast.AttentionFragment;
import com.sk.weichat.ui.cardcast.FriendFragment;
import com.sk.weichat.ui.cardcast.RoomFragment;
import com.sk.weichat.ui.circle.BusinessCircleFragment;
import com.sk.weichat.ui.find.MyFriendFragment;
import com.sk.weichat.ui.groupchat.GroupChatFragment;
import com.sk.weichat.ui.me.MeFragment;
import com.sk.weichat.ui.message.MessageFragment;
import com.sk.weichat.ui.nearby.NearbyFragment;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.PreferenceUtils;
import com.sk.weichat.view.DivideRadioGroup;
import com.sk.weichat.xmpp.CoreService;
import com.sk.weichat.xmpp.CoreService.CoreServiceBinder;
import com.sk.weichat.xmpp.ListenerManager;
import com.sk.weichat.xmpp.listener.AuthStateListener;

/**
 * 
 * @项目名称: SkWeiChat-Baidu
 * @包名: com.sk.weichat.ui
 * @作者:王阳
 * @创建时间: 2015年10月16日 下午3:14:20
 * @描述: Fragment所寄宿的Activity
 * @SVN版本号: $Rev$
 * @修改人: $Author$
 * @修改时间: $Date$
 * @修改的内容: TODO
 */
public class MainActivity extends BaseActivity implements NetWorkObserver, AuthStateListener{
	private static final String TAG_MESSAGE = "message";
	private static final String TAG_MY = "myfriend";
	private static final String TAG_NEARBY = "nearby";
	private static final String TAG_GROUP_CHAT = "group_chat";
	private static final String TAG_ME = "me";
	private static final String TAG_BusinessCircle = "my_BusinessCircle";
	
	private boolean mBind;
	private CoreService mXmppService;

	private ActivityManager mActivityManager;

	// 界面组件
	private DivideRadioGroup mTabRadioGroup;
	// 因为RadioGroup的check方法，会调用onCheckChange两次，用mLastFragment保存最后一次添加的fragment，防止重复add
	// fragment 出错
	private Fragment mLastFragment;
	private MessageFragment mMessageFragment;
	private MyFriendFragment mMyFriendFragment;
	private NearbyFragment mNearbyFragment;
	private GroupChatFragment mGroupChatFragment;
	private MeFragment mMeFragment;
	private BusinessCircleFragment mBusinessCircleFragment;
	private FriendFragment mFriend;// 互相关注
	private AttentionFragment mAttention;// 单向关注
	private RoomFragment mRoom;// 关注房间

	//
	private boolean mXmppBind;
	private CoreService mCoreService;
	private boolean isPause = true;// 界面是否暂停

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayShowHomeEnabled(false);
		getSupportActionBar().setDisplayHomeAsUpEnabled(false);
		setContentView(R.layout.activity_main);

		//百度推送
		PushManager.startWork(getApplicationContext(),PushConstants.LOGIN_TYPE_API_KEY,"8h0GOjOlgP8dXRzp9nG1dGBT");
		mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		}
		initView(savedInstanceState);

		// 注册网络改变回调
		MyApplication.getInstance().registerNetWorkObserver(this);
		// 绑定监听
		ListenerManager.getInstance().addAuthStateChangeListener(this);
		// 注册消息更新广播
		IntentFilter msgIntentFilter = new IntentFilter();
		msgIntentFilter.addAction(MsgBroadcast.ACTION_MSG_NUM_UPDATE);
		msgIntentFilter.addAction(MsgBroadcast.ACTION_MSG_NUM_RESET);
		registerReceiver(mUpdateUnReadReceiver, msgIntentFilter);
		// 注册用户登录状态广播
		registerReceiver(mUserLogInOutReceiver, LoginHelper.getLogInOutActionFilter());

		// 绑定服务
		mXmppBind = bindService(CoreService.getIntent(), mXmppServiceConnection, BIND_AUTO_CREATE);

		// 检查用户的状态，做不同的初始化工作
		User loginUser = MyApplication.getInstance().mLoginUser;
		if (!LoginHelper.isUserValidation(loginUser)) {
			LoginHelper.prepareUser(this);
		}

		if (!MyApplication.getInstance().mUserStatusChecked) {// 用户状态没有检测，那么开始检测
			mUserCheckHander.sendEmptyMessageDelayed(MSG_USER_CHECK, mRetryCheckDelay);
		} else {
			if (MyApplication.getInstance().mUserStatus == LoginHelper.STATUS_USER_VALIDATION) {
				LoginHelper.broadcastLogin(this);
			} else {// 重新检测
				MyApplication.getInstance().mUserStatusChecked = false;
				mUserCheckHander.sendEmptyMessageDelayed(MSG_USER_CHECK, mRetryCheckDelay);
			}
		}
		registerReceiver(mUpdateReceiver, CardcastUiUpdateUtil.getUpdateActionFilter());
		mBind = bindService(CoreService.getIntent(), mServiceConnection, BIND_AUTO_CREATE);
	}

	/* UserCheck */
	private static final int MSG_USER_CHECK = 0x1;
	private static final int RETRY_CHECK_DELAY_MAX = 30000;// 为成功的情况下，最长30s检测一次
	private int mRetryCheckDelay = 0;
	private Handler mUserCheckHander = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.what == MSG_USER_CHECK) {
				if (mRetryCheckDelay < RETRY_CHECK_DELAY_MAX) {
					mRetryCheckDelay += 5000;
				}
				mUserCheckHander.removeMessages(RETRY_CHECK_DELAY_MAX);
				doUserCheck();
			}
		}
	};

	private void doUserCheck() {
		if (!MyApplication.getInstance().isNetworkActive()) {
			return;
		}
		if (MyApplication.getInstance().mUserStatusChecked) {
			return;
		}
		LoginHelper.checkStatusForUpdate(this, new LoginHelper.OnCheckedFailedListener() {
			@Override
			public void onCheckFailed() {
				mUserCheckHander.sendEmptyMessageDelayed(MSG_USER_CHECK, mRetryCheckDelay);
			}
		});
	}

	private void cancelUserCheckIfExist() {
		mUserCheckHander.removeMessages(RETRY_CHECK_DELAY_MAX);
		cancelAll("checkStatus");
	}

	private void checkUserDb(final String userId) {
		// 检测用户基本数据库信息的完整性
		new Thread(new Runnable() {
			@Override
			public void run() {
				FriendDao.getInstance().checkSystemFriend(userId);
				initMsgUnReadTips(userId);
			}
		}).start();
	}

	private BroadcastReceiver mUserLogInOutReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(LoginHelper.ACTION_LOGIN)) {
				User user = MyApplication.getInstance().mLoginUser;
				Intent startIntent = CoreService.getIntent(MainActivity.this, user.getUserId(), user.getPassword(), user.getNickName());
				startService(startIntent);
				// ToastUtil.showNormalToast(MainActivity.this, "开始Xmpp登陆");
				checkUserDb(user.getUserId());

				mTabRadioGroup.clearCheck();
				mTabRadioGroup.check(R.id.main_tab_one);
			} else if (action.equals(LoginHelper.ACTION_LOGOUT)) {
				MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_SIMPLE_TELPHONE;
				mCoreService.logout();

				cancelUserCheckIfExist();
				startActivity(new Intent(MainActivity.this, LoginHistoryActivity.class));
				// mFindRb.setChecked(true);
				removeNeedUserFragment(false);

			} else if (action.equals(LoginHelper.ACTION_CONFLICT)) {
				// 改变用户状态
				MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_TOKEN_CHANGE;
				mCoreService.logout();
				// mFindRb.setChecked(true);
				removeNeedUserFragment(true);
				cancelUserCheckIfExist();
				// 弹出对话框
				startActivity(new Intent(MainActivity.this, UserCheckedActivity.class));

				if (Build.VERSION.SDK_INT == Build.VERSION_CODES.HONEYCOMB) {
					mActivityManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_WITH_HOME);
				} else if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
					mActivityManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_NO_USER_ACTION);
				}
			} else if (action.equals(LoginHelper.ACTION_NEED_UPDATE)) {
				// mFindRb.setChecked(true);
				removeNeedUserFragment(true);
				cancelUserCheckIfExist();
				// 弹出对话框
				startActivity(new Intent(MainActivity.this, UserCheckedActivity.class));
			} else if (action.equals(LoginHelper.ACTION_LOGIN_GIVE_UP)) {
				cancelUserCheckIfExist();
				MyApplication.getInstance().mUserStatus = LoginHelper.STATUS_USER_NO_UPDATE;
				mCoreService.logout();
			}

		}

	};

	/* 当注销当前用户时，将那些需要当前用户的Fragment销毁，以后重新登陆后，重新加载为初始状态 */
	private void removeNeedUserFragment(boolean startAgain) {
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();// 开始事物
		if (mMessageFragment != null) {
			fragmentTransaction.remove(mMessageFragment);
		}
		if (mMyFriendFragment != null) {
			fragmentTransaction.remove(mMyFriendFragment);
		}
		if (mNearbyFragment != null) {
			fragmentTransaction.remove(mNearbyFragment);
		}
		if (mGroupChatFragment != null) {
			fragmentTransaction.remove(mGroupChatFragment);
		}
		if (mMeFragment != null) {
			fragmentTransaction.remove(mMeFragment);
		}
		if(mBusinessCircleFragment!=null){
			fragmentTransaction.remove(mBusinessCircleFragment);
			
		}
		fragmentTransaction.commitAllowingStateLoss();
		mMessageFragment = null;
		mMyFriendFragment = null;
		mNearbyFragment = null;
		mGroupChatFragment = null;
		mMeFragment = null;
		mBusinessCircleFragment=null;
		mLastFragment = null;
		if (startAgain) {
			Intent intent = new Intent(this, MainActivity.class);
			startActivity(intent);
		}
	}

	private ServiceConnection mXmppServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mCoreService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mCoreService = ((CoreServiceBinder) service).getService();
			mImStatus = mCoreService.isAuthenticated() ? AuthStateListener.AUTH_STATE_SUCCESS : AuthStateListener.AUTH_STATE_NOT;
		}
	};

	@Override
	protected void onStop() {
		super.onStop();
		saveOfflineTime();

	}

	private void saveOfflineTime() {
		//将现在的时间存起来,
		long time=System.currentTimeMillis()/1000;
		Log.d("wang", "time_destory::" + time + "");
		PreferenceUtils.putLong(this, Constants.OFFLINE_TIME, time);
		MyApplication.getInstance().mLoginUser.setOfflineTime(time);
		UserDao.getInstance().updateUnLineTime(MyApplication.getInstance().mLoginUser.getUserId(), time);
	}

	@Override
	protected void onDestroy() {
		saveOfflineTime();
		MyApplication.getInstance().unregisterNetWorkObserver(this);
		ListenerManager.getInstance().removeAuthStateChangeListener(this);
		if (mXmppBind) {
			unbindService(mXmppServiceConnection);
		}
		unregisterReceiver(mUpdateUnReadReceiver);
		unregisterReceiver(mUserLogInOutReceiver);
		super.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {

		super.onSaveInstanceState(outState, outPersistentState);
		saveOfflineTime();
	}

	private void restoreState(Bundle savedInstanceState) {
		mLastFragment = (Fragment) getSupportFragmentManager().findFragmentById(R.id.main_content);
		mMessageFragment = (MessageFragment) getSupportFragmentManager().findFragmentByTag(TAG_MESSAGE);
		mMyFriendFragment = (MyFriendFragment) getSupportFragmentManager().findFragmentByTag(TAG_MY);
		mNearbyFragment = (NearbyFragment) getSupportFragmentManager().findFragmentByTag(TAG_NEARBY);
		mGroupChatFragment = (GroupChatFragment) getSupportFragmentManager().findFragmentByTag(TAG_GROUP_CHAT);
		mMeFragment = (MeFragment) getSupportFragmentManager().findFragmentByTag(TAG_ME);
		mBusinessCircleFragment = (BusinessCircleFragment) getSupportFragmentManager().findFragmentByTag(TAG_BusinessCircle);
	}

	private void initView(Bundle savedInstanceState) {
		mTabRadioGroup = (DivideRadioGroup) findViewById(R.id.main_tab_radio_group);
		mTabRadioGroup.setOnCheckedChangeListener(mTabRadioGroupChangeListener);
		if (savedInstanceState == null) {
			mTabRadioGroup.check(R.id.main_tab_one);
		}
		mMsgUnReadTv = (TextView) findViewById(R.id.main_tab_one_tv);
	}

	private DivideRadioGroup.OnCheckedChangeListener mTabRadioGroupChangeListener = new DivideRadioGroup.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(DivideRadioGroup group, int checkedId) {
			if (checkedId == R.id.main_tab_one) {
				if (mMessageFragment == null) {
					mMessageFragment = new MessageFragment();
				}
				changeFragment(mMessageFragment, TAG_MESSAGE);
				updateMessageTitle();
			} else if (checkedId == R.id.main_tab_two) {
				/*if (mMyFriendFragment == null) {
					mMyFriendFragment = new MyFriendFragment();
				}
				changeFragment(mMyFriendFragment, TAG_MY);
				getSupportActionBar().setTitle("通讯录");*/
				if (mBusinessCircleFragment == null) {
					mBusinessCircleFragment = new BusinessCircleFragment();
				}
				changeFragment(mBusinessCircleFragment, TAG_BusinessCircle);
				getSupportActionBar().setTitle("朋友圈");
				
			} 
			else if (checkedId == R.id.main_tab_three) {
				
				if (mNearbyFragment == null) {
					mNearbyFragment = new NearbyFragment();
				}
				changeFragment(mNearbyFragment, TAG_NEARBY);
				getSupportActionBar().setTitle(R.string.nearby);
			} 
			else if (checkedId == R.id.main_tab_four) {
				if (mGroupChatFragment == null) {
					mGroupChatFragment = new GroupChatFragment();
				}
				changeFragment(mGroupChatFragment, TAG_GROUP_CHAT);
				getSupportActionBar().setTitle(R.string.group_chat);
			} 
			else if (checkedId == R.id.main_tab_five) {
				if (mMeFragment == null) {
					mMeFragment = new MeFragment();
				}
				changeFragment(mMeFragment, TAG_ME);
				getSupportActionBar().setTitle(R.string.me);
			}
		}
	};

	private void changeFragment(Fragment addFragment, String tag) {
		FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();// 开始事物
		if (mLastFragment == addFragment) {
			return;
		}
		if (mLastFragment != null && mLastFragment != addFragment) {// 如果最后一次加载的不是现在要加载的Fragment，那么僵最后一次加载的移出
			fragmentTransaction.detach(mLastFragment);
		}
		if (addFragment == null) {
			return;
		}
		if (!addFragment.isAdded())// 如果还没有添加，就加上
			fragmentTransaction.add(R.id.main_content, addFragment, tag);
		if (addFragment.isDetached())
			fragmentTransaction.attach(addFragment);
		mLastFragment = addFragment;
		fragmentTransaction.commitAllowingStateLoss();
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			intent.addCategory(Intent.CATEGORY_HOME);

			startActivity(intent);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private int mImStatus = AuthStateListener.AUTH_STATE_NOT;

	@Override
	public void onAuthStateChange(int authState) {
		mImStatus = authState;
		if (mTabRadioGroup.getCheckedRadioButtonId() == R.id.main_tab_one) {
			updateMessageTitle();
		}
	}
/**
 * 更改消息在线不在线的状态
 * msg_online>消息(在线)  msg_offline>消息(离线)  msg_connect">消息(连接中)
 */
	private void updateMessageTitle() {
		if (mImStatus == AuthStateListener.AUTH_STATE_NOT) {
			getSupportActionBar().setTitle(R.string.msg_offline);
		} else if (mImStatus == AuthStateListener.AUTH_STATE_ING) {
			getSupportActionBar().setTitle(R.string.msg_connect);
		} else if (mImStatus == AuthStateListener.AUTH_STATE_SUCCESS) {
			getSupportActionBar().setTitle(R.string.msg_online);
		}
	}

	@Override
	public void onNetWorkStatusChange(boolean connected) {
		// 当网络状态改变时，判断当前用户的状态，是否需要更新
		if (connected) {
			if (!MyApplication.getInstance().mUserStatusChecked) {
				mRetryCheckDelay = 0;
				mUserCheckHander.sendEmptyMessageDelayed(MSG_USER_CHECK, mRetryCheckDelay);
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		isPause = false;
		if (mMsgNumNeedUpdate) {
			initMsgUnReadTips(MyApplication.getInstance().mLoginUser.getUserId());
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		isPause = true;
	}

	/*********************** 未读数量的更新功能 *****************/
	private Handler mUnReadHandler = new Handler();
	private TextView mMsgUnReadTv;
	private int mMsgUnReadNum = 0;
	private boolean mMsgNumNeedUpdate = false;

	private void initMsgUnReadTips(String userId) {// 初始化未读条数
		// 消息未读条数累加
		mMsgUnReadNum = FriendDao.getInstance().getMsgUnReadNumTotal(userId);
		mUnReadHandler.post(new Runnable() {
			@Override
			public void run() {
				updateMsgUnReadTv();
			}
		});
	}

	private BroadcastReceiver mUpdateUnReadReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(MsgBroadcast.ACTION_MSG_NUM_UPDATE)) {
				int operation = intent.getIntExtra(MsgBroadcast.EXTRA_NUM_OPERATION, MsgBroadcast.NUM_ADD);
				int count = intent.getIntExtra(MsgBroadcast.EXTRA_NUM_COUNT, 0);
				mMsgUnReadNum = (operation == MsgBroadcast.NUM_ADD) ? mMsgUnReadNum + count : mMsgUnReadNum - count;
				updateMsgUnReadTv();
			} else if (action.equals(MsgBroadcast.ACTION_MSG_NUM_RESET)) {
				if (isPause) {// 等待恢复的时候更新
					mMsgNumNeedUpdate = true;
				} else {// 立即更新
					initMsgUnReadTips(MyApplication.getInstance().mLoginUser.getUserId());
				}
			}
		}
	};

	private void updateMsgUnReadTv() {
		if (mMsgUnReadNum > 0) {
			mMsgUnReadTv.setVisibility(View.VISIBLE);
			String numStr = mMsgUnReadNum >= 99 ? "99+" : mMsgUnReadNum + "";
			mMsgUnReadTv.setText(numStr);
		} else {
			mMsgUnReadTv.setVisibility(View.INVISIBLE);
		}
	}

	public void exitMucChat(String toUserId) {
		if (mCoreService != null) {
			mCoreService.exitMucChat(toUserId);
		}
	}
	
	public void sendNewFriendMessage(String toUserId, NewFriendMessage message) {
		if (mBind && mXmppService != null) {
			mXmppService.sendNewFriendMessage(toUserId, message);
		}
	}
	
	private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(CardcastUiUpdateUtil.ACTION_UPDATE_UI)) {
				if (mAttention != null) {
					mAttention.update();
				}
				if (mFriend != null) {
					mFriend.update();
				}
				if (mRoom != null) {
					mRoom.update();
				}
			}
		}
	};
	
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mXmppService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mXmppService = ((CoreServiceBinder) service).getService();
		}
	};
	/**
	 * 获得fragment对象
	 * @return
	 */
	public BusinessCircleFragment getBusinessCircleFragment(){
		FragmentManager sfmanager = getSupportFragmentManager();
		return (BusinessCircleFragment)sfmanager.findFragmentByTag(TAG_BusinessCircle);
	}



}
