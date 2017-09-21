package com.lsjr.zizi.mvp.home.session;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.andview.adapter.ABaseRefreshAdapter;
import com.andview.adapter.BaseRecyclerHolder;
import com.andview.listener.OnItemClickListener;
import com.lsjr.bean.ObjectResult;
import com.lsjr.callback.ChatObjectCallBack;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.mvp.home.Constants;
import com.lsjr.zizi.chat.bean.MucRoom;
import com.lsjr.zizi.chat.bean.MucRoomSimple;
import com.lsjr.zizi.chat.bean.ResultCode;
import com.lsjr.zizi.chat.broad.CardcastUiUpdateUtil;
import com.lsjr.zizi.chat.broad.MucgroupUpdateUtil;
import com.lsjr.zizi.chat.dao.FriendDao;
import com.lsjr.zizi.chat.db.Area;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.loader.AvatarHelper;
import com.lsjr.zizi.chat.xmpp.CoreService;
import com.lsjr.zizi.mvp.listener.MyEditTextChangeListener;
import com.lsjr.zizi.util.TimeUtils;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;
import com.ymz.baselibrary.utils.UIUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/19 17:29
 */

@SuppressLint("Registered")
public class CreateGroupActivity extends MvpActivity {


    @BindView(R.id.id_rv_hv_seletor)
    RecyclerView idRvHvSeletor;
    @BindView(R.id.id_vt_contact)
    RecyclerView idVtContact;
    @BindView(R.id.ll_none)
    LinearLayout llNone;
    private CoreService mXmppService;
    private boolean mBind;
    private String mLoginUserId;
    private List<String> mSelectPositions;
    private List<Friend> mFriendList;
    private ContactAdapter adapter;

    private Boolean isAddUser;
    private String mRoomId;
    private String mRoomJid;
    private String mRoomDes;
    private String mRoomName;
    private List<String> mExistIds;

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_create_group;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        mFriendList = new ArrayList<>();
        mSelectPositions = new ArrayList<>();
        mLoginUserId = ConfigApplication.instance().mLoginUser.getUserId();
        if (getIntent() != null) {
            mRoomId = getIntent().getStringExtra("roomId");
            mRoomJid = getIntent().getStringExtra("roomJid");
            mRoomDes = getIntent().getStringExtra("roomDes");
            mRoomName = getIntent().getStringExtra("roomName");
            isAddUser = getIntent().getBooleanExtra(Constants.ISADD_USER, false);
            String ids = getIntent().getStringExtra("exist_ids");
            mExistIds = JSON.parseArray(ids, String.class);
            if (mExistIds != null) {
                for (int i = 0; i < mExistIds.size(); i++) {
                    L_.e("onCreate" + mExistIds.get(i));
                }
            }

        }
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        mBind = bindService(CoreService.getIntent(), mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void initView() {
        if (!isAddUser) {
            setTitleText("发起群聊");

        } else {
            setTitleText("选择好友");
        }
        getToolBarView().getRightTextView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSelectPositions.size() > 0) {
                    if (isAddUser) {
                        addFriendforGroup();
                    } else {
                        showCreateGroupChatDialog();
                    }
                } else {
                    T_.showToastReal("请选择好友");
                }
            }
        });
        setTopLeftButton(R.drawable.ic_back).
                setTitleTextColor(UIUtils.getColor(R.color.white)).
                setBackgroundColor(UIUtils.getColor(R.color.colorPrimary));
        adapter = new ContactAdapter(UIUtils.getContext(), mFriendList, R.layout.itme_contact_seletor);
        idVtContact.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return true;
            }
        });

        idVtContact.setAdapter(adapter);
        adapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(BaseRecyclerHolder baseRecyclerHolder, int position, Object item) {
                //  T_.showToastReal("选择好友");
                CheckBox checkBox = baseRecyclerHolder.getView(R.id.check_box);
                if (checkBox.isChecked()) {
                    if (mSelectPositions.size() > 0)
                        for (int i = 0; i < mSelectPositions.size(); i++) {
                            if (mSelectPositions.get(i).equals(mFriendList.get(position).getUserId())) {
                                mSelectPositions.remove(i);
                            }
                        }
                } else {
                    mSelectPositions.add(mFriendList.get(position).getUserId());
                }
                if (mSelectPositions.size() > 0) {
                    UIUtils.runInMainThread(new Runnable() {
                        @Override
                        public void run() {
                            getToolBarView().setRightText("确定" + (mSelectPositions.size()));
                        }
                    });
                } else {
                    getToolBarView().getRightTextView().setVisibility(View.GONE);
                }
                adapter.notifyDataSetChanged();
            }
        });


        if (!isAddUser) {
            loadData();
        } else {
            addUserloadData();
        }

    }

    private void addUserloadData() {
        List<Friend> userInfos = FriendDao.getInstance().getAllContacts(mLoginUserId);
        L_.e("增加好友:" + userInfos.size());
        if (userInfos != null) {
            mFriendList.clear();
            //mFriendList.addAll(userInfos);
            for (int i = 0; i < userInfos.size(); i++) {
                boolean isIn = isExist(userInfos.get(i));
                if (isIn) {
                    //mSelectPositions.add(userInfos.get(i).getUserId());
                    userInfos.remove(i);
                } else {
                    mFriendList.add(userInfos.get(i));
                    //mSelectPositions.remove(userInfos.get(i).getUserId());
                }
            }
            if (mFriendList.size() == 0) {
                //showEmptyView();
                idVtContact.setVisibility(View.GONE);
                llNone.setVisibility(View.VISIBLE);
                return;
            }else {
                idVtContact.setVisibility(View.VISIBLE);
                llNone.setVisibility(View.GONE);
            }
            L_.e("被选中的个数" + mSelectPositions.size());
            adapter.notifyDataSetChanged();
        }
    }

    /**
     * 是否存在已经在那个房间的好友
     *
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


    private void showCreateGroupChatDialog() {
        if (mXmppService == null || !mXmppService.isMucEnable()) {
            T_.showToastReal("服务失败");
            return;
        }
        View rootView = LayoutInflater.from(UIUtils.getContext()).inflate(R.layout.dialog_create_group_room, null);
        final EditText roomNameEdit = (EditText) rootView.findViewById(R.id.room_name_edit);
        final EditText roomDescEdit = (EditText) rootView.findViewById(R.id.room_desc_edit);
        final Button sure_btn = (Button) rootView.findViewById(R.id.sure_btn);
        UIUtils.addEditTextNumChanged(roomNameEdit, 8);// 设置EditText的字数限制
        UIUtils.addEditTextNumChanged(roomDescEdit, 20);
        final AlertDialog dialog = new AlertDialog.Builder(this).setTitle("创建房间").setView(rootView)
                .create();
        roomNameEdit.addTextChangedListener(new MyEditTextChangeListener() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() != 0 && s.toString() != null) {
                    sure_btn.setEnabled(true);
                } else {
                    sure_btn.setEnabled(false);
                }
            }
        });
        sure_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String roomName = roomNameEdit.getText().toString().trim();//防止空,或者输入空格
                if (TextUtils.isEmpty(roomName)) {
                    T_.showToastReal("请输入文字");
                    return;
                }
                String roomDesc = roomDescEdit.getText().toString();
                if (TextUtils.isEmpty(roomName)) {
                    T_.showToastReal("请输入文字");
                    sure_btn.setEnabled(true);
                    return;
                }
                if (!isAddUser) {
                    createGroupChat(roomName, null, roomDesc);
                } else {

                }
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        //  loadData();
    }

    private void loadData() {
        List<Friend> userInfos = FriendDao.getInstance().getAllContacts(mLoginUserId);
        if (userInfos != null) {
            mFriendList.clear();
            mFriendList.addAll(userInfos);
            adapter.notifyDataSetChanged();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBind) {
            unbindService(mServiceConnection);
        }
    }


    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mXmppService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mXmppService = ((CoreService.CoreServiceBinder) service).getService();
        }
    };


    private void addFriendforGroup() {
        if (mSelectPositions.size() < 1) {
            L_.e(mSelectPositions.get(0));
            finish();
            return;
        }
        Map<String, String> params = new HashMap<String, String>();
        params.put("access_token", ConfigApplication.instance().mAccessToken);
        params.put("roomId", mRoomId);
        List<String> inviteUsers = new ArrayList<String>();
        // 邀请好友
        for (int i = 0; i < mSelectPositions.size(); i++) {
            if (mSelectPositions.get(i) == null) {
                continue;
            }
            String userId = mSelectPositions.get(i);
            inviteUsers.add(userId);
        }
        params.put("text", JSON.toJSONString(inviteUsers));

        //showProgressDialogWithText("请等待");
        HttpUtils.getInstance().postServiceData(AppConfig.ROOM_MEMBER_UPDATE, params, new ChatObjectCallBack<Void>(Void.class) {

            @Override
            protected void onXError(String exception) {
                // dismissProgressDialog();
                T_.showToastReal(exception);
            }

            @Override
            protected void onSuccess(ObjectResult<Void> result) {
                boolean parserResult = ResultCode.defaultParser(result, true);
                if (parserResult) {
                    inviteFriendSuccess();
                }
            }
        });

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
            if (mSelectPositions.get(i) == null) {
                continue;
            }
            String firendUserId = mSelectPositions.get(i);
            mXmppService.invite(mRoomJid, firendUserId, reason);
            /*Intent broadcast=new Intent(Constants.CHAT_MESSAGE_DELETE_ACTION);
			broadcast.putExtra(Constants.GROUP_JOIN_NOTICE_FRIEND_ID,firendUserId);
			broadcast.putExtra(AppConstant.EXTRA_USER_ID, mRoomJid);
			this.sendBroadcast(broadcast);*/
            L_.e("添加成功" + firendUserId);
        }
        setResult(RESULT_OK);
        finish();
    }


    private void createGroupChat(final String roomName, String roomSubject, final String roomDesc) {
        String nickName = ConfigApplication.instance().mLoginUser.getNickName();
        final String roomJid = mXmppService.createMucRoom(nickName, roomName, roomSubject, roomDesc);
        if (TextUtils.isEmpty(roomJid)) {
            return;
        }
        Map<String, String> params = new HashMap<String, String>();
        params.put("access_token", ConfigApplication.instance().mAccessToken);
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
        double latitude = 100;
        double longitude = 100;
        if (latitude != 0)
            params.put("latitude", String.valueOf(latitude));
        if (longitude != 0)
            params.put("longitude", String.valueOf(longitude));

        List<String> inviteUsers = new ArrayList<String>();
        // 邀请好友
        for (int i = 0; i < mSelectPositions.size(); i++) {
            if (mSelectPositions.get(i) == null) {
                continue;
            }
            String userId = mSelectPositions.get(i);
            inviteUsers.add(userId);
        }
        params.put("text", JSON.toJSONString(inviteUsers));
        showProgressDialogWithText("请等待");
        HttpUtils.getInstance().postServiceData(AppConfig.ROOM_ADD, params, new ChatObjectCallBack<MucRoom>(MucRoom.class) {

            @Override
            protected void onXError(String exception) {
                dismissProgressDialog();
                T_.showToastReal(exception);
            }

            @Override
            protected void onSuccess(ObjectResult<MucRoom> result) {
                dismissProgressDialog();
                boolean parserResult = ResultCode.defaultParser(result, true);
                if (parserResult && result.getData() != null) {
                    createRoomSuccess(result.getData().getId(), roomJid, roomName, roomDesc);
                }
            }
        });


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
        String[] noticeFriendList = new String[mSelectPositions.size()];
        for (int i = 0; i < mSelectPositions.size(); i++) {
            if (mSelectPositions.get(i) == null) {
                continue;
            }
            String firendUserId = mSelectPositions.get(i);
            noticeFriendList[i] = firendUserId;
            mXmppService.invite(roomJid, firendUserId, reason);
        }
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(Constants.EXTRA_USER_ID, roomJid);
        intent.putExtra(Constants.EXTRA_NICK_NAME, roomName);
        intent.putExtra(Constants.EXTRA_IS_GROUP_CHAT, true);
        intent.putExtra(Constants.GROUP_JOIN_NOTICE, noticeFriendList);
        startActivity(intent);
        finish();
    }

    private class ContactAdapter extends ABaseRefreshAdapter<Friend> {

        ContactAdapter(Context context, List<Friend> datas, int itemLayoutId) {
            super(context, datas, itemLayoutId);
            L_.e("ContactAdapter   个数：" + datas.size());
        }

        @Override
        protected void convert(BaseRecyclerHolder viewHolder, Friend friend, int position) {
            L_.e("ContactAdapter   position 个数：" + position);
            // 设置头像
            RelativeLayout layoutRoot = viewHolder.getView(R.id.id_contacts_root);
            ImageView avatar_img = viewHolder.getView(R.id.avatar_img);
            TextView nick_name_tv = viewHolder.getView(R.id.nick_name_tv);
            CheckBox checkBox = viewHolder.getView(R.id.check_box);
            switch (friend.getUserId()) {
                case Friend.ID_SYSTEM_MESSAGE: // 系统消息的头像
                    avatar_img.setImageResource(R.drawable.im_notice);
                    break;
                case Friend.ID_NEW_FRIEND_MESSAGE: // 新朋友的头像
                    avatar_img.setImageResource(R.drawable.im_new_friends);
                    break;
                default: // 其他
                    AvatarHelper.getInstance().displayAvatar(friend, avatar_img, true);
                    break;
            }
            // 昵称
            String name = friend.getRemarkName();
            if (TextUtils.isEmpty(name)) {
                name = friend.getNickName();
            }
            nick_name_tv.setText(name);
            checkBox.setChecked(false);
            if (mSelectPositions.contains(friend.getUserId())) {
                checkBox.setChecked(true);
            }

        }
    }

}
