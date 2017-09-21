package com.sk.weichat.ui.circle;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.VolleyError;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.AddAttentionResult;
import com.sk.weichat.bean.Area;
import com.sk.weichat.bean.AttentionUser;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.User;
import com.sk.weichat.bean.message.NewFriendMessage;
import com.sk.weichat.bean.message.XmppMessage;
import com.sk.weichat.broadcast.CardcastUiUpdateUtil;
import com.sk.weichat.broadcast.MsgBroadcast;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.db.dao.NewFriendDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.helper.FriendHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.message.ChatActivity;
import com.sk.weichat.util.ProgressDialogUtil;
import com.sk.weichat.util.StringUtils;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.view.DataLoadView;
import com.sk.weichat.volley.ObjectResult;
import com.sk.weichat.volley.Result;
import com.sk.weichat.volley.StringJsonObjectRequest;
import com.sk.weichat.xmpp.CoreService;
import com.sk.weichat.xmpp.CoreService.CoreServiceBinder;
import com.sk.weichat.xmpp.ListenerManager;
import com.sk.weichat.xmpp.listener.NewFriendListener;
import com.ymz.baselibrary.utils.L_;

import java.util.HashMap;
import java.util.Map;

/**
 * 聊天点头像查看个人基本资料
 * 
 * @author Dean Tao
 * 
 */
public class BasicInfoActivity extends BaseActivity implements NewFriendListener {

	private ImageView mAvatarImg;
	private TextView mNameTv;
	private TextView mSexTv;
	private TextView mBirthdayTv;
	private TextView mCityTv;
	private Button mNextStepBtn;
	private Button mLookLocationBtn;
	private DataLoadView mDataLoadView;

	private String mUserId;
	private User mUser;
	private Friend mFriend;// 如果这个用户是当前登陆者的好友或者关注着，那么该值有意义
	private ProgressDialog mProgressDialog;
	private boolean mBind;
	private CoreService mXmppService;

	private String mLoginUserId;
	private boolean isMyInfo = false;// 快捷判断

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.getWindow().invalidatePanelMenu(Window.FEATURE_OPTIONS_PANEL);
		if (getIntent() != null) {
			mUserId = getIntent().getStringExtra(AppConstant.EXTRA_USER_ID);
		}
		mLoginUserId = MyApplication.getInstance().mLoginUser.getUserId();
		if (TextUtils.isEmpty(mUserId)) {
			mUserId = mLoginUserId;
		}
		setContentView(R.layout.activity_basic_info);
		mProgressDialog = ProgressDialogUtil.init(this, null, getString(R.string.please_wait));

		initView();

		if (mLoginUserId.equals(mUserId) || TextUtils.isEmpty(mUserId)) {// 显示我的资料
			mUserId = mLoginUserId;// 让mUserId变为和登陆者一样，当做是查看登陆者自己的个人资料
			isMyInfo = true;
			loadMyInfoFromDb();
		} else {// 显示其他用户的资料
			isMyInfo = false;
			loadOthersInfoFromNet();
		}

		ListenerManager.getInstance().addNewFriendListener(this);
		mBind = bindService(CoreService.getIntent(), mServiceConnection, BIND_AUTO_CREATE);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (showMenu && mFriend != null) {
			menu.clear();//将之前的clear掉,防止重复
			if (mFriend.getStatus() != Friend.STATUS_BLACKLIST && mFriend.getStatus() == Friend.STATUS_ATTENTION
					&& mFriend.getStatus() == Friend.STATUS_FRIEND) {
				;
			} else {
				getMenuInflater().inflate(R.menu.menu_basic_info, menu);
				if (mFriend.getStatus() == Friend.STATUS_BLACKLIST) {
					// 在黑名单中,显示“设置备注名”、“移除黑名单”,"取消关注"，“彻底删除”
					menu.findItem(R.id.add_blacklist).setVisible(false);
				} else {
					menu.findItem(R.id.remove_blacklist).setVisible(false);
				}
			}
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (showMenu && mFriend != null) {
			if (mFriend.getStatus() != Friend.STATUS_BLACKLIST && mFriend.getStatus() == Friend.STATUS_ATTENTION
					&& mFriend.getStatus() == Friend.STATUS_FRIEND) {
				;
			} else {
				getMenuInflater().inflate(R.menu.menu_basic_info, menu);
				if (mFriend.getStatus() == Friend.STATUS_BLACKLIST) {// 在黑名单中,显示“设置备注名”、“移除黑名单”,"取消关注"，“彻底删除”
					menu.findItem(R.id.add_blacklist).setVisible(false);
				} else {
					menu.findItem(R.id.remove_blacklist).setVisible(false);
				}
			}
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onBackPressed() {
		// TODO Auto-generated method stub
		super.onBackPressed();
		finish();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mFriend == null) {
			return super.onOptionsItemSelected(item);
		}

		if (mFriend.getStatus() != Friend.STATUS_BLACKLIST && mFriend.getStatus() == Friend.STATUS_ATTENTION
				&& mFriend.getStatus() == Friend.STATUS_FRIEND) {
			return super.onOptionsItemSelected(item);
		}
		CharSequence[] items = new CharSequence[4];
		items[0] = getString(R.string.set_remark_name);// 设置备注名
		if (mFriend.getStatus() == Friend.STATUS_BLACKLIST) {// 在黑名单中,显示“设置备注名”、“移除黑名单”,"取消关注"，“彻底删除”
			items[1] = getString(R.string.remove_blacklist);
		} else {
			items[1] = getString(R.string.add_blacklist);
		}

		items[2] = getString(R.string.cancel_attention);
		items[3] = getString(R.string.delete_all);
		int itemId = item.getItemId();
		switch (itemId) {
		case R.id.set_remark_name:// 设置备注名
			showRemarkDialog(mFriend);
			break;
		case R.id.remove_blacklist:// 加入黑名单，或者移除黑名单
			showBlacklistDialog(mFriend);
			break;
		case R.id.add_blacklist:// 加入黑名单，或者移除黑名单
			showBlacklistDialog(mFriend);
			break;
		case R.id.cancel_attention:// 设置备注名
			showCancelAttentionDialog(mFriend);
			break;
		case R.id.delete_all:// 设置备注名
			showDeleteAllDialog(mFriend);
			break;
		default:
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	/**
	 * 懒得判断操作的用户到底属于好友、企业、还是公司，直接发广播，让所有的名片盒页面都更新
	 */
	private void updateAllCardcastUi() {
		CardcastUiUpdateUtil.broadcastUpdateUi(this);
	}

	private void loadMyInfoFromDb() {
		mDataLoadView.showSuccess();
		mUser = MyApplication.getInstance().mLoginUser;
		updateUI();
	}

	private void loadOthersInfoFromNet() {
		mDataLoadView.showLoading();
		Map<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("userId", mUserId);
		StringJsonObjectRequest<User> request = new StringJsonObjectRequest<User>(mConfig.USER_GET_URL,
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						ToastUtil.showErrorNet(mContext);
						mDataLoadView.showFailed();
					}
				}, new StringJsonObjectRequest.Listener<User>() {

					@Override
					public void onResponse(ObjectResult<User> result) {

						boolean success = Result.defaultParser(mContext, result, true);
						if (success && result.getData() != null) {
							mUser = result.getData();
							// 如果本地的好友状态不正确，那么就更新本地好友状态
							AttentionUser attentionUser = mUser.getFriends();// 服务器的状态
							boolean changed = FriendHelper.updateFriendRelationship(mLoginUserId, mUser.getUserId(),
									attentionUser);
							if (changed) {
								updateAllCardcastUi();
							}

							mDataLoadView.showSuccess();
							updateUI();
						} else {
							mDataLoadView.showFailed();
						}
					}
				}, User.class, params);
		addDefaultRequest(request);
	}

	private boolean showMenu = false;

	private void updateUI() {
		if (mUser == null) {
			return;
		}
		if (isMyInfo) {
			getSupportActionBar().setTitle(R.string.my_data);
			showMenu = false;
		} else {
			getSupportActionBar().setTitle(R.string.basic_info);
			// 在这里查询出本地好友的状态
			initFriendMoreAction();
		}

		// 设置头像
		AvatarHelper.getInstance().displayAvatar(mUser.getUserId(), mAvatarImg, false);
		// 判断是否有备注名,有就显示
		if(mFriend!=null){
			if(mFriend.getRemarkName()!=null){
				mNameTv.setText(mFriend.getRemarkName());
			}
		}else{
			mNameTv.setText(mUser.getNickName());
			
		}
		mSexTv.setText(mUser.getSex() == 0 ? R.string.sex_woman : R.string.sex_man);
		mBirthdayTv.setText(TimeUtils.sk_time_s_long_2_str(mUser.getBirthday()));
		mCityTv.setText(Area.getProvinceCityString(mUser.getProvinceId(), mUser.getCityId()));

		// ActionBtn 的初始化
		if (isMyInfo) {// 如果是我自己，不显示ActionBtn
			mNextStepBtn.setVisibility(View.GONE);
			mLookLocationBtn.setVisibility(View.GONE);
		} else {
			mNextStepBtn.setVisibility(View.VISIBLE);
			if (mFriend == null) {
				mNextStepBtn.setText(R.string.add_attention);
				mNextStepBtn.setOnClickListener(new AddAttentionListener());
			} else {
				switch (mFriend.getStatus()) {
				case Friend.STATUS_BLACKLIST:// 在黑名单中，显示移除黑名单
					mNextStepBtn.setText(R.string.remove_blacklist);
					mNextStepBtn.setOnClickListener(new RemoveBlacklistListener());
					break;
				case Friend.STATUS_ATTENTION:// 已经是关注了，显示打招呼
					mNextStepBtn.setText(R.string.say_hello);
					mNextStepBtn.setOnClickListener(new SayHelloListener());
					break;
				case Friend.STATUS_FRIEND:// 已经是朋友了，显示发消息
					mNextStepBtn.setText(R.string.send_msg);
					mNextStepBtn.setOnClickListener(new SendMsgListener());
					break;
				default:// 其他（理论上不可能的哈，容错）
					mNextStepBtn.setText(R.string.add_attention);
					mNextStepBtn.setOnClickListener(new AddAttentionListener());
					break;
				}
			}

			mLookLocationBtn.setVisibility(View.VISIBLE);
			mLookLocationBtn.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					double latitude = 0;
					double longitude = 0;
					if (mUser != null && mUser.getLoginLog() != null) {
						latitude = mUser.getLoginLog().getLatitude();
						longitude = mUser.getLoginLog().getLongitude();
					}
					// latitude = 22.534023677879738;
					// longitude = 114.06090214848518;
					if (latitude == 0 || longitude == 0) {
						ToastUtil.showToast(mContext, "该好友未公开位置信息");
					}
					Intent intent = new Intent(mContext, BaiduMapActivity.class);
					intent.putExtra("userName", mUser.getNickName());
					intent.putExtra("latitude", latitude);
					intent.putExtra("longitude", longitude);
					startActivity(intent);
				}
			});
		}
		invalidateOptionsMenu();
	}

	private void initFriendMoreAction() {
		mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mUser.getUserId());// 更新好友的状态
		if (mFriend == null) {// 这个人不是我的好友
			showMenu = false;
		} else {
			showMenu = true;
		}
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
		getSupportActionBar().setTitle(R.string.basic_info);
		mAvatarImg = (ImageView) findViewById(R.id.avatar_img);
		mNameTv = (TextView) findViewById(R.id.name_tv);
		mSexTv = (TextView) findViewById(R.id.sex_tv);
		mBirthdayTv = (TextView) findViewById(R.id.birthday_tv);
		mCityTv = (TextView) findViewById(R.id.city_tv);
		mDataLoadView = (DataLoadView) findViewById(R.id.data_load_view);
		mDataLoadView.setLoadingEvent(new DataLoadView.LoadingEvent() {
			@Override
			public void load() {
				loadOthersInfoFromNet();
			}
		});
		mAvatarImg.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Intent intent = new Intent(PersonalInfoActivity.this,
				// BusinessCircleActivity.class);
				// intent.putExtra(Constant.EXTRA_CIRCLE_TYPE,
				// Constant.CIRCLE_TYPE_PERSONAL_SPACE);
				// intent.putExtra(Constant.EXTRA_USER_ID, mUser.getUserId());
				// intent.putExtra(Constant.EXTRA_NICK_NAME,
				// mUser.getNickName());
				// startActivity(intent);
			}
		});
		mNextStepBtn = (Button) findViewById(R.id.next_step_btn);
		mLookLocationBtn = (Button) findViewById(R.id.look_location_btn);
	}

	// 加关注
	private class AddAttentionListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			doAddAttention();
		}
	}

	// 打招呼
	private class SayHelloListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			doSayHello();
		}
	}

	// 发消息
	private class SendMsgListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			MsgBroadcast.broadcastMsgUiUpdate(BasicInfoActivity.this);
			MsgBroadcast.broadcastMsgNumReset(BasicInfoActivity.this);
			Intent intent = new Intent(mContext, ChatActivity.class);
			intent.putExtra(ChatActivity.FRIEND, mFriend);
			startActivity(intent);
		}
	}

	// 移除黑名单
	private class RemoveBlacklistListener implements View.OnClickListener {
		@Override
		public void onClick(View v) {
			if (mFriend == null || mFriend.getStatus() != Friend.STATUS_BLACKLIST) {
				return;
			}
			removeBlacklist(mFriend);
			mFriend = FriendDao.getInstance().getFriend(mLoginUserId, mUser.getUserId());// 更新好友的状态
		}
	}

	public void doSayHello() {
		final EditText editText = new EditText(this);
		editText.setMaxLines(2);
		editText.setLines(2);
		editText.setHint(R.string.say_hello_dialog_hint);
		editText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(20) });
		editText.setLayoutParams(
				new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.say_hello_dialog_title)
				.setView(editText).setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final String text = editText.getText().toString().trim();
						doSayHello(text);
					}
				}).setNegativeButton(getString(R.string.cancel), null);
		builder.create().show();
	}

	private void doSayHello(String text) {
		if (TextUtils.isEmpty(text)) {
			text = getString(R.string.say_hello_default);
		}
		NewFriendMessage message = NewFriendMessage.createWillSendMessage(MyApplication.getInstance().mLoginUser,
				XmppMessage.TYPE_SAYHELLO, text, mUser);
		NewFriendDao.getInstance().createOrUpdateNewFriend(message);
		mXmppService.sendNewFriendMessage(mUser.getUserId(), message);
		// 提示打招呼成功
		ToastUtil.showToast(this, R.string.say_hello_succ);
	}

	private void doAddAttention() {
		if (mUser == null) {
			return;
		}
		ProgressDialogUtil.show(mProgressDialog);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("toUserId", mUser.getUserId());

		StringJsonObjectRequest<AddAttentionResult> request = new StringJsonObjectRequest<AddAttentionResult>(
				mConfig.FRIENDS_ATTENTION_ADD, new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						ProgressDialogUtil.dismiss(mProgressDialog);
						ToastUtil.showErrorNet(mContext);
					}
				}, new StringJsonObjectRequest.Listener<AddAttentionResult>() {
					@Override
					public void onResponse(ObjectResult<AddAttentionResult> result) {
						boolean success = Result.defaultParser(mContext, result, true);
						if (success && result.getData() != null) {// 接口加关注成功
							if (result.getData().getType() == 1 || result.getData().getType() == 3) {// 单方关注成功或已经是关注的
								// 发送推送消息
								NewFriendMessage message = NewFriendMessage.createWillSendMessage(
										MyApplication.getInstance().mLoginUser, XmppMessage.TYPE_NEWSEE, null, mUser);
								mXmppService.sendNewFriendMessage(mUser.getUserId(), message);
								// 添加为关注
								NewFriendDao.getInstance().ascensionNewFriend(message, Friend.STATUS_ATTENTION);
								FriendHelper.addAttentionExtraOperation(mLoginUserId, mUser.getUserId());

								// 提示加关注成功
								ToastUtil.showToast(mContext, R.string.add_attention_succ);
								// 更新界面
								mNextStepBtn.setText(R.string.say_hello);
								mNextStepBtn.setOnClickListener(new SayHelloListener());
								// 由陌生关系变为关注了,那么右上角更多操作可以显示了
								initFriendMoreAction();
								// 更新名片盒
								updateAllCardcastUi();
								invalidateOptionsMenu();
							} else if (result.getData().getType() == 2 || result.getData().getType() == 4) {// 已经是好友了
								// 发送推送的消息
								NewFriendMessage message = NewFriendMessage.createWillSendMessage(
										MyApplication.getInstance().mLoginUser, XmppMessage.TYPE_FRIEND, null, mUser);
								mXmppService.sendNewFriendMessage(mUser.getUserId(), message);

								// 添加为好友
								NewFriendDao.getInstance().ascensionNewFriend(message, Friend.STATUS_FRIEND);
								FriendHelper.addFriendExtraOperation(mLoginUserId, mUser.getUserId());

								// 提示加好友成功
								ToastUtil.showToast(mContext, R.string.add_friend_succ);
								// 更新界面
								mNextStepBtn.setText(R.string.send_msg);
								mNextStepBtn.setOnClickListener(new SendMsgListener());
								// 由陌生或者关注变为好友了,那么右上角更多操作可以显示了
								initFriendMoreAction();
								// 更新名片盒
								updateAllCardcastUi();
								invalidateOptionsMenu();
							} else if (result.getData().getType() == 5) {
								ToastUtil.showToast(mContext, R.string.add_attention_failed);
							}
						}
						ProgressDialogUtil.dismiss(mProgressDialog);
					}
				}, AddAttentionResult.class, params);
		addDefaultRequest(request);
	}

	@Override
	public void onNewFriendSendStateChange(String toUserId, NewFriendMessage message, int messageState) {
	}

	@Override
	public boolean onNewFriend(NewFriendMessage message) {
		return false;
	}

	private void showRemarkDialog(final Friend friend) {
		final EditText editText = new EditText(this);
		editText.setMaxLines(2);
		editText.setLines(2);
		editText.setText(friend.getShowName());
		editText.setFilters(new InputFilter[] { new InputFilter.LengthFilter(20) });
		editText.setLayoutParams(
				new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.set_remark_name).setView(editText)
				.setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String input = editText.getText().toString();
						if (input.equals(friend.getShowName())) {// 备注名没变
							return;
						}
						if (!StringUtils.isNickName(input)) {// 不符合昵称
							if (input.length() != 0) {
								ToastUtil.showToast(mContext, R.string.remark_name_format_error);
								return;
							} else {// 不符合昵称，因为长度为0，但是可以做备注名操作，操作就是清除备注名
									// 判断之前有没有备注名
								if (TextUtils.isEmpty(friend.getRemarkName())) {// 如果没有备注名，就不需要清除
									return;
								}
							}
						}
						remarkFriend(friend, input);
					}
				}).setNegativeButton(getString(R.string.cancel), null);
		builder.create().show();
	}

	private void remarkFriend(final Friend friend, final String remarkName) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("toUserId", friend.getUserId());
		params.put("remarkName", remarkName);

		ProgressDialogUtil.show(mProgressDialog);
		StringJsonObjectRequest<Void> request = new StringJsonObjectRequest<Void>(mConfig.FRIENDS_REMARK,
				new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						ProgressDialogUtil.dismiss(mProgressDialog);
						ToastUtil.showErrorNet(mContext);
					}
				}, new StringJsonObjectRequest.Listener<Void>() {
					@Override
					public void onResponse(ObjectResult<Void> result) {
						boolean success = Result.defaultParser(mContext, result, true);
						if (success) {
							friend.setRemarkName(remarkName);
							// 更新到数据库
							FriendDao.getInstance().setRemarkName(MyApplication.getInstance().mLoginUser.getUserId(),
									friend.getUserId(), remarkName);

							// 更新界面显示
							// TODO
							// if (TextUtils.isEmpty(remarkName)) {// 清除了备注名
							// mRemarkNamell.setVisibility(View.GONE);
							// } else {
							// mRemarkNamell.setVisibility(View.VISIBLE);
							// mRemarkNameTv.setText(remarkName);
							// }

							updateAllCardcastUi();
							// 改了昵称，通知消息界面更新
							MsgBroadcast.broadcastMsgUiUpdate(mContext);
						}
						ProgressDialogUtil.dismiss(mProgressDialog);
						updateUI();
					}
				}, Void.class, params);
		addDefaultRequest(request);

	}

	/**
	 * 取消关注
	 * 
	 * @param friend
	 */
	private void showCancelAttentionDialog(final Friend friend) {
		if (friend.getStatus() == Friend.STATUS_UNKNOW) {
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.prompt_title)
				.setMessage(R.string.cancel_attention_prompt)
				.setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						deleteFriend(friend, 0);
					}
				}).setNegativeButton(getString(R.string.cancel), null);
		builder.create().show();
	}

	private void showDeleteAllDialog(final Friend friend) {
		if (friend.getStatus() == Friend.STATUS_UNKNOW) {
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.prompt_title)
				.setMessage(R.string.delete_all_prompt)
				.setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						deleteFriend(friend, 1);
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
	private void deleteFriend(final Friend friend, final int type) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("toUserId", friend.getUserId());

		String url = null;
		if (type == 0) {
			url = mConfig.FRIENDS_ATTENTION_DELETE;// 取消关注
		} else {
			url = mConfig.FRIENDS_DELETE;// 删除好友
		}

		ProgressDialogUtil.show(mProgressDialog);
		StringJsonObjectRequest<Void> request = new StringJsonObjectRequest<Void>(url, new ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
				ProgressDialogUtil.dismiss(mProgressDialog);
				ToastUtil.showErrorNet(mContext);
			}
		}, new StringJsonObjectRequest.Listener<Void>() {
			@Override
			public void onResponse(ObjectResult<Void> result) {
				boolean success = Result.defaultParser(mContext, result, true);
				if (success) {
					if (type == 0) {
						ToastUtil.showToast(mContext, R.string.cancel_attention_succ);
						NewFriendMessage message = NewFriendMessage.createWillSendMessage(
								MyApplication.getInstance().mLoginUser, XmppMessage.TYPE_DELSEE, null, friend);
						mXmppService.sendNewFriendMessage(mUser.getUserId(), message);// 解除关注
					} else {
						ToastUtil.showToast(mContext, R.string.delete_all_succ);
						NewFriendMessage message = NewFriendMessage.createWillSendMessage(
								MyApplication.getInstance().mLoginUser, XmppMessage.TYPE_DELALL, null, friend);
						mXmppService.sendNewFriendMessage(mUser.getUserId(), message);// 解除好友
					}

					FriendHelper.removeAttentionOrFriend(mLoginUserId, friend.getUserId());
					updateAllCardcastUi();
					/* 更新本界面 */
					// 1、备注名没有了//TODO
					// mRemarkNamell.setVisibility(View.GONE);
					// 2、mFriend设置为null
					mFriend = null;
					// 右上角没有更多操作
					showMenu = false;
					invalidateOptionsMenu();
					// Action Btn设置为打招呼
					mNextStepBtn.setText(R.string.add_attention);
					mNextStepBtn.setOnClickListener(new AddAttentionListener());
				}
				ProgressDialogUtil.dismiss(mProgressDialog);
			}
		}, Void.class, params);
		addDefaultRequest(request);

	}

	/* 显示加入黑名单的对话框 */
	private void showBlacklistDialog(final Friend friend) {
		int messageId = 0;
		if (friend.getStatus() == Friend.STATUS_BLACKLIST) {// 已经在黑名单，那就是移出黑名单
			messageId = R.string.remove_blacklist_prompt;
		} else if (friend.getStatus() == Friend.STATUS_ATTENTION || friend.getStatus() == Friend.STATUS_FRIEND) {
			messageId = R.string.add_blacklist_prompt;
		} else {// 其他关系（错误的状态）
			return;
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle(R.string.prompt_title)
				.setMessage(messageId)
				.setPositiveButton(getString(R.string.sure), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (friend.getStatus() == Friend.STATUS_BLACKLIST) {// 已经在黑名单，那就是移出黑名单
							removeBlacklist(friend);
						} else if (friend.getStatus() == Friend.STATUS_ATTENTION
								|| friend.getStatus() == Friend.STATUS_FRIEND) {
							addBlacklist(friend);
						}

					}
				}).setNegativeButton(getString(R.string.cancel), null);
		builder.create().show();
	}

	private void addBlacklist(final Friend friend) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("toUserId", friend.getUserId());

		ProgressDialogUtil.show(mProgressDialog);
		StringJsonObjectRequest<Void> request = new StringJsonObjectRequest<Void>(mConfig.FRIENDS_BLACKLIST_ADD,
				new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						ProgressDialogUtil.dismiss(mProgressDialog);
						ToastUtil.showErrorNet(mContext);
					}
				}, new StringJsonObjectRequest.Listener<Void>() {
					@Override
					public void onResponse(ObjectResult<Void> result) {
						boolean success = Result.defaultParser(mContext, result, true);
						if (success) {
							FriendDao.getInstance().updateFriendStatus(friend.getOwnerId(), friend.getUserId(),
									Friend.STATUS_BLACKLIST);
							FriendHelper.addBlacklistExtraOperation(mLoginUserId, friend.getUserId());

							updateAllCardcastUi();

							// Action Btn设置为打招呼
							mNextStepBtn.setText(R.string.remove_blacklist);
							mNextStepBtn.setOnClickListener(new RemoveBlacklistListener());

							/* 发送加入黑名单的通知 */
							if (friend.getStatus() == Friend.STATUS_FRIEND) {// 之前是好友，需要发消息让那个人不能看我的商务圈
								NewFriendMessage message = NewFriendMessage.createWillSendMessage(
										MyApplication.getInstance().mLoginUser, XmppMessage.TYPE_BLACK, null, friend);
								mXmppService.sendNewFriendMessage(friend.getUserId(), message);// 加入黑名单
							}

							friend.setStatus(Friend.STATUS_BLACKLIST);
							ToastUtil.showToast(mContext, R.string.add_blacklist_succ);
						}
						ProgressDialogUtil.dismiss(mProgressDialog);
					}
				}, Void.class, params);
		addDefaultRequest(request);

	}

	private void removeBlacklist(final Friend friend) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("toUserId", friend.getUserId());

		ProgressDialogUtil.show(mProgressDialog);
		StringJsonObjectRequest<AttentionUser> request = new StringJsonObjectRequest<AttentionUser>(
				mConfig.FRIENDS_BLACKLIST_DELETE, new ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						ProgressDialogUtil.dismiss(mProgressDialog);
						ToastUtil.showErrorNet(mContext);
					}
				}, new StringJsonObjectRequest.Listener<AttentionUser>() {
					@Override
					public void onResponse(ObjectResult<AttentionUser> result) {
						boolean success = Result.defaultParser(mContext, result, true);
						if (success) {
							int currentStatus = Friend.STATUS_UNKNOW;
							if (result.getData() != null) {
								currentStatus = result.getData().getStatus();
							}
							FriendDao.getInstance().updateFriendStatus(friend.getOwnerId(), friend.getUserId(),
									currentStatus);
							friend.setStatus(currentStatus);
							updateAllCardcastUi();

							switch (currentStatus) {
							case Friend.STATUS_ATTENTION:
								mNextStepBtn.setText(R.string.say_hello);
								mNextStepBtn.setOnClickListener(new SayHelloListener());
								NewFriendMessage message1 = NewFriendMessage.createWillSendMessage(
										MyApplication.getInstance().mLoginUser, XmppMessage.TYPE_NEWSEE, null, friend);
								mXmppService.sendNewFriendMessage(friend.getUserId(), message1);

								FriendHelper.addAttentionExtraOperation(friend.getOwnerId(), friend.getUserId());
								break;
							case Friend.STATUS_FRIEND:
								mNextStepBtn.setText(R.string.send_msg);
								mNextStepBtn.setOnClickListener(new SendMsgListener());

								NewFriendMessage message2 = NewFriendMessage.createWillSendMessage(
										MyApplication.getInstance().mLoginUser, XmppMessage.TYPE_FRIEND, null, mUser);
								mXmppService.sendNewFriendMessage(mUser.getUserId(), message2);
								FriendHelper.addFriendExtraOperation(friend.getOwnerId(), friend.getUserId());
								break;
							default:// 其他，理论上不可能
								mNextStepBtn.setText(R.string.add_attention);
								mNextStepBtn.setOnClickListener(new AddAttentionListener());
								break;
							}

							ToastUtil.showToast(mContext, R.string.remove_blacklist_succ);
						}
						ProgressDialogUtil.dismiss(mProgressDialog);
					}
				}, AttentionUser.class, params);
		addDefaultRequest(request);
	}

}
