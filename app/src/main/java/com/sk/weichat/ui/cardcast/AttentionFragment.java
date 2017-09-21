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
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
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
import com.sk.weichat.bean.message.NewFriendMessage;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.broadcast.MsgBroadcast;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.helper.FriendHelper;
import com.sk.weichat.sortlist.BaseComparator;
import com.sk.weichat.sortlist.BaseSortModel;
import com.sk.weichat.sortlist.PingYinUtil;
import com.sk.weichat.sortlist.SideBar;
import com.sk.weichat.ui.MainActivity;
import com.sk.weichat.ui.base.EasyFragment;
import com.sk.weichat.ui.circle.BasicInfoActivity;
import com.sk.weichat.util.ProgressDialogUtil;
import com.sk.weichat.util.StringUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.volley.ObjectResult;
import com.sk.weichat.volley.Result;
import com.sk.weichat.volley.StringJsonObjectRequest;

public class AttentionFragment extends EasyFragment {

	private PullToRefreshListView mPullToRefreshListView;
	private TextView mTextDialog;
	private SideBar mSideBar;
	private ProgressDialog mProgressDialog;
	private List<BaseSortModel<Friend>> mSortFriends;
	private BaseComparator<Friend> mBaseComparator;
	private FriendSortAdapter mAdapter;
	private String mLoginUserId;
	private Handler mHandler = new Handler();

	public AttentionFragment() {
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
				Friend friend = mSortFriends.get((int) id).getBean();
				Intent intent = new Intent(getActivity(), BasicInfoActivity.class);
				intent.putExtra(AppConstant.EXTRA_USER_ID, friend.getUserId());
				startActivity(intent);
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

		mProgressDialog = ProgressDialogUtil.init(getActivity(), null, getString(R.string.please_wait));
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

//	private MainActivity mActivity;
	private CardcastActivity mActivity;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
//		mActivity = (MainActivity) getActivity();
		mActivity = (CardcastActivity) getActivity();
	}

	private void loadData() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				long startTime = System.currentTimeMillis();
				final List<Friend> friends = FriendDao.getInstance().getAllAttentions(mLoginUserId);

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
		CharSequence[] items = new CharSequence[4];
		items[0] = getString(R.string.set_remark_name);// 设置备注名
		if (friend.getStatus() == Friend.STATUS_BLACKLIST) {// 在黑名单中,显示“设置备注名”、“移除黑名单”,"取消关注"，“彻底删除”
			items[1] = getString(R.string.remove_blacklist);
		} else {
			items[1] = getString(R.string.add_blacklist);
		}
		items[2] = getString(R.string.cancel_attention);
		items[3] = getString(R.string.delete_all);

		new AlertDialog.Builder(getActivity()).setItems(items, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				switch (which) {
				case 0:// 设置备注名
					showRemarkDialog(sortFriend);
					break;
				case 1:// 加入黑名单，或者移除黑名单
					showBlacklistDialog(sortFriend);
					break;
				case 2:// 取消关注
					showCancelAttentionDialog(sortFriend);
					break;
				case 3:// 解除关注关系或者解除好友关系
					showDeleteAllDialog(sortFriend);
					break;
				}
			}
		}).setCancelable(true).create().show();
	}

	private void showRemarkDialog(final BaseSortModel<Friend> sortFriend) {
		final EditText editText = new EditText(getActivity());
		editText.setMaxLines(2);
		editText.setLines(2);
		editText.setText(sortFriend.getBean().getShowName());
		editText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(20) });
		editText.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(R.string.set_remark_name).setView(editText)
				.setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String input = editText.getText().toString();
						if (input.equals(sortFriend.getBean().getShowName())) {// 备注名没变
							return;
						}
						if (!StringUtils.isNickName(input)) {// 不符合昵称
							if (input.length() != 0) {
								ToastUtil.showToast(getActivity(), R.string.remark_name_format_error);
								return;
							} else {// 不符合昵称，因为长度为0，但是可以做备注名操作，操作就是清除备注名
									// 判断之前有没有备注名
								if (TextUtils.isEmpty(sortFriend.getBean().getRemarkName())) {// 如果没有备注名，就不需要清除
									return;
								}
							}
						}
						remarkFriend(sortFriend, input);
					}
				}).setNegativeButton(getString(R.string.cancel), null);
		builder.create().show();
	}

	private void remarkFriend(final BaseSortModel<Friend> sortFriend, final String remarkName) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("toUserId", sortFriend.getBean().getUserId());
		params.put("remarkName", remarkName);

		ProgressDialogUtil.show(mProgressDialog);
		StringJsonObjectRequest<Result> request = new StringJsonObjectRequest<Result>(mActivity.mConfig.FRIENDS_REMARK, new ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
				ProgressDialogUtil.dismiss(mProgressDialog);
				ToastUtil.showErrorNet(getActivity());
			}
		}, new StringJsonObjectRequest.Listener<Result>() {
			@Override
			public void onResponse(ObjectResult<Result> result) {
				boolean success = Result.defaultParser(getActivity(), result, true);
				ProgressDialogUtil.dismiss(mProgressDialog);
				if (success) {
					String firstLetter = sortFriend.getFirstLetter();
					mSideBar.removeExist(firstLetter);// 移除之前设置的首字母
					sortFriend.getBean().setRemarkName(remarkName);// 修改备注名称
					setSortCondition(sortFriend);
					Collections.sort(mSortFriends, mBaseComparator);
					mAdapter.notifyDataSetChanged();
					// 更新到数据库
					FriendDao.getInstance().setRemarkName(mLoginUserId, sortFriend.getBean().getUserId(), remarkName);
					// 更新消息界面（因为昵称变了，所有要更新）
					MsgBroadcast.broadcastMsgUiUpdate(getActivity());
				}

			}
		}, Result.class, params);
		mActivity.addDefaultRequest(request);
	}

	/* 显示加入黑名单的对话框 */
	private void showBlacklistDialog(final BaseSortModel<Friend> sortFriend) {
		final Friend friend = sortFriend.getBean();
		int messageId = 0;
		if (friend.getStatus() == Friend.STATUS_BLACKLIST) {// 已经在黑名单，那就是移出黑名单(在名片盒界面，不可能出现此情况，从别的地方copy过来的，懒得去掉)
			messageId = R.string.remove_blacklist_prompt;
		} else if (friend.getStatus() == Friend.STATUS_ATTENTION || friend.getStatus() == Friend.STATUS_FRIEND) {
			messageId = R.string.add_blacklist_prompt;
		} else {// 其他关系（错误的状态）
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setTitle(R.string.prompt_title).setMessage(messageId)
				.setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (friend.getStatus() == Friend.STATUS_BLACKLIST) {// 已经在黑名单，那就是移出黑名单(在名片盒界面，不可能出现此情况)
							// removeBlacklist(friend);
						} else if (friend.getStatus() == Friend.STATUS_ATTENTION || friend.getStatus() == Friend.STATUS_FRIEND) {
							addBlacklist(sortFriend);
						}
					}
				}).setNegativeButton(getString(R.string.cancel), null);
		builder.create().show();
	}

	private void addBlacklist(final BaseSortModel<Friend> sortFriend) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("toUserId", sortFriend.getBean().getUserId());

		ProgressDialogUtil.show(mProgressDialog);
		StringJsonObjectRequest<Void> request = new StringJsonObjectRequest<Void>(mActivity.mConfig.FRIENDS_BLACKLIST_ADD, new ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
				ProgressDialogUtil.dismiss(mProgressDialog);
				ToastUtil.showErrorNet(getActivity());
			}
		}, new StringJsonObjectRequest.Listener<Void>() {
			@Override
			public void onResponse(ObjectResult<Void> result) {

				boolean success = Result.defaultParser(getActivity(), result, true);
				if (success) {
					FriendDao.getInstance().updateFriendStatus(sortFriend.getBean().getOwnerId(), sortFriend.getBean().getUserId(),
							Friend.STATUS_BLACKLIST);
					FriendHelper.addBlacklistExtraOperation(mLoginUserId, sortFriend.getBean().getUserId());

					/* 发送加入黑名单的通知 */
					if (sortFriend.getBean().getStatus() == Friend.STATUS_FRIEND) {// 之前是好友，需要发消息让那个人不能看我的商务圈
						NewFriendMessage message = NewFriendMessage.createWillSendMessage(MyApplication.getInstance().mLoginUser,
								XmppMessage.TYPE_BLACK, null, sortFriend.getBean());
						mActivity.sendNewFriendMessage(sortFriend.getBean().getUserId(), message);// 加入黑名单
					}

					ToastUtil.showToast(getActivity(), R.string.add_blacklist_succ);

					mSortFriends.remove(sortFriend);
					String firstLetter = sortFriend.getFirstLetter();
					mSideBar.removeExist(firstLetter);// 移除之前设置的首字母
					mAdapter.notifyDataSetInvalidated();

					// 更新消息界面
					MsgBroadcast.broadcastMsgUiUpdate(getActivity());
				}
				ProgressDialogUtil.dismiss(mProgressDialog);
			}
		}, Void.class, params);
		mActivity.addDefaultRequest(request);
	}

	/**
	 * 取消关注
	 * 
	 * @param friend
	 */
	private void showCancelAttentionDialog(final BaseSortModel<Friend> sortFriend) {
		if (sortFriend.getBean().getStatus() == Friend.STATUS_UNKNOW) {
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setTitle(R.string.prompt_title)
				.setMessage(R.string.cancel_attention_prompt).setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						deleteFriend(sortFriend, 0);
					}
				}).setNegativeButton(getString(R.string.cancel), null);
		builder.create().show();
	}

	private void showDeleteAllDialog(final BaseSortModel<Friend> sortFriend) {
		if (sortFriend.getBean().getStatus() == Friend.STATUS_UNKNOW) {
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(getActivity()).setTitle(R.string.prompt_title).setMessage(R.string.delete_all_prompt)
				.setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						deleteFriend(sortFriend, 1);
					}
				}).setNegativeButton(getString(R.string.cancel), null);
		builder.create().show();
	}

	/**
	 * 
	 * @param friend
	 * @param type
	 *            0 取消关注 <br/>
	 *            1、彻底删除<br/>
	 */
	private void deleteFriend(final BaseSortModel<Friend> sortFriend, final int type) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("toUserId", sortFriend.getBean().getUserId());

		String url = null;
		if (type == 0) {
			url = mActivity.mConfig.FRIENDS_ATTENTION_DELETE;// 取消关注
		} else {
			url = mActivity.mConfig.FRIENDS_DELETE;// 删除好友
		}

		ProgressDialogUtil.show(mProgressDialog);
		StringJsonObjectRequest<Void> request = new StringJsonObjectRequest<Void>(url, new ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
				ProgressDialogUtil.dismiss(mProgressDialog);
				ToastUtil.showErrorNet(getActivity());
			}
		}, new StringJsonObjectRequest.Listener<Void>() {
			@Override
			public void onResponse(ObjectResult<Void> result) {
				boolean success = Result.defaultParser(getActivity(), result, true);
				if (success) {
					if (type == 0) {
						ToastUtil.showToast(getActivity(), R.string.cancel_attention_succ);
						NewFriendMessage message = NewFriendMessage.createWillSendMessage(MyApplication.getInstance().mLoginUser,
								XmppMessage.TYPE_DELSEE, null, sortFriend.getBean());
						mActivity.sendNewFriendMessage(sortFriend.getBean().getUserId(), message);// 解除关注
					} else {
						ToastUtil.showToast(getActivity(), R.string.delete_all_succ);
						NewFriendMessage message = NewFriendMessage.createWillSendMessage(MyApplication.getInstance().mLoginUser,
								XmppMessage.TYPE_DELALL, null, sortFriend.getBean());
						mActivity.sendNewFriendMessage(sortFriend.getBean().getUserId(), message);// 解除好友
					}

					FriendHelper.removeAttentionOrFriend(mLoginUserId, sortFriend.getBean().getUserId());

					mSortFriends.remove(sortFriend);
					String firstLetter = sortFriend.getFirstLetter();
					mSideBar.removeExist(firstLetter);// 移除之前设置的首字母
					mAdapter.notifyDataSetInvalidated();

					// 更新消息界面
					MsgBroadcast.broadcastMsgUiUpdate(getActivity());
				}
				ProgressDialogUtil.dismiss(mProgressDialog);
			}
		}, Void.class, params);
		mActivity.addDefaultRequest(request);
	}

}
