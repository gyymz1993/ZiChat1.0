package com.sk.weichat.ui.cardcast;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;

import com.sk.weichat.R;
import com.sk.weichat.bean.message.NewFriendMessage;
import com.sk.weichat.broadcast.CardcastUiUpdateUtil;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.xmpp.CoreService;
import com.sk.weichat.xmpp.CoreService.CoreServiceBinder;

/**
 * 我的名片盒
 * 
 * 
 */
@SuppressWarnings("deprecation")
public class CardcastActivity extends BaseActivity {

	private TabListener<FriendFragment> mFriend;
	private TabListener<AttentionFragment> mAttention;
	private TabListener<RoomFragment> mRoom;

	private boolean mBind;
	private CoreService mXmppService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setup action bar for tabs
		ActionBar actionBar = getSupportActionBar();
		actionBar.setTitle(R.string.my_friend);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		mFriend = new TabListener<FriendFragment>(this, "friend", FriendFragment.class);
		mAttention = new TabListener<AttentionFragment>(this, "attention", AttentionFragment.class);
		mRoom = new TabListener<RoomFragment>(this, "room", RoomFragment.class);
		int index = 0;
		if (savedInstanceState != null) {
			mFriend.setFragment(getSupportFragmentManager().findFragmentByTag("friend"));
			mAttention.setFragment(getSupportFragmentManager().findFragmentByTag("attention"));
			mRoom.setFragment(getSupportFragmentManager().findFragmentByTag("room"));
			index = savedInstanceState.getInt("index", 0);
		}
		Tab tab = actionBar.newTab().setText(R.string.focus_on_each_other).setTabListener(mFriend);
		actionBar.addTab(tab);
		tab = actionBar.newTab().setText(R.string.unilateral_attention).setTabListener(mAttention);
		actionBar.addTab(tab);
		tab = actionBar.newTab().setText(R.string.focus_room).setTabListener(mRoom);
		actionBar.addTab(tab);
		actionBar.setSelectedNavigationItem(index);

		registerReceiver(mUpdateReceiver, CardcastUiUpdateUtil.getUpdateActionFilter());
		mBind = bindService(CoreService.getIntent(), mServiceConnection, BIND_AUTO_CREATE);
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("index", getSupportActionBar().getSelectedNavigationIndex());
	}

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

	void exitMucChat(String toUserId) {
		if (mXmppService != null) {
			mXmppService.exitMucChat(toUserId);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		unregisterReceiver(mUpdateReceiver);
		if (mBind) {
			unbindService(mServiceConnection);
		}
	}

	void sendNewFriendMessage(String toUserId, NewFriendMessage message) {
		if (mBind && mXmppService != null) {
			mXmppService.sendNewFriendMessage(toUserId, message);
		}
	}

	private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(CardcastUiUpdateUtil.ACTION_UPDATE_UI)) {
				if (mAttention.getFragment() != null) {
					mAttention.getFragment().update();
				}
				if (mFriend.getFragment() != null) {
					mFriend.getFragment().update();
				}
				if (mRoom.getFragment() != null) {
					mRoom.getFragment().update();
				}
			}
		}
	};

	public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
		private Fragment mFragment;
		private final Activity mActivity;
		private final String mTag;
		private final Class<T> mClass;

		public TabListener(Activity activity, String tag, Class<T> clz) {
			mActivity = activity;
			mTag = tag;
			mClass = clz;
		}

		public void setFragment(Fragment fragment) {
			mFragment = fragment;
		}

		@SuppressWarnings("unchecked")
		public T getFragment() {
			return (T) mFragment;
		}

		public void onTabSelected(Tab tab, FragmentTransaction ft) {
			// Check if the fragment is already initialized
			if (mFragment == null) {
				// If not, instantiate and add it to the activity
				mFragment = Fragment.instantiate(mActivity, mClass.getName());
				ft.add(android.R.id.content, mFragment, mTag);
			} else {
				// If it exists, simply attach it in order to show it
				ft.attach(mFragment);
			}
		}

		public void onTabUnselected(Tab tab, FragmentTransaction ft) {
			if (mFragment != null) {
				// Detach the fragment, because another one is being attached
				ft.detach(mFragment);
			}
		}

		public void onTabReselected(Tab tab, FragmentTransaction ft) {
			// User selected the already selected tab. Usually do nothing.
		}
	}

}
