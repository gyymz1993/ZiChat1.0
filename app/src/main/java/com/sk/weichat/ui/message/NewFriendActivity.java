package com.sk.weichat.ui.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.adapter.NewFriendAdapter;
import com.sk.weichat.bean.AddAttentionResult;
import com.sk.weichat.bean.AttentionUser;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.message.NewFriendMessage;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.db.dao.NewFriendDao;
import com.sk.weichat.helper.FriendHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.circle.BasicInfoActivity;
import com.sk.weichat.util.ProgressDialogUtil;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.volley.ObjectResult;
import com.sk.weichat.volley.Result;
import com.sk.weichat.volley.StringJsonObjectRequest;
import com.sk.weichat.xmpp.CoreService;
import com.sk.weichat.xmpp.CoreService.CoreServiceBinder;
import com.sk.weichat.xmpp.ListenerManager;
import com.sk.weichat.xmpp.listener.NewFriendListener;

public class NewFriendActivity extends BaseActivity implements NewFriendListener {

	private PullToRefreshListView mPullToRefreshListView;
	private NewFriendAdapter mAdapter;
	private List<NewFriendMessage> mNewFriends;
	private ProgressDialog mProgressDialog;
	private String mLoginUserId;
	private boolean mBind;
	private CoreService mXmppService;
	private Handler mHandler = new Handler();

	@Override
	protected void onCreate(android.os.Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.layout_pullrefresh_list);
		mNewFriends = new ArrayList<NewFriendMessage>();
		mProgressDialog = ProgressDialogUtil.init(this, null, getString(R.string.please_wait));
		mLoginUserId = MyApplication.getInstance().mLoginUser.getUserId();
		initView();
		ListenerManager.getInstance().addNewFriendListener(this);
		mBind = bindService(CoreService.getIntent(), mServiceConnection, BIND_AUTO_CREATE);
		// 是所有消息都变为已读
		FriendDao.getInstance().markUserMessageRead(mLoginUserId, Friend.ID_NEW_FRIEND_MESSAGE);
		NewFriendDao.getInstance().markNewFriendRead(mLoginUserId);
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

	protected void onDestroy() {
		super.onDestroy();
		ListenerManager.getInstance().removeNewFriendListener(this);
		if (mBind) {
			unbindService(mServiceConnection);
		}
	}

	private void initView() {
		getSupportActionBar().setTitle(R.string.new_friend);
		mPullToRefreshListView = (PullToRefreshListView) findViewById(R.id.pull_refresh_list);
		mPullToRefreshListView.setMode(Mode.PULL_FROM_START);
		mAdapter = new NewFriendAdapter(this, mNewFriends, mNewFriendActionListener);
		mPullToRefreshListView.getRefreshableView().setAdapter(mAdapter);

		mPullToRefreshListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
			@Override
			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
				loadData();
			}
		});

		mPullToRefreshListView.getRefreshableView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				NewFriendMessage newFriend = mNewFriends.get((int) id);
				Intent intent = new Intent(NewFriendActivity.this, BasicInfoActivity.class);
				intent.putExtra(AppConstant.EXTRA_USER_ID, newFriend.getUserId());
				startActivity(intent);
			}
		});
	}

	@Override
	protected void onResume() {
		super.onResume();
		loadData();
	}

	private NewFriendAdapter.NewFriendActionListener mNewFriendActionListener = new NewFriendAdapter.NewFriendActionListener() {
		@Override
		public void sayHellow(int position) {
			doFeedbackOrSayHello(position, 0);
		}

		@Override
		public void feedback(int position) {

		}

		@Override
		public void agree(int position) {
			doAgreeOrAttention(position, 1);
		}

		@Override
		public void addAttention(int position) {
			doAgreeOrAttention(position, 0);
		}

		@Override
		public void removeBalckList(int position) {
			removeBlacklist(position);
		}
	};

	/**
	 * 请求公共消息
	 * 
	 * @param isPullDwonToRefersh
	 *            是下拉刷新，还是上拉加载
	 */
	private void loadData() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				long startTime = System.currentTimeMillis();
				final List<NewFriendMessage> friends = NewFriendDao.getInstance().getAllNewFriendMsg(mLoginUserId);
				long delayTime = 200 - (startTime - System.currentTimeMillis());// 保证至少200ms的刷新过程
				if (delayTime < 0) {
					delayTime = 0;
				}
				mHandler.postDelayed(new Runnable() {
					@Override
					public void run() {
						mNewFriends.clear();
						if (friends != null && friends.size() > 0) {// 没有更多数据
							mNewFriends.addAll(friends);
						}
						mAdapter.notifyDataSetChanged();
						mPullToRefreshListView.onRefreshComplete();
					}
				}, delayTime);
			}
		}).start();

	}

	class OperationCache {
		int position;
		int type;
	}

	public Map<String, OperationCache> mOpeartionCaches = new HashMap<String, OperationCache>();

	/**
	 * 加关注或者同意别人的加好友
	 * 
	 * @param position
	 * @param 0,加关注<br/>
	 *        1、同意加好友
	 */
	private void doAgreeOrAttention(final int position, final int type) {
		final NewFriendMessage friend = mNewFriends.get(position);
		ProgressDialogUtil.show(mProgressDialog);

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("toUserId", friend.getUserId());

		StringJsonObjectRequest<AddAttentionResult> request = new StringJsonObjectRequest<AddAttentionResult>(mConfig.FRIENDS_ATTENTION_ADD,
				new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						ProgressDialogUtil.dismiss(mProgressDialog);
						ToastUtil.showErrorNet(NewFriendActivity.this);
					}
				}, new StringJsonObjectRequest.Listener<AddAttentionResult>() {
					@Override
					public void onResponse(ObjectResult<AddAttentionResult> result) {
						boolean success = Result.defaultParser(NewFriendActivity.this, result, true);
						if (success && result.getData() != null) {// 接口加关注成功
							if (result.getData().getType() == 1 || result.getData().getType() == 3) {// 单方关注成功或已经是关注的
								// 发送推送消息
								NewFriendMessage message = NewFriendMessage.createWillSendMessage(MyApplication.getInstance().mLoginUser,
										XmppMessage.TYPE_NEWSEE, null, friend);
								mXmppService.sendNewFriendMessage(friend.getUserId(), message);
								// 添加为关注
								NewFriendDao.getInstance().ascensionNewFriend(message, Friend.STATUS_ATTENTION);
								FriendHelper.addAttentionExtraOperation(mLoginUserId, friend.getUserId());

								// 提示加关注成功
								ToastUtil.showToast(NewFriendActivity.this, R.string.add_attention_succ);

								mNewFriends.set(position, message);
								mAdapter.notifyDataSetChanged();
							} else if (result.getData().getType() == 2 || result.getData().getType() == 4) {// 已经是好友了

								int messageType = type == 0 ? XmppMessage.TYPE_FRIEND : XmppMessage.TYPE_PASS;
								// 发送推送的消息
								NewFriendMessage message = NewFriendMessage.createWillSendMessage(MyApplication.getInstance().mLoginUser,
										messageType, null, friend);// 表示已经同意
								mXmppService.sendNewFriendMessage(friend.getUserId(), message);

								// 添加为好友
								NewFriendDao.getInstance().ascensionNewFriend(message, Friend.STATUS_FRIEND);
								FriendHelper.addFriendExtraOperation(mLoginUserId, friend.getUserId());

								// 提示加好友成功
								int toastResId = type == 0 ? R.string.add_friend_succ : R.string.agreed;
								ToastUtil.showToast(NewFriendActivity.this, toastResId);

								mNewFriends.set(position, message);
								mAdapter.notifyDataSetChanged();
							} else if (result.getData().getType() == 5) {
								ToastUtil.showToast(NewFriendActivity.this, R.string.add_attention_failed);
							}
						}
						ProgressDialogUtil.dismiss(mProgressDialog);
					}
				}, AddAttentionResult.class, params);
		addDefaultRequest(request);

	}

	/**
	 * 
	 * @param position
	 * @param type
	 *            0打招呼<br/>
	 *            1回话<br/>
	 */
	public void doFeedbackOrSayHello(final int position, final int type) {
		final EditText editText = new EditText(this);
		editText.setMaxLines(2);
		editText.setLines(2);
		if (type == 0) {
			editText.setHint(R.string.say_hello_dialog_hint);
		} else {
			editText.setHint(R.string.feedback);
		}

		int titleResId = type == 0 ? R.string.say_hello_dialog_title : R.string.feedback;

		editText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(20) });
		editText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(titleResId).setView(editText)
				.setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final String text = editText.getText().toString().trim();
						doFeedbackOrSayHello(position, type, text);
					}
				}).setNegativeButton(getString(R.string.cancel), null);
		builder.create().show();
	}

	public void doFeedbackOrSayHello(int position, int type, String text) {
		if (TextUtils.isEmpty(text)) {
			text = getString(R.string.say_hello_default);
		}
		NewFriendMessage friend = mNewFriends.get(position);

		int messageType = type == 0 ? XmppMessage.TYPE_SAYHELLO : XmppMessage.TYPE_FEEDBACK;

		NewFriendMessage message = NewFriendMessage.createWillSendMessage(MyApplication.getInstance().mLoginUser, messageType, text, friend);

		NewFriendDao.getInstance().createOrUpdateNewFriend(message);
		mXmppService.sendNewFriendMessage(friend.getUserId(), message);
		// 提示打招呼成功
		ToastUtil.showToast(this, R.string.feedback_succ);

		mNewFriends.set(position, message);
		mAdapter.notifyDataSetChanged();
	}

	private void removeBlacklist(final int position) {
		final NewFriendMessage friend = mNewFriends.get(position);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("toUserId", friend.getUserId());

		ProgressDialogUtil.show(mProgressDialog);
		StringJsonObjectRequest<AttentionUser> request = new StringJsonObjectRequest<AttentionUser>(mConfig.FRIENDS_BLACKLIST_DELETE,
				new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						ProgressDialogUtil.dismiss(mProgressDialog);
						ToastUtil.showErrorNet(NewFriendActivity.this);
					}
				}, new StringJsonObjectRequest.Listener<AttentionUser>() {
					@Override
					public void onResponse(ObjectResult<AttentionUser> result) {
						boolean success = Result.defaultParser(NewFriendActivity.this, result, true);
						if (success) {
							int currentStatus = Friend.STATUS_UNKNOW;
							if (result.getData() != null) {
								currentStatus = result.getData().getStatus();
							}
							FriendDao.getInstance().updateFriendStatus(friend.getOwnerId(), friend.getUserId(), currentStatus);

							NewFriendMessage message = null;
							switch (currentStatus) {
							case Friend.STATUS_ATTENTION:
								message = NewFriendMessage.createWillSendMessage(MyApplication.getInstance().mLoginUser, XmppMessage.TYPE_NEWSEE,
										null, friend);
								mXmppService.sendNewFriendMessage(friend.getUserId(), message);
								FriendHelper.addAttentionExtraOperation(friend.getOwnerId(), friend.getUserId());
								break;
							case Friend.STATUS_FRIEND:
								message = NewFriendMessage.createWillSendMessage(MyApplication.getInstance().mLoginUser, XmppMessage.TYPE_FRIEND,
										null, friend);
								mXmppService.sendNewFriendMessage(friend.getUserId(), message);
								FriendHelper.addFriendExtraOperation(friend.getOwnerId(), friend.getUserId());
								break;
							default:// 其他，理论上不可能
								break;
							}
							ToastUtil.showToast(NewFriendActivity.this, R.string.remove_blacklist_succ);
							mAdapter.notifyDataSetChanged();
						}
						ProgressDialogUtil.dismiss(mProgressDialog);
					}
				}, AttentionUser.class, params);
		addDefaultRequest(request);
	}

	@Override
	public void onNewFriendSendStateChange(String toUserId, NewFriendMessage message, int messageState) {
	}

	@Override
	public boolean onNewFriend(NewFriendMessage message) {
		loadData();
		return true;
	}

}
