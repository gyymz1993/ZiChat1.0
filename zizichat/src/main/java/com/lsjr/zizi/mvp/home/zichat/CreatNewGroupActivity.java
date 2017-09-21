package com.lsjr.zizi.mvp.home.zichat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.andview.myrvview.LQRRecyclerView;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.chat.bean.MucRoomSimple;
import com.lsjr.zizi.chat.broad.CardcastUiUpdateUtil;
import com.lsjr.zizi.chat.broad.MucgroupUpdateUtil;
import com.lsjr.zizi.chat.dao.FriendDao;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.chat.xmpp.CoreService;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.mvp.home.Constants;
import com.lsjr.zizi.mvp.home.session.ChatActivity;
import com.lsjr.zizi.mvp.home.zichat.presenter.CreateNewGroup;
import com.lsjr.zizi.mvp.listener.MyEditTextChangeListener;
import com.lsjr.zizi.util.TimeUtils;
import com.lsjr.zizi.view.QuickIndexBar;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;
import com.ymz.baselibrary.utils.UIUtils;
import com.ymz.baselibrary.widget.NavigationBarView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/15 14:36
 */

@SuppressLint("Registered")
public class CreatNewGroupActivity extends MvpActivity<CreateNewGroup.CreateGroupPresenter> implements CreateNewGroup.ICreateGroupView {

   // public ArrayList<String> mSelectedTeamMemberAccounts;
    @BindView(R.id.rvSelectedContacts)
   LQRRecyclerView mRvSelectedContacts;
    @BindView(R.id.etKey)
    EditText mEtKey;
    private View mHeaderView;
    @BindView(R.id.rvContacts)
    LQRRecyclerView mRvContacts;
    @BindView(R.id.qib)
    QuickIndexBar mQib;
    @BindView(R.id.tvLetter)
    TextView mTvLetter;
    public List<String> mSelectedTeamMemberAccounts;
    boolean isAdd;
    private CoreService mXmppService;
    private boolean mBind;

    private String mRoomId;
    private String mRoomJid;
    private String mRoomDes;
    private String mRoomName;

    @Override
    protected void initData() {
        mvpPresenter.loadContacts();
    }

    @Override
    protected void initTitle() {
        super.initTitle();
        setTitleText("创建群聊");
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getIntent() != null) {
            //isAdd = getIntent().getBooleanExtra(Constants.ISADD_USER, false);
            mRoomId = getIntent().getStringExtra("roomId");
            mRoomJid = getIntent().getStringExtra("roomJid");
            mRoomDes = getIntent().getStringExtra("roomDes");
            mRoomName = getIntent().getStringExtra("roomName");
            String ids = getIntent().getStringExtra("exist_ids");
            mSelectedTeamMemberAccounts = JSON.parseArray(ids, String.class);
        }
        if (mSelectedTeamMemberAccounts == null) {
            mSelectedTeamMemberAccounts = new ArrayList<>();
        }
        mBind = bindService(CoreService.getIntent(), mServiceConnection, BIND_AUTO_CREATE);
        super.onCreate(savedInstanceState);
    }

    public void initListener() {
        getBtnToolbarSend().setRightTextOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TextUtils.isEmpty(mRoomId)){
                    showCreateRoomChatDialog();
                }else {
                    mvpPresenter.addFriendforRoom(mRoomId);
                }
            }
        });
    }


    @Override
    protected void onDestroy() {
        if (mBind) {
            unbindService(mServiceConnection);
        }
        super.onDestroy();
    }

    private void showCreateRoomChatDialog() {
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
                String nickName = ConfigApplication.instance().mLoginUser.getNickName();
                final String roomJid = mXmppService.createMucRoom(nickName, roomName, null, roomDesc);
                mvpPresenter.createGroupChat(roomJid,roomName, null, roomDesc);
                dialog.dismiss();
            }
        });
        dialog.show();
    }


    @Override
    public void addFriendSuccess(List<Friend> mSelectedData) {
        MucRoomSimple mucRoomSimple = new MucRoomSimple();
        mucRoomSimple.setId(mRoomId);
        mucRoomSimple.setJid(mRoomJid);
        mucRoomSimple.setName(mRoomName);
        mucRoomSimple.setDesc(mRoomDes);
        mucRoomSimple.setUserId(ConfigApplication.instance().getLoginUserId());
        mucRoomSimple.setTimeSend(TimeUtils.sk_time_current_time());
        String reason = JSON.toJSONString(mucRoomSimple);
        // 邀请好友
        for (int i = 0; i < mSelectedData.size(); i++) {
            if (mSelectedData.get(i) == null) {
                continue;
            }

            String firendUserId = mSelectedData.get(i).getUserId();
            L_.e("添加成功" + firendUserId);
            mXmppService.invite(mRoomJid, firendUserId, reason);
            /*Intent broadcast=new Intent(Constants.CHAT_MESSAGE_DELETE_ACTION);
			broadcast.putExtra(Constants.GROUP_JOIN_NOTICE_FRIEND_ID,firendUserId);
			broadcast.putExtra(AppConstant.EXTRA_USER_ID, mRoomJid);
			this.sendBroadcast(broadcast);*/

        }
        setResult(RESULT_OK);
        finish();
    }


    @Override
    public void createRoomSuccess(String roomId, String roomJid, String roomName, String roomDesc) {
        Friend friend = new Friend();// 将房间也存为好友
        friend.setOwnerId(ConfigApplication.instance().getLoginUserId());
        friend.setUserId(roomJid);
        friend.setNickName(roomName);
        friend.setDescription(roomDesc);
        friend.setRoomFlag(1);
        friend.setRoomId(roomId);
        friend.setRoomCreateUserId(ConfigApplication.instance().getLoginUserId());
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
        mucRoomSimple.setUserId(ConfigApplication.instance().getLoginUserId());
        mucRoomSimple.setTimeSend(TimeUtils.sk_time_current_time());
        String reason = JSON.toJSONString(mucRoomSimple);
        Log.d("roamer", "reason:" + reason);
        // 邀请好友
        String[] noticeFriendList = new String[mSelectedTeamMemberAccounts.size()];
        for (int i = 0; i < mSelectedTeamMemberAccounts.size(); i++) {
            if (mSelectedTeamMemberAccounts.get(i) == null) {
                continue;
            }
            String firendUserId = mSelectedTeamMemberAccounts.get(i);
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


    private void showLetter(String letter) {
        mTvLetter.setVisibility(View.VISIBLE);
        mTvLetter.setText(letter);
    }

    private void hideLetter() {
        mTvLetter.setVisibility(View.GONE);
    }


    @Override
    protected CreateNewGroup.CreateGroupPresenter createPresenter() {
        return new CreateNewGroup.CreateGroupPresenter(this,this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_create_newgroup;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        initListener();
    }

    @Override
    public NavigationBarView getBtnToolbarSend() {
        return getToolBarView();
    }

    @Override
    public LQRRecyclerView getRvContacts() {
        return mRvContacts;
    }

    @Override
    public LQRRecyclerView getRvSelectedContacts() {
        return mRvSelectedContacts;
    }

    @Override
    public EditText getEtKey() {
        return null;
    }

    @Override
    public View getHeaderView() {
        mHeaderView = View.inflate(this, R.layout.header_group_cheat, null);
        return mHeaderView;
    }

}
