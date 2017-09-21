package com.sk.weichat.ui.cardcast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.adapter.FriendSortAdapter;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.broadcast.MsgBroadcast;
import com.sk.weichat.db.dao.ChatMessageDao;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.sortlist.BaseComparator;
import com.sk.weichat.sortlist.BaseSortModel;
import com.sk.weichat.sortlist.PingYinUtil;
import com.sk.weichat.sortlist.SideBar;
import com.sk.weichat.ui.MainActivity;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.base.EasyFragment;
import com.sk.weichat.ui.message.MucChatActivity;
import com.sk.weichat.util.ProgressDialogUtil;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.volley.ObjectResult;
import com.sk.weichat.volley.Result;
import com.sk.weichat.volley.StringJsonObjectRequest;

public class RoomFragment extends EasyFragment {

	private PullToRefreshListView mPullToRefreshListView;
	private TextView mTextDialog;
	private SideBar mSideBar;
	private List<BaseSortModel<Friend>> mSortFriends;
	private BaseComparator<Friend> mBaseComparator;
	private FriendSortAdapter mAdapter;
	private String mLoginUserId;
	private Handler mHandler = new Handler();

	public RoomFragment() {
		mSortFriends = new ArrayList<BaseSortModel<Friend>>();
		mBaseComparator = new BaseComparator<Friend>();
		mLoginUserId = MyApplication.getInstance().mLoginUser.getUserId();
	}

	@Override
	protected int inflateLayoutId() {
		return R.layout.fragment_friend;
	}

	@Override
	protected void onCreateView(Bundle savedInstanceState, boolean createView) {
		if (createView) {
			initView();
		}
	}

	private void initView() {
		mPullToRefreshListView = (PullToRefreshListView) findViewById(R.id.pull_refresh_list);
		mTextDialog = (TextView) findViewById(R.id.text_dialog);
		mSideBar = (SideBar) findViewById(R.id.sidebar);
		mSideBar.setTextView(mTextDialog);

		mSideBar.setOnTouchingLetterChangedListener(new SideBar.OnTouchingLetterChangedListener() {
			@Override
			public void onTouchingLetterChanged(String s) {
				// 该字母首次出现的位置
				int position = mAdapter.getPositionForSection(s.charAt(0));
				if (position != -1) {
					mPullToRefreshListView.getRefreshableView().setSelection(position);
				}
			}
		});

		mAdapter = new FriendSortAdapter(getActivity(), mSortFriends);

		mPullToRefreshListView.setMode(Mode.PULL_FROM_START);
		mPullToRefreshListView.getRefreshableView().setAdapter(mAdapter);

		mPullToRefreshListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
			@Override
			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
				loadData();
			}
		});

		mPullToRefreshListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

				// Intent intent = new Intent(getActivity(), PersonalInfoActivity.class);
				// intent.putExtra(Constant.EXTRA_USER_ID, friend.getUserId());
				// startActivity(intent);

				Friend friend = mSortFriends.get((int) id).getBean();
				if (friend.getRoomFlag() == 0) {
					// 不会出现此情况，因为必定不是一个房间
					;
				} else {
					Intent intent = new Intent(getActivity(), MucChatActivity.class);
					intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
					intent.putExtra(AppConstant.EXTRA_NICK_NAME, friend.getNickName());
					intent.putExtra(AppConstant.EXTRA_IS_GROUP_CHAT, true);
					startActivity(intent);
				}

				MsgBroadcast.broadcastMsgNumReset(getActivity());
				MsgBroadcast.broadcastMsgUiUpdate(getActivity());

			}
		});
		mPullToRefreshListView.getRefreshableView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				BaseSortModel<Friend> sortFriend = mSortFriends.get((int) id);
				if (sortFriend == null || sortFriend.getBean() == null) {
					return false;
				}
				showLongClickOperationDialog(sortFriend);
				return true;
			}
		});

	}

	public void update() {
		if (isResumed()) {
			loadData();
		} else {
			mNeedUpdate = true;
		}
	}

	private boolean mNeedUpdate = true;

	@Override
	public void onResume() {
		super.onResume();
		if (mNeedUpdate) {
			loadData();
			mNeedUpdate = false;
		}
	}

	private void loadData() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				long startTime = System.currentTimeMillis();
				final List<Friend> friends = FriendDao.getInstance().getAllRooms(mLoginUserId);

				long delayTime = 200 - (startTime - System.currentTimeMillis());// 保证至少500ms的刷新过程
				if (delayTime < 0) {
					delayTime = 0;
				}

				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						mSortFriends.clear();
						mSideBar.clearExist();
						if (friends != null && friends.size() > 0) {
							for (int i = 0; i < friends.size(); i++) {
								BaseSortModel<Friend> mode = new BaseSortModel<Friend>();
								mode.setBean(friends.get(i));
								setSortCondition(mode);
								mSortFriends.add(mode);
							}
							Collections.sort(mSortFriends, mBaseComparator);
						}
						mAdapter.notifyDataSetInvalidated();
						mPullToRefreshListView.onRefreshComplete();
					}
				}, delayTime);
			}
		}).start();
	}

	private final void setSortCondition(BaseSortModel<Friend> mode) {
		Friend friend = mode.getBean();
		if (friend == null) {
			return;
		}
		String name = friend.getShowName();
		String wholeSpell = PingYinUtil.getPingYin(name);
		if (!TextUtils.isEmpty(wholeSpell)) {
			String firstLetter = Character.toString(wholeSpell.charAt(0));
			mSideBar.addExist(firstLetter);
			mode.setWholeSpell(wholeSpell);
			mode.setFirstLetter(firstLetter);
			mode.setSimpleSpell(PingYinUtil.converterToFirstSpell(name));
		} else {// 如果全拼为空，理论上是一种错误情况，因为这代表着昵称为空
			mode.setWholeSpell("#");
			mode.setFirstLetter("#");
			mode.setSimpleSpell("#");
		}
	}

	// ///////////其他操作///////////////////
	private void showLongClickOperationDialog(final BaseSortModel<Friend> sortFriend) {
		Friend friend = sortFriend.getBean();
		if (friend.getStatus() != Friend.STATUS_BLACKLIST && friend.getStatus() == Friend.STATUS_ATTENTION
				&& friend.getStatus() == Friend.STATUS_FRIEND) {
			return;
		}
		CharSequence[] items = new CharSequence[1];
		// items[0] = getString(R.string.set_remark_name);// 设置备注名
		// if (friend.getStatus() == Friend.STATUS_BLACKLIST) {// 在黑名单中,显示“设置备注名”、“移除黑名单”,"取消关注"，“彻底删除”
		// items[1] = getString(R.string.remove_blacklist);
		// } else {
		// items[1] = getString(R.string.add_blacklist);
		// }
		// items[2] = getString(R.string.cancel_attention);
		items[0] = getString(R.string.delete_all);

		new AlertDialog.Builder(getActivity()).setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case 0:// 彻底删除
					deleteRoom(sortFriend);
					break;
				}
				// switch (which) {
				// case 0:// 设置备注名
				// showRemarkDialog(sortFriend);
				// break;
				// case 1:// 加入黑名单，或者移除黑名单
				// showBlacklistDialog(sortFriend);
				// break;
				// case 2:// 取消关注
				// showCancelAttentionDialog(sortFriend);
				// break;
				// case 3:// 解除关注关系或者解除好友关系
				// showDeleteAllDialog(sortFriend);
				// break;
				// }
			}
		}).setCancelable(true).create().show();
	}

	private void deleteRoom(final BaseSortModel<Friend> sortFriend) {
		CardcastActivity activity = (CardcastActivity) getActivity();

		boolean deleteRoom = false;
		if (mLoginUserId.equals(sortFriend.getBean().getRoomCreateUserId())) {
			deleteRoom = true;
		}
		String url = null;
		if (deleteRoom) {
			url = activity.mConfig.ROOM_DELETE;
		} else {
			url = activity.mConfig.ROOM_MEMBER_DELETE;
		}
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("roomId", sortFriend.getBean().getRoomId());
		if (!deleteRoom) {
			params.put("userId", mLoginUserId);
		}

		final ProgressDialog dialog = ProgressDialogUtil.init(getActivity(), null, getString(R.string.please_wait));
		ProgressDialogUtil.show(dialog);
		StringJsonObjectRequest<Void> request = new StringJsonObjectRequest<Void>(url, new ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
				ProgressDialogUtil.dismiss(dialog);
				ToastUtil.showErrorNet(getActivity());
			}
		}, new StringJsonObjectRequest.Listener<Void>() {
			@Override
			public void onResponse(ObjectResult<Void> result) {
				boolean success = Result.defaultParser(getActivity(), result, true);
				if (success) {
					deleteFriend(sortFriend);
				}
				ProgressDialogUtil.dismiss(dialog);
			}
		}, Void.class, params);
		activity.addDefaultRequest(request);
	}

	private void deleteFriend(final BaseSortModel<Friend> sortFriend) {
		mSortFriends.remove(sortFriend);
		String firstLetter = sortFriend.getFirstLetter();
		mSideBar.removeExist(firstLetter);// 移除之前设置的首字母
		mAdapter.notifyDataSetChanged();

		Friend friend = sortFriend.getBean();
		// 删除这个房间
		FriendDao.getInstance().deleteFriend(mLoginUserId, friend.getUserId());
		// 消息表中删除
		ChatMessageDao.getInstance().deleteMessageTable(mLoginUserId, friend.getUserId());

		// 更新消息界面
		MsgBroadcast.broadcastMsgNumReset(getActivity());
		MsgBroadcast.broadcastMsgUiUpdate(getActivity());

		CardcastActivity activity = (CardcastActivity) getActivity();
		activity.exitMucChat(friend.getUserId());
	}

}
