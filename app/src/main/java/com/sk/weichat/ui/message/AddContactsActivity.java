package com.sk.weichat.ui.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.sk.weichat.AppConstant;
import com.sk.weichat.MyApplication;
import com.sk.weichat.R;
import com.sk.weichat.bean.Friend;
import com.sk.weichat.bean.MucRoomSimple;
import com.sk.weichat.db.dao.FriendDao;
import com.sk.weichat.helper.AvatarHelper;
import com.sk.weichat.ui.base.BaseActivity;
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

public class AddContactsActivity extends BaseActivity {

	private final int LAST_ICON = -1;

	private ListView mListView;
	private HorizontalListView mHorizontalListView;
	private Button mOkBtn;

	private List<Friend> mFriendList;
	private ListViewAdapter mAdapter;
	private List<Integer> mSelectPositions;
	private HorListViewAdapter mHorAdapter;

	private String mRoomId;
	private String mRoomJid;
	private String mRoomDes;
	private String mRoomName;
	private List<String> mExistIds;

	private String mLoginUserId;

	private boolean mXmppBind;
	private CoreService mCoreService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_contacts);
		if (getIntent() != null) {
			mRoomId = getIntent().getStringExtra("roomId");
			mRoomJid = getIntent().getStringExtra("roomJid");
			mRoomDes = getIntent().getStringExtra("roomDes");
			mRoomName = getIntent().getStringExtra("roomName");
			String ids = getIntent().getStringExtra("exist_ids");
			mExistIds = JSON.parseArray(ids, String.class);
		}

		mFriendList = new ArrayList<Friend>();
		mAdapter = new ListViewAdapter();
		mSelectPositions = new ArrayList<Integer>();
		mSelectPositions.add(LAST_ICON);// 增加一个虚线框的位置
		mHorAdapter = new HorListViewAdapter();

		mLoginUserId = MyApplication.getInstance().mLoginUser.getUserId();
		initView();
		// 绑定服务
		mXmppBind = bindService(CoreService.getIntent(), mXmppServiceConnection, BIND_AUTO_CREATE);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (mXmppBind) {
			unbindService(mXmppServiceConnection);
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
		}
	};

	@Override
	public void onResume() {
		super.onResume();
		loadData();
	}

	private void loadData() {
		List<Friend> userInfos = FriendDao.getInstance().getAllContacts(mLoginUserId);
		if (userInfos != null) {
			mFriendList.clear();
			for (int i = 0; i < userInfos.size(); i++) {
				boolean isIn = isExist(userInfos.get(i));
				if (isIn) {
					userInfos.remove(i);
					i--;
				} else {
					mFriendList.add(userInfos.get(i));
				}
			}
			mAdapter.notifyDataSetChanged();
		}
	}

	/**
	 * 是否存在已经在那个房间的好友
	 * 
	 * @param friendEntity
	 * @return
	 */
	private boolean isExist(Friend friend) {
		for (int i = 0; i < mExistIds.size(); i++) {
			if (mExistIds.get(i) == null) {
				continue;
			}
			if (friend.getUserId().equals(mExistIds.get(i))) {
				return true;
			}
		}
		return false;
	}

	private void initView() {
		getSupportActionBar().setTitle("选择联系人");
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
				inviteFriend();
			}
		});

		mProgressDialog = ProgressDialogUtil.init(mContext, null, getString(R.string.please_wait));
	}

	public ProgressDialog mProgressDialog;

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
					AvatarHelper.getInstance().displayAvatar(mFriendList.get(selectPosition).getUserId(), imageView, true);
				}
			}
			return convertView;

		}

	}

	/**
	 * 邀请好友
	 */
	private void inviteFriend() {
		if (mSelectPositions.size() <= 1) {
			finish();
			return;
		}
		Map<String, String> params = new HashMap<String, String>();
		params.put("access_token", MyApplication.getInstance().mAccessToken);
		params.put("roomId", mRoomId);

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
		StringJsonObjectRequest<Void> request = new StringJsonObjectRequest<Void>(mConfig.ROOM_MEMBER_UPDATE, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError arg0) {
				ProgressDialogUtil.dismiss(mProgressDialog);
				ToastUtil.showErrorNet(mContext);
			}
		}, new StringJsonObjectRequest.Listener<Void>() {
			@Override
			public void onResponse(ObjectResult<Void> result) {
				boolean parserResult = Result.defaultParser(mContext, result, true);
				if (parserResult) {
					inviteFriendSuccess();
				}
				ProgressDialogUtil.dismiss(mProgressDialog);
			}
		}, Void.class, params);
		addDefaultRequest(request);
	}

	private void inviteFriendSuccess() {
		MucRoomSimple mucRoomSimple = new MucRoomSimple();
		mucRoomSimple.setId(mRoomId);
		mucRoomSimple.setJid(mRoomJid);
		mucRoomSimple.setName(mRoomName);
		mucRoomSimple.setDesc(mRoomDes);
		mucRoomSimple.setUserId(mLoginUserId);
		mucRoomSimple.setTimeSend(TimeUtils.sk_time_current_time());
		String reason = JSON.toJSONString(mucRoomSimple);
		// 邀请好友
		for (int i = 0; i < mSelectPositions.size(); i++) {
			if (mSelectPositions.get(i) == -1) {
				continue;
			}
			String firendUserId = mFriendList.get(mSelectPositions.get(i)).getUserId();

			mCoreService.invite(mRoomJid, firendUserId, reason);
			/*Intent broadcast=new Intent(Constants.CHAT_MESSAGE_DELETE_ACTION);
			broadcast.putExtra(Constants.GROUP_JOIN_NOTICE_FRIEND_ID,firendUserId);
			broadcast.putExtra(AppConstant.EXTRA_USER_ID, mRoomJid);
			this.sendBroadcast(broadcast);*/
		}
		setResult(RESULT_OK);

		finish();
	}

}
