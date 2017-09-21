package com.sk.weichat.ui.groupchat;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.Area;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.MucRoomSimple;
import com.sk.weichat.bean.message.MucRoom;
import com.sk.weichat.broadcast.CardcastUiUpdateUtil;
import com.sk.weichat.broadcast.MucgroupUpdateUtil;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.ui.base.BaseActivity;
import com.sk.weichat.ui.message.MucChatActivity;
import com.sk.weichat.util.Constants;
import com.sk.weichat.util.DisplayUtil;
import com.sk.weichat.util.ProgressDialogUtil;
import com.sk.weichat.util.TimeUtils;
import com.sk.weichat.util.ToastUtil;
import com.sk.weichat.util.ViewHolder;
import com.sk.weichat.view.HorizontalListView;
import com.sk.weichat.volley.ObjectResult;
import com.sk.weichat.volley.Result;
import com.sk.weichat.volley.StringJsonObjectRequest;
import com.sk.weichat.xmpp.CoreService;
import com.sk.weichat.xmpp.CoreService.CoreServiceBinder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * @项目名称: SkWeiChat-Baidu
 * @包名: com.sk.weichat.ui.groupchat
 * @作者:王阳
 * @创建时间: 2015年10月16日 上午11:29:13
 * @描述: 选择联系人创建群的界面
 * @SVN版本号: $Rev$
 * @修改人: $Author$
 * @修改时间: $Date$
 * @修改的内容: 增加房间名和房间描述的字数限制
 */
public class SelectContactsActivity extends BaseActivity {

	private final int LAST_ICON = -1;

	private ListView mListView;
	private HorizontalListView mHorizontalListView;
	private Button mOkBtn;

	private List<Friend> mFriendList;
	private ListViewAdapter mAdapter;
	private List<Integer> mSelectPositions;
	private HorListViewAdapter mHorAdapter;
	public ProgressDialog mProgressDialog;

	private String mLoginUserId;
	private boolean mBind;
	private CoreService mXmppService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_contacts);
		mFriendList = new ArrayList<Friend>();
		mAdapter = new ListViewAdapter();
		mSelectPositions = new ArrayList<Integer>();
		mSelectPositions.add(LAST_ICON);// 增加一个虚线框的位置
		mHorAdapter = new HorListViewAdapter();
		mLoginUserId = MyApplication.getInstance().mLoginUser.getUserId();
		initView();

		mBind = bindService(CoreService.getIntent(), mServiceConnection, BIND_AUTO_CREATE);
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mBind) {
			unbindService(mServiceConnection);
		}
	}

	private void initView() {
		getSupportActionBar().setTitle(R.string.select_contacts);
		mListView = (ListView) findViewById(R.id.list_view);
		mHorizontalListView = (HorizontalListView) findViewById(R.id.horizontal_list_view);
		mOkBtn = (Button) findViewById(R.id.ok_btn);
		mListView.setAdapter(mAdapter);
		mHorizontalListView.setAdapter(mHorAdapter);
		mOkBtn.setText(getString(R.string.add_chat_ok_btn, mSelectPositions.size() - 1));

		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				if (hasSelected(position)) {
					removeSelect(position);
				} else {
					addSelect(position);
				}
			}
		});

		mHorizontalListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				if (position == mSelectPositions.size() - 1) {
					return;
				}
				mSelectPositions.remove(position);
				mAdapter.notifyDataSetInvalidated();
				mHorAdapter.notifyDataSetInvalidated();
				mOkBtn.setText(getString(R.string.add_chat_ok_btn, mSelectPositions.size() - 1));
			}
		});

		mOkBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				showCreateGroupChatDialog();
			}
		});

		mProgressDialog = ProgressDialogUtil.init(mContext, null, getString(R.string.please_wait), false, false);
	}

	@Override
	public void onResume() {
		super.onResume();
		loadData();
	}

	private void loadData() {
		List<Friend> userInfos = FriendDao.getInstance().getAllContacts(mLoginUserId);
		if (userInfos != null) {
			mFriendList.clear();
			mFriendList.addAll(userInfos);
			mAdapter.notifyDataSetChanged();
		}
	}

	private void addSelect(int position) {
		if (!hasSelected(position)) {
			mSelectPositions.add(0, position);
			mAdapter.notifyDataSetInvalidated();
			mHorAdapter.notifyDataSetInvalidated();
			mOkBtn.setText(getString(R.string.add_chat_ok_btn, mSelectPositions.size() - 1));
		}
	}

	private boolean hasSelected(int position) {
		for (int i = 0; i < mSelectPositions.size(); i++) {
			if (mSelectPositions.get(i) == position) {
				return true;
			} else if (i == mSelectPositions.size() - 1) {
				return false;
			}
		}
		return false;
	}

	private void removeSelect(int position) {
		mSelectPositions.remove(Integer.valueOf(position));
		mAdapter.notifyDataSetInvalidated();
		mHorAdapter.notifyDataSetInvalidated();
		mOkBtn.setText(getString(R.string.add_chat_ok_btn, mSelectPositions.size() - 1));
	}




	private class ListViewAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return mFriendList.size();
		}

		@Override
		public Object getItem(int position) {
			return mFriendList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(mContext).inflate(R.layout.row_select_contacts, parent, false);
			}
			ImageView avatarImg = ViewHolder.get(convertView, R.id.avatar_img);
			TextView userNameTv = ViewHolder.get(convertView, R.id.user_name_tv);
			CheckBox checkBox = ViewHolder.get(convertView, R.id.check_box);
			AvatarHelper.getInstance().displayAvatar(mFriendList.get(position).getUserId(), avatarImg, true);
			userNameTv.setText(mFriendList.get(position).getNickName());
			checkBox.setChecked(false);
			if (mSelectPositions.contains(Integer.valueOf(position))) {
				checkBox.setChecked(true);
			}
			return convertView;
		}

	}

	private class HorListViewAdapter extends BaseAdapter {
		@Override
		public int getCount() {
			return mSelectPositions.size();
		}

		@Override
		public Object getItem(int position) {
			return mSelectPositions.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = new ImageView(mContext);
				int size = DisplayUtil.dip2px(mContext, 37);
				AbsListView.LayoutParams param = new AbsListView.LayoutParams(size, size);
				convertView.setLayoutParams(param);
			}
			ImageView imageView = (ImageView) convertView;
			int selectPosition = mSelectPositions.get(position);
			if (selectPosition == -1) {
				imageView.setImageResource(R.drawable.dot_avatar);
			} else {
				if (selectPosition >= 0 && selectPosition < mFriendList.size()) {
					AvatarHelper.getInstance().displayAvatar(mFriendList.get(selectPosition).getUserId(), imageView,
							true);
				}
			}
			return convertView;
		}
	}

	
	private void showCreateGroupChatDialog() {
		if (mXmppService == null || !mXmppService.isMucEnable()) {
			ToastUtil.showToast(mContext, R.string.service_start_failed);
			return;
		}
		View rootView = LayoutInflater.from(mContext).inflate(R.layout.dialog_create_muc_room, null);
		final EditText roomNameEdit = (EditText) rootView.findViewById(R.id.room_name_edit);
		// final EditText roomSubjectEdit = (EditText)
		// rootView.findViewById(R.id.room_subject_edit);
		final EditText roomDescEdit = (EditText) rootView.findViewById(R.id.room_desc_edit);
		final Button sure_btn = (Button) rootView.findViewById(R.id.sure_btn);

		ToastUtil.addEditTextNumChanged(SelectContactsActivity.this,roomNameEdit, 8);// 设置EditText的字数限制
		ToastUtil.addEditTextNumChanged(SelectContactsActivity.this,roomDescEdit, 20);

		final AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.create_room).setView(rootView)
				.create();
		sure_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String roomName = roomNameEdit.getText().toString().trim();//防止空,或者输入空格
				if (TextUtils.isEmpty(roomName)) {
					ToastUtil.showToast(mContext, R.string.room_name_empty_error);
					return;
				}
				// String roomSubject = roomSubjectEdit.getText().toString();
				String roomDesc = roomDescEdit.getText().toString();
				if (TextUtils.isEmpty(roomName)) {
					ToastUtil.showToast(mContext, R.string.room_des_empty_error);
					return;
				}
				createGroupChat(roomName, null, roomDesc);
				dialog.dismiss();
			}
		});
		dialog.show();
	}

	private void createGroupChat(final String roomName, String roomSubject, final String roomDesc) {
		String nickName = MyApplication.getInstance().mLoginUser.getNickName();
		final String roomJid = mXmppService.createMucRoom(nickName, roomName, roomSubject, roomDesc);
		if (TextUtils.isEmpty(roomJid)) {
			ToastUtil.showToast(mContext, R.string.create_room_failed);
			return;
		}

		Map<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("jid", roomJid);
		params.put("name", roomName);
		params.put("desc", roomDesc);
		params.put("countryId", String.valueOf(Area.getDefaultCountyId()));// 国家Id

		Area area = Area.getDefaultProvince();
		if (area != null) {
			params.put("provinceId", String.valueOf(area.getId()));// 省份Id
		}
		area = Area.getDefaultCity();
		if (area != null) {
			params.put("cityId", String.valueOf(area.getId()));// 城市Id
			area = Area.getDefaultDistrict(area.getId());
			if (area != null) {
				params.put("areaId", String.valueOf(area.getId()));// 城市Id
			}
		}

		double latitude = MyApplication.getInstance().getBdLocationHelper().getLatitude();
		double longitude = MyApplication.getInstance().getBdLocationHelper().getLongitude();
		if (latitude != 0)
			params.put("latitude", String.valueOf(latitude));
		if (longitude != 0)
			params.put("longitude", String.valueOf(longitude));

		List<String> inviteUsers = new ArrayList<String>();
		// 邀请好友
		for (int i = 0; i < mSelectPositions.size(); i++) {
			if (mSelectPositions.get(i) == -1) {
				continue;
			}
			String userId = mFriendList.get(mSelectPositions.get(i)).getUserId();
			inviteUsers.add(userId);
		}
		params.put("text", JSON.toJSONString(inviteUsers));

		ProgressDialogUtil.show(mProgressDialog);
		StringJsonObjectRequest<MucRoom> request = new StringJsonObjectRequest<MucRoom>(mConfig.ROOM_ADD,
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError arg0) {
						ProgressDialogUtil.dismiss(mProgressDialog);
						ToastUtil.showErrorNet(mContext);
					}
				}, new StringJsonObjectRequest.Listener<MucRoom>() {
					@Override
					public void onResponse(ObjectResult<MucRoom> result) {
						boolean parserResult = Result.defaultParser(mContext, result, true);
						if (parserResult && result.getData() != null) {
							createRoomSuccess(result.getData().getId(), roomJid, roomName, roomDesc);
						}
						ProgressDialogUtil.dismiss(mProgressDialog);
					}
				}, MucRoom.class, params);
		addDefaultRequest(request);
	}

	private void createRoomSuccess(String roomId, String roomJid, String roomName, String roomDesc) {
		Friend friend = new Friend();// 将房间也存为好友
		friend.setOwnerId(mLoginUserId);
		friend.setUserId(roomJid);
		friend.setNickName(roomName);
		friend.setDescription(roomDesc);
		friend.setRoomFlag(1);
		friend.setRoomId(roomId);
		friend.setRoomCreateUserId(mLoginUserId);
		// timeSend作为取群聊离线消息的标志，所以要在这里设置一个初始值
		friend.setTimeSend(TimeUtils.sk_time_current_time());
		friend.setStatus(Friend.STATUS_FRIEND);
		FriendDao.getInstance().createOrUpdateFriend(friend);
		// 更新名片盒（可能需要更新）
		CardcastUiUpdateUtil.broadcastUpdateUi(this);
		// 更新群聊界面
		MucgroupUpdateUtil.broadcastUpdateUi(this);

		MucRoomSimple mucRoomSimple = new MucRoomSimple();
		mucRoomSimple.setId(roomId);
		mucRoomSimple.setJid(roomJid);
		mucRoomSimple.setName(roomName);
		mucRoomSimple.setDesc(roomDesc);
		mucRoomSimple.setUserId(mLoginUserId);
		mucRoomSimple.setTimeSend(TimeUtils.sk_time_current_time());
		String reason = JSON.toJSONString(mucRoomSimple);
		Log.d("roamer", "reason:" + reason);
		// 邀请好友
		String[] noticeFriendList=new String[mSelectPositions.size()];
		for (int i = 0; i < mSelectPositions.size(); i++) {
			if (mSelectPositions.get(i) == -1) {
				continue;
			}
			String firendUserId = mFriendList.get(mSelectPositions.get(i)).getUserId();
			noticeFriendList[i]=firendUserId;
			mXmppService.invite(roomJid, firendUserId, reason);
		}

		Intent intent = new Intent(this, MucChatActivity.class);
		intent.putExtra(AppConstant.EXTRA_USER_ID, roomJid);
		intent.putExtra(AppConstant.EXTRA_NICK_NAME, roomName);
		intent.putExtra(AppConstant.EXTRA_IS_GROUP_CHAT, true);
		intent.putExtra(Constants.GROUP_JOIN_NOTICE,noticeFriendList);
		startActivity(intent);
		finish();
	}
}
