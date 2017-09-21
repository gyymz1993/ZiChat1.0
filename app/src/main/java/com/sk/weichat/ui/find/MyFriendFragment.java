package com.sk.weichat.ui.find;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.RadioGroup;

import com.sk.weichat.R;
import com.sk.weichat.ui.base.EasyFragment;
import com.sk.weichat.ui.cardcast.AttentionFragment;
import com.sk.weichat.ui.cardcast.FriendFragment;
import com.sk.weichat.ui.cardcast.RoomFragment;
import com.sk.weichat.ui.circle.BusinessCircleActivity;

public class MyFriendFragment extends EasyFragment{

	private static final String TAG_NEWEST = "newest";
	private static final String TAG_HOTEST = "hotest";
	private static final String TAG_NEAREST = "nearest";
	private RadioGroup mRadioGroup;

	private FriendFragment mNewestFragment;// 互相关注
	private AttentionFragment mHotFragment;// 单向关注
	private RoomFragment mNearestFragment;// 关注房间

	private Fragment mLastFragment;//当前显示的fragment

	public MyFriendFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.menu_find, menu);
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.title_bisniss) {
			startActivity(new Intent(getActivity(), BusinessCircleActivity.class));
		}
		return super.onOptionsItemSelected(item);
	}

	private void restoreState(Bundle savedInstanceState) {
		mLastFragment = (Fragment) getChildFragmentManager().findFragmentById(R.id.find_content);
		mNewestFragment = (FriendFragment) getChildFragmentManager().findFragmentByTag(TAG_NEWEST);
		mHotFragment = (AttentionFragment) getChildFragmentManager().findFragmentByTag(TAG_HOTEST);
		mNearestFragment = (RoomFragment) getChildFragmentManager().findFragmentByTag(TAG_NEAREST);
	}

	@Override
	protected int inflateLayoutId() {
		return R.layout.fragment_find;
	}

	@Override
	protected void onCreateView(Bundle savedInstanceState, boolean createView) {
		if (createView) {
			initView(savedInstanceState);
		}
	}

	private void initView(Bundle savedInstanceState) {
		mRadioGroup = (RadioGroup) findViewById(R.id.radio_group);
		mRadioGroup.setOnCheckedChangeListener(mRadioGroupChangeListener);
		if (savedInstanceState == null) {
			mRadioGroup.check(R.id.newest_rb);
		}
	}

	private RadioGroup.OnCheckedChangeListener mRadioGroupChangeListener = new RadioGroup.OnCheckedChangeListener() {
		@Override
		public void onCheckedChanged(RadioGroup group, int checkedId) {
			switch (checkedId) {
			case R.id.newest_rb:
				if (mNewestFragment == null) {
					mNewestFragment = new FriendFragment();
				}
				changeFragment(mNewestFragment, TAG_NEWEST);
				break;
			case R.id.hot_rb:
				if (mHotFragment == null) {
					mHotFragment = new AttentionFragment();
				}
				changeFragment(mHotFragment, TAG_HOTEST);
				break;
			case R.id.nearest_rb:
				if (mNearestFragment == null) {
					mNearestFragment = new RoomFragment();
				}
				changeFragment(mNearestFragment, TAG_NEAREST);
				break;
			}
		}
	};

	private void changeFragment(Fragment addFragment, String tag) {
		FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();// 开始事物
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
			fragmentTransaction.add(R.id.find_content, addFragment, tag);
		if (addFragment.isDetached())
			fragmentTransaction.attach(addFragment);
		mLastFragment = addFragment;
		fragmentTransaction.commitAllowingStateLoss();
	}
	

}
