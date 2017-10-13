package com.lsjr.zizi.mvp.home.zichat;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.alibaba.fastjson.JSON;
import com.andview.myrvview.LQRRecyclerView;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.chat.bean.MucRoom;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.chat.xmpp.CoreService;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.mvp.home.Constants;
import com.lsjr.zizi.mvp.home.session.GroupActivity;
import com.lsjr.zizi.mvp.home.zichat.presenter.GroupInfo;
import com.lsjr.zizi.view.OptionItemView;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;
import com.zhy.autolayout.AutoLinearLayout;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/16 9:21
 */

@SuppressLint("Registered")
public class GroupInfoActivity extends MvpActivity<GroupInfo.Presenter> implements GroupInfo.IView ,View.OnClickListener{

    @BindView(R.id.id_more)
    ImageView idMore;
    @BindView(R.id.rv_group_manager)
    LQRRecyclerView rvGroupMg;

    @BindView(R.id.group_name)
    TextView groupName;
    @BindView(R.id.tvAccount)
    TextView tvAccount;
    @BindView(R.id.id_user_size)
    TextView tvUserSize;
    @BindView(R.id.id_al_avatar)
    AutoLinearLayout idAlAvatar;
    @BindView(R.id.llMyInfo)
    AutoLinearLayout llMyInfo;

    @BindView(R.id.oivRoomName)
    OptionItemView oivRoomName;
    @BindView(R.id.id_featuregroup)
    OptionItemView idFeaturegroup;
    @BindView(R.id.id_user_nickName)
    OptionItemView idUserNickName;
    @BindView(R.id.id_invitation_user)
    OptionItemView idInviattionUser;
    @BindView(R.id.item_table_bar)
    RelativeLayout itemTableAdd;

    @BindView(R.id.delet_room)
    TextView deletRoom;


    private String mRoomJid;
    private MucRoom mucRoom;
    private Friend mRoom;

    private boolean mBind;
    private CoreService mXmppService;

    @Override
    protected int getLayoutId() {
        return R.layout.ac_group_info;
    }

    @Override
    protected void initTitle() {
        super.initTitle();
        setTitleText("成员管理");

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
    protected void initData() {
        mvpPresenter.init();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (getIntent() != null) {
            mRoomJid = getIntent().getStringExtra(AppConfig.EXTRA_USER_ID);
        }
        if (TextUtils.isEmpty(mRoomJid)) {
            return;
        }
        mBind = bindService(CoreService.getIntent(), mServiceConnection, BIND_AUTO_CREATE);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        oivRoomName.setOnClickListener(this);
        idFeaturegroup.setOnClickListener(this);
        idUserNickName.setOnClickListener(this);
        idInviattionUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoAddContact();
            }
        });
        itemTableAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoAddContact();
            }
        });

        deletRoom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mvpPresenter.deleteRoom(mRoom);
            }
        });

    }


    public void gotoAddContact() {
        List<String> existIds = new ArrayList<>();
        if (mucRoom==null)return;
        for (int i = 0; i < mucRoom.getMembers().size(); i++) {
            existIds.add(mucRoom.getMembers().get(i).getUserId());
        }
        // 去添加人
        if (mucRoom == null) return;
        Intent intent = new Intent(GroupInfoActivity.this, CreatNewGroupActivity.class);
        intent.putExtra("exist_ids", JSON.toJSONString(existIds));
        intent.putExtra("roomId", mRoom.getRoomId());
        intent.putExtra("roomJid", mRoomJid);
        intent.putExtra("roomName", mucRoom.getName());
        intent.putExtra("roomDes", mRoom.getDescription());
        intent.putExtra("roomUserId", mucRoom.getUserId());
        intent.putExtra(Constants.ISADD_USER, true);
        startActivityForResult(intent, 1);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK) {
            mvpPresenter.loadMembers();
        }
        if (requestCode == 2 && resultCode == RESULT_OK) {
            //T_.showToastReal("修改成功");
            if (data==null)return;
            String updateStr= data.getStringExtra("udaptes");
           if (TextUtils.isEmpty(updateStr))return;
            switch (data.getIntExtra("key",0)){
                case 0:
                    return;
                case 1:
                    oivRoomName.setRightText(updateStr);
                    break;
                case 2:
                    idFeaturegroup.setRightText(updateStr);
                    break;
                case 3:
                    idUserNickName.setRightText(updateStr);
                    break;
            }
        }
    }


    @Override
    protected GroupInfo.Presenter createPresenter() {
        return new GroupInfo.Presenter(this, this);
    }

    @Override
    public LQRRecyclerView getRecycleView() {
        return rvGroupMg;
    }

    @Override
    public void upDataUI(MucRoom mucRoom) {
        this.mucRoom=mucRoom;
        oivRoomName.setRightText(mucRoom.getName());
        tvUserSize.setText(String.valueOf(mucRoom.getMembers().size()));
    }

    @Override
    public void getRoom(Friend mRoom) {
        this.mRoom=mRoom;
        idFeaturegroup.setRightText(mRoom.getDescription());
        if (mRoom.getRoomMyNickName()!=null){
            idUserNickName.setRightText(mRoom.getRoomMyNickName());
        }else {
            idUserNickName.setRightText(ConfigApplication.instance().mLoginUser.getNickName());
        }

    }

    @Override
    public void deleteRoom(boolean isDelete) {
        if (isDelete){
            exitMucChat(mRoom.getUserId());
            openActivity(GroupActivity.class);
        }

    }

    @Override
    protected void onDestroy() {
        if (mBind) {
            unbindService(mServiceConnection);
        }
        super.onDestroy();
    }

    public void exitMucChat(String toUserId) {
        if (mXmppService != null) {
            T_.showToastReal("退出群聊");
            mXmppService.exitMucChat(toUserId);
        }
    }


    public String getmRoomJid() {
        return mRoomJid;
    }


    @Override
    public void onClick(View v) {
        Intent intent = new Intent(GroupInfoActivity.this, UpdateSourceActivity.class);
        Bundle bundle=new Bundle();
        switch (v.getId()){
            case R.id.oivRoomName:
                bundle.putString("updateContent","修改群昵称");
                bundle.putInt("key",1);
                break;
            case R.id.id_featuregroup:
                bundle.putString("updateContent","修改群公告");
                bundle.putInt("key",2);
                break;
            case R.id.id_user_nickName:
                bundle.putString("updateContent","修改我的昵称");
                bundle.putInt("key",3);
                break;
        }
        bundle.putString("roomId",mRoom.getRoomId());
        bundle.putSerializable("room", mRoom);
        intent.putExtras(bundle);
        startActivityForResult(intent, 2);
    }
}
