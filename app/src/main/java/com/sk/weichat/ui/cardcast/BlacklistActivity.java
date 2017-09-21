//package com.sk.weichat.ui.cardcast;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//
//import android.app.AlertDialog;
//import android.app.ProgressDialog;
//import android.content.BroadcastReceiver;
//import android.content.ComponentName;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.ServiceConnection;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.IBinder;
//import android.text.InputFilter;
//import android.text.TextUtils;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.AdapterView;
//import android.widget.EditText;
//import android.widget.ListView;
//import android.widget.TextView;
//
//import com.android.volley.Response.ErrorListener;
//import com.android.volley.VolleyError;
//import com.handmark.pulltorefresh.library.PullToRefreshBase;
//import com.handmark.pulltorefresh.library.PullToRefreshBase.Mode;
//import com.handmark.pulltorefresh.library.PullToRefreshListView;
//import com.sk.im.AnalysisActivity;
//import com.sk.im.Constant;
//import com.sk.im.MyApplication;
//import com.sk.im.R;
//import com.sk.im.adapter.FriendSortAdapter;
//import com.sk.im.bean.AttentionUser;
//import com.sk.im.bean.Friend;
//import com.sk.im.bean.message.NewFriendMessage;
//import com.sk.im.bean.message.XmppMessage;
//import com.sk.im.broadcast.CardcastUiUpdateUtil;
//import com.sk.im.broadcast.MsgBroadcast;
//import com.sk.im.db.dao.FriendDao;
//import com.sk.im.service.FriendHelper;
//import com.sk.im.sortlist.BaseComparator;
//import com.sk.im.sortlist.BaseSortModel;
//import com.sk.im.sortlist.PingYinUtil;
//import com.sk.im.sortlist.SideBar;
//import com.sk.im.ui.circle.PersonalInfoActivity;
//import com.sk.im.util.ProgressDialogUtil;
//import com.sk.im.util.ToastUtil;
//import com.sk.im.util.Utils;
//import com.sk.im.view.TopNormalBar;
//import com.sk.im.volley.FastVolley;
//import com.sk.im.volley.ObjectResult;
//import com.sk.im.volley.Result;
//import com.sk.im.volley.StringJsonObjectRequest;
//import com.sk.im.xmpp.CoreService;
//import com.sk.im.xmpp.CoreService.CoreServiceBinder;
//
//public class BlacklistActivity extends AnalysisActivity {
//
//	private TopNormalBar mTopTitleBar;
//	private PullToRefreshListView mPullToRefreshListView;
//	private TextView mTextDialog;
//	private SideBar mSideBar;
//	private ProgressDialog mProgressDialog;
//	private List<BaseSortModel<Friend>> mSortFriends;
//	private BaseComparator<Friend> mBaseComparator;
//	private FriendSortAdapter mAdapter;
//	private String mLoginUserId;
//	private Handler mHandler = new Handler();
//
//	private boolean mBind;
//	private CoreService mXmppService;
//
//	@Override
//	protected void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//
//		mSortFriends = new ArrayList<BaseSortModel<Friend>>();
//		mBaseComparator = new BaseComparator<Friend>();
//		mLoginUserId = MyApplication.getInstance().mLoginUser.getUserId();
//
//		setContentView(R.layout.activity_simple_sort_pullrefresh_list);
//		initView();
//
//		registerReceiver(mUpdateReceiver, CardcastUiUpdateUtil.getUpdateActionFilter());
//		mBind = bindService(CoreService.getIntent(), mServiceConnection, BIND_AUTO_CREATE);
//
//	}
//
//	private ServiceConnection mServiceConnection = new ServiceConnection() {
//		@Override
//		public void onServiceDisconnected(ComponentName name) {
//			mXmppService = null;
//		}
//
//		@Override
//		public void onServiceConnected(ComponentName name, IBinder service) {
//			mXmppService = ((CoreServiceBinder) service).getService();
//		}
//	};
//
//	@Override
//	protected void onDestroy() {
//		super.onDestroy();
//		unregisterReceiver(mUpdateReceiver);
//		if (mBind) {
//			unbindService(mServiceConnection);
//		}
//	}
//
//	private boolean mNeedUpdate = true;
//	private boolean mResumed = false;
//
//	@Override
//	protected void onResume() {
//		super.onResume();
//		if (mNeedUpdate) {
//			loadData();
//			mNeedUpdate = false;
//		}
//		mResumed = true;
//	}
//
//	@Override
//	protected void onPause() {
//		super.onPause();
//		mResumed = false;
//	}
//
//	private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
//		@Override
//		public void onReceive(Context context, Intent intent) {
//			if (intent.getAction().equals(CardcastUiUpdateUtil.ACTION_UPDATE_UI)) {
//				if (mResumed) {
//					loadData();
//				} else {
//					mNeedUpdate = true;
//				}
//			}
//		}
//	};
//
//	private void initView() {
//		initTopTitleBar();
//		mPullToRefreshListView = (PullToRefreshListView) findViewById(R.id.pull_refresh_list);
//		mTextDialog = (TextView) findViewById(R.id.text_dialog);
//		mSideBar = (SideBar) findViewById(R.id.sidebar);
//		mSideBar.setTextView(mTextDialog);
//
//		mSideBar.setOnTouchingLetterChangedListener(new SideBar.OnTouchingLetterChangedListener() {
//			@Override
//			public void onTouchingLetterChanged(String s) {
//				// 该字母首次出现的位置
//				int position = mAdapter.getPositionForSection(s.charAt(0));
//				if (position != -1) {
//					mPullToRefreshListView.getRefreshableView().setSelection(position);
//				}
//			}
//		});
//
//		mAdapter = new FriendSortAdapter(this, mSortFriends);
//
//		mPullToRefreshListView.setMode(Mode.PULL_FROM_START);
//		mPullToRefreshListView.getRefreshableView().setAdapter(mAdapter);
//
//		mPullToRefreshListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
//			@Override
//			public void onRefresh(PullToRefreshBase<ListView> refreshView) {
//				loadData();
//			}
//		});
//
//		mPullToRefreshListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//			@Override
//			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//				Friend friend = mSortFriends.get((int) id).getBean();
//				Intent intent = new Intent(BlacklistActivity.this, PersonalInfoActivity.class);
//				intent.putExtra(Constant.EXTRA_USER_ID, friend.getUserId());
//				startActivity(intent);
//			}
//		});
//
//		mPullToRefreshListView.getRefreshableView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
//			@Override
//			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
//				BaseSortModel<Friend> sortFriend = mSortFriends.get((int) id);
//				if (sortFriend == null || sortFriend.getBean() == null) {
//					return false;
//				}
//				showLongClickOperationDialog(sortFriend);
//				return true;
//			}
//		});
//
//		mProgressDialog = ProgressDialogUtil.init(this, null, getString(R.string.requesting));
//
//	}
//
//	private void initTopTitleBar() {
//		mTopTitleBar = (TopNormalBar) findViewById(R.id.top_title_bar);
//		mTopTitleBar.title(R.string.my_blacklist);
//	}
//
//	private void loadData() {
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				long startTime = System.currentTimeMillis();
//				final List<Friend> friends = FriendDao.getInstance().getAllBlacklists(mLoginUserId);
//
//				long delayTime = 200 - (startTime - System.currentTimeMillis());// 保证至少500ms的刷新过程
//				if (delayTime < 0) {
//					delayTime = 0;
//				}
//
//				mHandler.postDelayed(new Runnable() {
//					@Override
//					public void run() {
//						mSortFriends.clear();
//						mSideBar.clearExist();
//						if (friends != null && friends.size() > 0) {
//							for (int i = 0; i < friends.size(); i++) {
//								BaseSortModel<Friend> mode = new BaseSortModel<Friend>();
//								mode.setBean(friends.get(i));
//								setSortCondition(mode);
//								mSortFriends.add(mode);
//							}
//							Collections.sort(mSortFriends, mBaseComparator);
//						}
//						mAdapter.notifyDataSetInvalidated();
//						mPullToRefreshListView.onRefreshComplete();
//					}
//				}, delayTime);
//			}
//		}).start();
//	}
//
//	private final void setSortCondition(BaseSortModel<Friend> mode) {
//		Friend friend = mode.getBean();
//		if (friend == null) {
//			return;
//		}
//		String name = friend.getShowName();
//		String wholeSpell = PingYinUtil.getPingYin(name);
//		if (!TextUtils.isEmpty(wholeSpell)) {
//			String firstLetter = Character.toString(wholeSpell.charAt(0));
//			mSideBar.addExist(firstLetter);
//			mode.setWholeSpell(wholeSpell);
//			mode.setFirstLetter(firstLetter);
//			mode.setSimpleSpell(PingYinUtil.converterToFirstSpell(name));
//		} else {// 如果全拼为空，理论上是一种错误情况，因为这代表着昵称为空
//			mode.setWholeSpell("#");
//			mode.setFirstLetter("#");
//			mode.setSimpleSpell("#");
//		}
//	}
//
//	// ///////////其他操作///////////////////
//	private void showLongClickOperationDialog(final BaseSortModel<Friend> sortFriend) {
//		Friend friend = sortFriend.getBean();
//		if (friend.getStatus() != Friend.STATUS_BLACKLIST && friend.getStatus() == Friend.STATUS_ATTENTION
//				&& friend.getStatus() == Friend.STATUS_FRIEND) {
//			return;
//		}
//		CharSequence[] items = new CharSequence[4];
//		items[0] = getString(R.string.set_remark_name);// 设置备注名
//		if (friend.getStatus() == Friend.STATUS_BLACKLIST) {// 在黑名单中,显示“设置备注名”、“移除黑名单”,"取消关注"，“彻底删除”
//			items[1] = getString(R.string.remove_blacklist);
//		} else {
//			items[1] = getString(R.string.add_blacklist);
//		}
//		items[2] = getString(R.string.cancel_attention);
//		items[3] = getString(R.string.delete_all);
//
//		new AlertDialog.Builder(this).setItems(items, new DialogInterface.OnClickListener() {
//			@Override
//			public void onClick(DialogInterface dialog, int which) {
//				switch (which) {
//				case 0:// 设置备注名
//					showRemarkDialog(sortFriend);
//					break;
//				case 1:// 加入黑名单，或者移除黑名单
//					showBlacklistDialog(sortFriend);
//					break;
//				case 2:// 取消关注
//					showCancelAttentionDialog(sortFriend);
//					break;
//				case 3:// 解除关注关系或者解除好友关系
//					showDeleteAllDialog(sortFriend);
//					break;
//				}
//			}
//		}).setCancelable(true).create().show();
//	}
//
//	private void showRemarkDialog(final BaseSortModel<Friend> sortFriend) {
//		final EditText editText = new EditText(BlacklistActivity.this);
//		editText.setMaxLines(2);
//		editText.setLines(2);
//		editText.setText(sortFriend.getBean().getShowName());
//		editText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(20) });
//		editText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
//
//		AlertDialog.Builder builder = new AlertDialog.Builder(this);
//		builder.setTitle(R.string.set_remark_name).setView(editText)
//				.setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface dialog, int which) {
//						String input = editText.getText().toString();
//						if (input.equals(sortFriend.getBean().getShowName())) {// 备注名没变
//							return;
//						}
//						if (!Utils.isNickName(input)) {// 不符合昵称
//							if (input.length() != 0) {
//								ToastUtil.showNormalToast(BlacklistActivity.this, R.string.remark_name_format_error);
//								return;
//							} else {// 不符合昵称，因为长度为0，但是可以做备注名操作，操作就是清除备注名
//									// 判断之前有没有备注名
//								if (TextUtils.isEmpty(sortFriend.getBean().getRemarkName())) {// 如果没有备注名，就不需要清除
//									return;
//								}
//							}
//						}
//						remarkFriend(sortFriend, input);
//					}
//				}).setNegativeButton(getString(R.string.cancel), null);
//		builder.create().show();
//	}
//
//	private void remarkFriend(final BaseSortModel<Friend> sortFriend, final String remarkName) {
//		HashMap<String, String> params = new HashMap<String, String>();
//		params.put("access_token", MyApplication.getInstance().mAccessToken);
//		params.put("toUserId", sortFriend.getBean().getUserId());
//		params.put("remarkName", remarkName);
//
//		ProgressDialogUtil.show(mProgressDialog);
//		StringJsonObjectRequest<Result> request = new StringJsonObjectRequest<Result>(mConfig.FRIENDS_REMARK, new ErrorListener() {
//			@Override
//			public void onErrorResponse(VolleyError arg0) {
//				ProgressDialogUtil.dismiss(mProgressDialog);
//				ToastUtil.showErrorNet(BlacklistActivity.this);
//			}
//		}, new StringJsonObjectRequest.Listener<Result>() {
//			@Override
//			public void onResponse(ObjectResult<Result> result) {
//				boolean success = Result.defaultParser(BlacklistActivity.this, result, true);
//				ProgressDialogUtil.dismiss(mProgressDialog);
//				if (success) {
//					String firstLetter = sortFriend.getFirstLetter();
//					mSideBar.removeExist(firstLetter);// 移除之前设置的首字母
//					sortFriend.getBean().setRemarkName(remarkName);// 修改备注名称
//					setSortCondition(sortFriend);
//					Collections.sort(mSortFriends, mBaseComparator);
//					mAdapter.notifyDataSetChanged();
//					// 更新到数据库
//					FriendDao.getInstance().setRemarkName(mLoginUserId, sortFriend.getBean().getUserId(), remarkName);
//					// 更新消息界面（因为昵称变了，所有要更新）
//					// MainUiUpdateUtil.broadcastUpdateMsgUi(BlacklistActivity.this);
//				}
//
//			}
//		}, Result.class, params);
//		request.setRetryPolicy(FastVolley.newDefaultRetryPolicy());
//		FastVolley.getInstance().add(request);
//
//	}
//
//	/* 显示加入黑名单的对话框 */
//	private void showBlacklistDialog(final BaseSortModel<Friend> sortFriend) {
//		final Friend friend = sortFriend.getBean();
//		int messageId = 0;
//		if (friend.getStatus() == Friend.STATUS_BLACKLIST) {// 已经在黑名单，那就是移出黑名单
//			messageId = R.string.remove_blacklist_prompt;
//		} else if (friend.getStatus() == Friend.STATUS_ATTENTION || friend.getStatus() == Friend.STATUS_FRIEND) {
//			messageId = R.string.add_blacklist_prompt;
//		} else {// 其他关系（错误的状态）
//			return;
//		}
//		AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.prompt_title).setMessage(messageId)
//				.setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface dialog, int which) {
//						if (friend.getStatus() == Friend.STATUS_BLACKLIST) {//
//							removeBlacklist(sortFriend);
//						} else if (friend.getStatus() == Friend.STATUS_ATTENTION || friend.getStatus() == Friend.STATUS_FRIEND) {
//							// addBlacklist(sortFriend);//黑名单界面，不可能出现此情况
//						}
//					}
//				}).setNegativeButton(getString(R.string.cancel), null);
//		builder.create().show();
//	}
//
//	private void removeBlacklist(final BaseSortModel<Friend> sortFriend) {
//		HashMap<String, String> params = new HashMap<String, String>();
//		params.put("access_token", MyApplication.getInstance().mAccessToken);
//		params.put("toUserId", sortFriend.getBean().getUserId());
//
//		ProgressDialogUtil.show(mProgressDialog);
//		StringJsonObjectRequest<AttentionUser> request = new StringJsonObjectRequest<AttentionUser>(mConfig.FRIENDS_BLACKLIST_DELETE,
//				new ErrorListener() {
//					@Override
//					public void onErrorResponse(VolleyError arg0) {
//						ProgressDialogUtil.dismiss(mProgressDialog);
//						ToastUtil.showErrorNet(BlacklistActivity.this);
//					}
//				}, new StringJsonObjectRequest.Listener<AttentionUser>() {
//					@Override
//					public void onResponse(ObjectResult<AttentionUser> result) {
//						boolean success = Result.defaultParser(BlacklistActivity.this, result, true);
//						if (success) {
//							int currentStatus = Friend.STATUS_UNKNOW;
//							if (result.getData() != null) {
//								currentStatus = result.getData().getStatus();
//							}
//							FriendDao.getInstance().updateFriendStatus(sortFriend.getBean().getOwnerId(), sortFriend.getBean().getUserId(),
//									currentStatus);
//
//							switch (currentStatus) {
//							case Friend.STATUS_ATTENTION:
//								NewFriendMessage message1 = NewFriendMessage.createWillSendMessage(MyApplication.getInstance().mLoginUser,
//										XmppMessage.TYPE_NEWSEE, null, sortFriend.getBean());
//								mXmppService.sendNewFriendMessage(sortFriend.getBean().getUserId(), message1);
//
//								FriendHelper.addAttentionExtraOperation(sortFriend.getBean().getOwnerId(), sortFriend.getBean().getUserId());
//
//								break;
//							case Friend.STATUS_FRIEND:
//
//								NewFriendMessage message2 = NewFriendMessage.createWillSendMessage(MyApplication.getInstance().mLoginUser,
//										XmppMessage.TYPE_FRIEND, null, sortFriend.getBean());
//								mXmppService.sendNewFriendMessage(sortFriend.getBean().getUserId(), message2);
//								FriendHelper.addFriendExtraOperation(sortFriend.getBean().getOwnerId(), sortFriend.getBean().getUserId());
//								break;
//							default:// 其他，理论上不可能
//								break;
//							}
//
//							ToastUtil.showNormalToast(BlacklistActivity.this, R.string.remove_blacklist_succ);
//
//							mSortFriends.remove(sortFriend);
//							String firstLetter = sortFriend.getFirstLetter();
//							mSideBar.removeExist(firstLetter);// 移除之前设置的首字母
//							mAdapter.notifyDataSetInvalidated();
//
//							// 更新消息界面
//							MsgBroadcast.broadcastMsgUiUpdate(mContext);
//						}
//						ProgressDialogUtil.dismiss(mProgressDialog);
//					}
//				}, AttentionUser.class, params);
//		request.setRetryPolicy(FastVolley.newDefaultRetryPolicy());
//		FastVolley.getInstance().add(request);
//	}
//
//	/**
//	 * 取消关注
//	 * 
//	 * @param friend
//	 */
//	private void showCancelAttentionDialog(final BaseSortModel<Friend> sortFriend) {
//		if (sortFriend.getBean().getStatus() == Friend.STATUS_UNKNOW) {
//			return;
//		}
//		AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.prompt_title).setMessage(R.string.cancel_attention_prompt)
//				.setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface dialog, int which) {
//						deleteFriend(sortFriend, 0);
//					}
//				}).setNegativeButton(getString(R.string.cancel), null);
//		builder.create().show();
//	}
//
//	private void showDeleteAllDialog(final BaseSortModel<Friend> sortFriend) {
//		if (sortFriend.getBean().getStatus() == Friend.STATUS_UNKNOW) {
//			return;
//		}
//		AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.prompt_title).setMessage(R.string.delete_all_prompt)
//				.setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface dialog, int which) {
//						deleteFriend(sortFriend, 1);
//					}
//				}).setNegativeButton(getString(R.string.cancel), null);
//		builder.create().show();
//	}
//
//	/**
//	 * 
//	 * @param friend
//	 * @param type
//	 *            0 取消关注 <br/>
//	 *            1、彻底删除<br/>
//	 */
//	private void deleteFriend(final BaseSortModel<Friend> sortFriend, final int type) {
//		HashMap<String, String> params = new HashMap<String, String>();
//		params.put("access_token", MyApplication.getInstance().mAccessToken);
//		params.put("toUserId", sortFriend.getBean().getUserId());
//
//		String url = null;
//		if (type == 0) {
//			url = mConfig.FRIENDS_ATTENTION_DELETE;// 取消关注
//		} else {
//			url = mConfig.FRIENDS_DELETE;// 删除好友
//		}
//
//		ProgressDialogUtil.show(mProgressDialog);
//		StringJsonObjectRequest<Void> request = new StringJsonObjectRequest<Void>(url, new ErrorListener() {
//			@Override
//			public void onErrorResponse(VolleyError arg0) {
//				ProgressDialogUtil.dismiss(mProgressDialog);
//				ToastUtil.showErrorNet(BlacklistActivity.this);
//			}
//		}, new StringJsonObjectRequest.Listener<Void>() {
//			@Override
//			public void onResponse(ObjectResult<Void> result) {
//				boolean success = Result.defaultParser(BlacklistActivity.this, result, true);
//				if (success) {
//					if (type == 0) {
//						ToastUtil.showNormalToast(BlacklistActivity.this, R.string.cancel_attention_succ);
//						NewFriendMessage message = NewFriendMessage.createWillSendMessage(MyApplication.getInstance().mLoginUser,
//								XmppMessage.TYPE_DELSEE, null, sortFriend.getBean());
//						mXmppService.sendNewFriendMessage(sortFriend.getBean().getUserId(), message);// 解除关注
//					} else {
//						ToastUtil.showNormalToast(BlacklistActivity.this, R.string.delete_all_succ);
//						NewFriendMessage message = NewFriendMessage.createWillSendMessage(MyApplication.getInstance().mLoginUser,
//								XmppMessage.TYPE_DELALL, null, sortFriend.getBean());
//						mXmppService.sendNewFriendMessage(sortFriend.getBean().getUserId(), message);// 解除好友
//					}
//
//					FriendHelper.removeAttentionOrFriend(mLoginUserId, sortFriend.getBean().getUserId());
//
//					mSortFriends.remove(sortFriend);
//					String firstLetter = sortFriend.getFirstLetter();
//					mSideBar.removeExist(firstLetter);// 移除之前设置的首字母
//					mAdapter.notifyDataSetInvalidated();
//
//					// 更新消息界面
//					MsgBroadcast.broadcastMsgUiUpdate(mContext);
//				}
//				ProgressDialogUtil.dismiss(mProgressDialog);
//			}
//		}, Void.class, params);
//		request.setRetryPolicy(FastVolley.newDefaultRetryPolicy());
//		FastVolley.getInstance().add(request);
//
//	}
//
//}
