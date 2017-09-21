package com.sk.weichat.ui.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshBase.OnRefreshListener;
import com.roamer.slidelistview.SlideBaseAdapter;
import com.roamer.slidelistview.SlideListView;
import com.roamer.slidelistview.SlideListView.SlideMode;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.broadcast.MsgBroadcast;
import com.sk.weichat.broadcast.MucgroupUpdateUtil;
import com.sk.weichat.db.dao.ChatMessageDao;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.sortlist.BaseSortModel;
import com.sk.weichat.sortlist.PingYinUtil;
import com.sk.weichat.ui.MainActivity;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.base.EasyFragment;
import com.sk.weichat.util.HtmlUtils;
import com.sk.weichat.util.ProgressDialogUtil;
import com.sk.weichat.util.StringUtils;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.util.ViewHolder;
import com.sk.weichat.view.ClearEditText;
import com.sk.weichat.view.PullToRefreshSlideListView;
import com.sk.weichat.volley.ObjectResult;
import com.sk.weichat.volley.Result;
import com.sk.weichat.volley.StringJsonObjectRequest;

public class MessageFragment extends EasyFragment {
	private boolean mNeedUpdate = true;

	private ClearEditText mClearEditText;
	private PullToRefreshSlideListView mPullToRefreshListView;
	private List<BaseSortModel<Friend>> mFriendList;// 筛选后的朋友数据
	private List<BaseSortModel<Friend>> mOriginalFriendList;// 原始的朋友数据，也就是从数据库查询出来，没有筛选的
	private NearlyMessageAdapter mAdapter;
	private Handler mHandler = new Handler();

	public MessageFragment() {
		mOriginalFriendList = new ArrayList<>();
		mFriendList = new ArrayList<>();
	}

	@Override
	protected int inflateLayoutId() {
		return R.layout.fragment_message;
	}

	@Override
	protected void onCreateView(Bundle savedInstanceState, boolean createView) {
		if (createView) {
			initView();
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getActivity().registerReceiver(mUpdateReceiver, new IntentFilter(MsgBroadcast.ACTION_MSG_UI_UPDATE));
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		getActivity().unregisterReceiver(mUpdateReceiver);
	}

	private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(MsgBroadcast.ACTION_MSG_UI_UPDATE)) {
				if (isResumed()) {
					loadData();
				} else {
					mNeedUpdate = true;
				}
			}
		}
	};

	@Override
	public void onResume() {
		super.onResume();
		if (mNeedUpdate) {
			mNeedUpdate = false;
			loadData();
		}
	}

	private void initView() {
		mClearEditText = (ClearEditText) findViewById(R.id.search_edit);
		mClearEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				String filter = mClearEditText.getText().toString().trim().toUpperCase();
				mFriendList.clear();
				if (mOriginalFriendList != null && mOriginalFriendList.size() > 0) {
					for (int i = 0; i < mOriginalFriendList.size(); i++) {
						BaseSortModel<Friend> mode = mOriginalFriendList.get(i);
						// 获取筛选的数据
						if (TextUtils.isEmpty(filter) || mode.getSimpleSpell().startsWith(filter) || mode.getWholeSpell().startsWith(filter)
								|| mode.getBean().getShowName().startsWith(filter)) {
							mFriendList.add(mode);
						}

					}
				}
				mAdapter.notifyDataSetChanged();
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		mPullToRefreshListView = (PullToRefreshSlideListView) findViewById(R.id.pull_refresh_list);
		mPullToRefreshListView.setShowIndicator(false);
		mPullToRefreshListView.setMode(Mode.PULL_FROM_START);

		mAdapter = new NearlyMessageAdapter(getActivity());

		mPullToRefreshListView.getRefreshableView().setAdapter(mAdapter);

		mPullToRefreshListView.setOnRefreshListener(new OnRefreshListener<SlideListView>() {
			@Override
			public void onRefresh(PullToRefreshBase<SlideListView> refreshView) {
				loadData();
			}
		});

		mPullToRefreshListView.getRefreshableView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				Friend friend = (Friend) arg0.getItemAtPosition(position);
				if (friend.getRoomFlag() == 0) {
					if (friend.getUserId().equals(Friend.ID_NEW_FRIEND_MESSAGE)) {// 新朋友消息
						startActivity(new Intent(getActivity(), NewFriendActivity.class));
					} else {
						Intent intent = new Intent(getActivity(), ChatActivity.class);
						intent.putExtra(ChatActivity.FRIEND, friend);
						startActivity(intent);
					}
				} else {
					Intent intent = new Intent(getActivity(), MucChatActivity.class);
					intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
					intent.putExtra(AppConstant.EXTRA_NICK_NAME, friend.getNickName());
					intent.putExtra(AppConstant.EXTRA_IS_GROUP_CHAT, true);
					startActivity(intent);
				}

				if (friend.getUnReadNum() > 0) {
					MsgBroadcast.broadcastMsgNumUpdate(getActivity(), false, friend.getUnReadNum());
					friend.setUnReadNum(0);
					mAdapter.notifyDataSetChanged();
				}
			}
		});
	}

	private BaseActivity mActivity;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mActivity = (BaseActivity) getActivity();
	}

	/**
	 * 请求加载新的筛选条件的数据
	 * 
	 * @param isPullDwonToRefersh
	 *            是下拉刷新，还是上拉加载
	 */
	private void loadData() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				String mLoginUserId = MyApplication.getInstance().mLoginUser.getUserId();
				long startTime = System.currentTimeMillis();
				final List<Friend> friends = FriendDao.getInstance().getNearlyFriendMsg(mLoginUserId);
				long delayTime = 200 - (startTime - System.currentTimeMillis());// 保证至少200ms的刷新过程
				if (delayTime < 0) {
					delayTime = 0;
				}
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						mOriginalFriendList.clear();
						mFriendList.clear();
						String filter = mClearEditText.getText().toString().trim().toUpperCase();
						if (friends != null && friends.size() > 0) {
							for (int i = 0; i < friends.size(); i++) {
								BaseSortModel<Friend> mode = new BaseSortModel<Friend>();
								mode.setBean(friends.get(i));
								setSortCondition(mode);
								mOriginalFriendList.add(mode);
								// 获取筛选的数据
								if (TextUtils.isEmpty(filter) || mode.getSimpleSpell().startsWith(filter) || mode.getWholeSpell().startsWith(filter)
										|| mode.getBean().getShowName().startsWith(filter)) {
									mFriendList.add(mode);
								}

							}
						}
						mAdapter.notifyDataSetChanged();
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
			mode.setWholeSpell(wholeSpell);
			mode.setFirstLetter(firstLetter);
			mode.setSimpleSpell(PingYinUtil.converterToFirstSpell(name));
		} else {// 如果全拼为空，理论上是一种错误情况，因为这代表着昵称为空
			mode.setWholeSpell("#");
			mode.setFirstLetter("#");
			mode.setSimpleSpell("#");
		}
	}

	public class NearlyMessageAdapter extends SlideBaseAdapter {

		public NearlyMessageAdapter(Context context) {
			super(context);
		}

		@Override
		public int getCount() {
			return mFriendList.size();
		}

		@Override
		public Object getItem(int position) {
			return mFriendList.get(position).getBean();
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = createConvertView(position);
			}
			ImageView avatar_img = ViewHolder.get(convertView, R.id.avatar_img);
			TextView num_tv = ViewHolder.get(convertView, R.id.num_tv);
			TextView nick_name_tv = ViewHolder.get(convertView, R.id.nick_name_tv);
			TextView content_tv = ViewHolder.get(convertView, R.id.content_tv);
			TextView time_tv = ViewHolder.get(convertView, R.id.time_tv);

			TextView delete_tv = ViewHolder.get(convertView, R.id.delete_tv);

			final Friend friend = mFriendList.get(position).getBean();

			if (friend.getRoomFlag() == 0) {// 这是单个人
				if (friend.getUserId().equals(Friend.ID_SYSTEM_MESSAGE)) {// 系统消息的头像
					avatar_img.setImageResource(R.drawable.im_notice);
				} else if (friend.getUserId().equals(Friend.ID_NEW_FRIEND_MESSAGE)) {// 新朋友的头像
					avatar_img.setImageResource(R.drawable.im_new_friends);
				} else {// 其他
					AvatarHelper.getInstance().displayAvatar(friend.getUserId(), avatar_img, true);
				}
			} else {// 这是1个房间
				if (TextUtils.isEmpty(friend.getRoomCreateUserId())) {
//					avatar_img.setImageResource(R.drawable.avatar_normal);
					avatar_img.setImageResource(R.drawable.muc_icons);
				} else {
//					AvatarHelper.getInstance().displayAvatar(friend.getRoomCreateUserId(), avatar_img, true);// 目前在备注名放房间的创建者Id
					avatar_img.setImageResource(R.drawable.muc_icons);
				}
			}

			nick_name_tv.setText(friend.getShowName());
			time_tv.setText(TimeUtils.getFriendlyTimeDesc(getActivity(), friend.getTimeSend()));

			CharSequence content = "";
			if (friend.getType() == XmppMessage.TYPE_TEXT) {
				String s = StringUtils.replaceSpecialChar(friend.getContent());
				content = HtmlUtils.transform200SpanString(s.replaceAll("\n", "\r\n"), true);
			} else {
				content = friend.getContent();
			}
			content_tv.setText(content);

			if (friend.getUnReadNum() > 0) {
				String numStr = friend.getUnReadNum() >= 99 ? "99+" : friend.getUnReadNum() + "";
				num_tv.setText(numStr);
				num_tv.setVisibility(View.VISIBLE);
			} else {
				num_tv.setVisibility(View.GONE);
			}

			delete_tv.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					String mLoginUserId = MyApplication.getInstance().mLoginUser.getUserId();
					if (friend.getRoomFlag() == 0) {
						if (friend.getUnReadNum() > 0) {
							MsgBroadcast.broadcastMsgNumUpdate(getActivity(), false, friend.getUnReadNum());
						}
						BaseSortModel<Friend> mode = mFriendList.get(position);

						mFriendList.remove(mode);
						mOriginalFriendList.remove(mode);

						mAdapter.notifyDataSetChanged();
						// 如果是普通的人，从好友表中删除最后一条消息的记录，这样就不会查出来了
						FriendDao.getInstance().resetFriendMessage(mLoginUserId, friend.getUserId());

						// 消息表中删除
						ChatMessageDao.getInstance().deleteMessageTable(mLoginUserId, friend.getUserId());
					} else {
						deleteRoom(mLoginUserId, mFriendList.get(position));
					}

				}
			});
			return convertView;
		}

		@Override
		public SlideMode getSlideModeInPosition(int position) {
			Friend friend = mFriendList.get(position).getBean();
			if (friend != null && (friend.getUserId().equals(Friend.ID_SYSTEM_MESSAGE) || friend.getUserId().equals(Friend.ID_NEW_FRIEND_MESSAGE))) {
				return SlideMode.NONE;
			}
			return super.getSlideModeInPosition(position);
		}

		@Override
		public int getFrontViewId(int position) {
			return R.layout.row_nearly_message;
		}

		@Override
		public int getLeftBackViewId(int position) {
			return 0;
		}

		@Override
		public int getRightBackViewId(int position) {
			return R.layout.row_item_delete;
		}
	}

	/**
	 * 删除房间
	 * 
	 * @param sortFriend
	 */
	private void deleteRoom(final String loginUserId, final BaseSortModel<Friend> sortFriend) {
		BaseActivity activity = (BaseActivity) getActivity();
		boolean deleteRoom = false;
		if (loginUserId.equals(sortFriend.getBean().getRoomCreateUserId())) {
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
			params.put("userId", loginUserId);
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
					deleteFriend(loginUserId, sortFriend);
					sendBroadcast();
				}
				ProgressDialogUtil.dismiss(dialog);
			}
		}, Void.class, params);
		activity.addDefaultRequest(request);
	}

	private void deleteFriend(final String loginUserId, final BaseSortModel<Friend> sortFriend) {
		Friend friend = sortFriend.getBean();
		if (friend.getUnReadNum() > 0) {
			MsgBroadcast.broadcastMsgNumUpdate(getActivity(), false, friend.getUnReadNum());
		}
		mFriendList.remove(sortFriend);
		mOriginalFriendList.remove(sortFriend);
		mAdapter.notifyDataSetChanged();

		// 删除这个房间
		FriendDao.getInstance().deleteFriend(loginUserId, friend.getUserId());
		// 消息表中删除
		ChatMessageDao.getInstance().deleteMessageTable(loginUserId, friend.getUserId());

		MainActivity activity = (MainActivity) getActivity();
		activity.exitMucChat(friend.getUserId());

	}
	
	public void sendBroadcast(){
		Intent mIntent = new Intent(MucgroupUpdateUtil.ACTION_UPDATE);
		getActivity().sendBroadcast(mIntent);
	};

}
