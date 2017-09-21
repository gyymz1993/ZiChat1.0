package com.lsjr.zizi.mvp.home.session;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.andview.adapter.ABaseRefreshAdapter;
import com.andview.adapter.BaseRecyclerHolder;
import com.andview.listener.OnItemClickListener;
import com.lsjr.bean.ArrayResult;
import com.lsjr.callback.ChatArrayCallBack;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.mvp.home.Constants;
import com.lsjr.zizi.chat.bean.MucRoom;
import com.lsjr.zizi.chat.bean.ResultCode;
import com.lsjr.zizi.chat.broad.MucgroupUpdateUtil;
import com.lsjr.zizi.chat.dao.FriendDao;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.loader.AvatarHelper;
import com.lsjr.zizi.mvp.home.zichat.CreatNewGroupActivity;
import com.lsjr.zizi.mvp.home.zichat.presenter.CreateNewGroup;
import com.lsjr.zizi.util.TimeUtils;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.UIUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/14 17:04
 */

@SuppressLint("Registered")
public class GroupActivity extends MvpActivity {
    /* 分页的Size */
    public static final int PAGE_SIZE = 50;
    private List<MucRoom> mMucRooms;
    private MucRoomAdapter mAdapter;
    private int mPageIndex = 0;
    private boolean mNeedUpdate = true;
    @BindView(R.id.id_contacts)
    RecyclerView idRvGroup;
    String loginUserId="";
    @Override
    protected BasePresenter createPresenter() {
        return null;
    }


    @Override
    protected int getLayoutId() {
        return R.layout.activity_nearby;
    }


    @Override
    protected void initView() {
        showProgressDialogWithText("加载中");
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
       registerReceiver(mUpdateReceiver, MucgroupUpdateUtil.getUpdateActionFilter());
       initRvView();
       requestData();
    }

    private void initRvView() {
        setTitleText("群聊");
        setTopLeftButton(R.drawable.ic_back).
                setTitleTextColor(UIUtils.getColor(R.color.white)).
                setBackgroundColor(UIUtils.getColor(R.color.colorPrimary));
        setTopRightButton(R.drawable.actionbar_add_icon, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //T_.showToastReal("新建群聊");
                //openActivity(CreateGroupActivity.class);
                openActivity(CreatNewGroupActivity.class);
            }
        });
        loginUserId = ConfigApplication.instance().mLoginUser.getUserId();
        mMucRooms = new ArrayList<>();
        mAdapter = new MucRoomAdapter(this,mMucRooms,R.layout.item_group);
        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(BaseRecyclerHolder baseRecyclerHolder, int position, Object item) {
                MucRoom room = mMucRooms.get(position);
                Friend friend = FriendDao.getInstance().getFriend(
                        loginUserId, room.getJid());
                if (friend == null) {// friend为null，说明之前没加入过该房间，那么调用接口加入
                    // 将房间作为一个好友存到好友表
                    joinRoom(room, loginUserId);
                } else {
                    interMucChat(room.getJid(), room.getName());
                }
//                Intent intent = new Intent(GroupActivity.this, ChatActivity.class);
//                //intent.putExtra(ChatActivity.FRIEND,friend);
//                intent.putExtra(Constants.EXTRA_USER_ID, friend.getUserId());
//                intent.putExtra(Constants.EXTRA_NICK_NAME, friend.getNickName());
//                intent.putExtra(Constants.EXTRA_IS_GROUP_CHAT, true);
//                startActivity(intent);
            }
        });
        idRvGroup.setLayoutManager(new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return true;
            }
        });
        idRvGroup.setAdapter(mAdapter);
    }


    private void joinRoom(final MucRoom room, final String loginUserId) {
        Log.d("roamer","joinRoom");
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", ConfigApplication.instance().mAccessToken);
        params.put("roomId", room.getId());
        if (room.getUserId() == loginUserId)
            params.put("type", "1");
        else
            params.put("type", "2");

        showProgressDialogWithText("进入房间");
        HttpUtils.getInstance().postServiceData(AppConfig.ROOM_JOIN, params, new ChatArrayCallBack<Void>(Void.class) {

            @Override
            protected void onXError(String exception) {
                dismissProgressDialog();
                //T_.showToastReal(exception);
            }

            @Override
            protected void onSuccess(ArrayResult<Void> result) {
                dismissProgressDialog();
                boolean success = ResultCode.defaultParser(result, true);
                if (success) {
                    Friend friend = new Friend();// 将房间也存为好友
                    friend.setOwnerId(loginUserId);
                    friend.setUserId(room.getJid());
                    friend.setNickName(room.getName());
                    friend.setDescription(room.getDesc());
                    friend.setRoomFlag(1);
                    friend.setRoomId(room.getId());
                    friend.setRoomCreateUserId(room.getUserId());
                    // timeSend作为取群聊离线消息的标志，所以要在这里设置一个初始值
                    friend.setTimeSend(TimeUtils.sk_time_current_time());
                    friend.setStatus(Friend.STATUS_FRIEND);
                    FriendDao.getInstance()
                            .createOrUpdateFriend(friend);

                    interMucChat(room.getJid(), room.getName());
                }
            }
        });

    }


    private void requestData() {
        mPageIndex = 0;
        HashMap<String, String> params = new HashMap<>();
        params.put("userId", loginUserId);
        params.put("pageIndex", String.valueOf(mPageIndex));
        params.put("pageSize", String.valueOf(PAGE_SIZE));
        params.put("access_token", ConfigApplication.instance().mAccessToken);
        HttpUtils.getInstance().postServiceData(AppConfig.ROOM_LIST, params, new ChatArrayCallBack<MucRoom>(MucRoom.class) {
            @Override
            protected void onXError(String exception) {
                dismissProgressDialog();
            }

            @Override
            protected void onSuccess(ArrayResult result) {
                dismissProgressDialog();
                boolean success = ResultCode.defaultParser(result, true);
                if (success) {
                    L_.e(result.getData().toString());
                    mMucRooms=result.getData();
                    mPageIndex++;
                    mAdapter.notifyDataSetChanged(mMucRooms);
                }
            }

        });
    }


    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MucgroupUpdateUtil.ACTION_UPDATE)) {
//                if (isResumed()) {
//                    requestData();
//                } else {
//                    mNeedUpdate = true;
//                }
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUpdateReceiver);
    }

    public class MucRoomAdapter extends ABaseRefreshAdapter<MucRoom> {

         MucRoomAdapter(Context context, List<MucRoom> datas, int itemLayoutId) {
            super(context, datas, itemLayoutId);
        }

        @Override
        protected void convert(BaseRecyclerHolder holder, MucRoom room, int position) {
            CircleImageView avatar_img = holder.getView(R.id.avatar_img);
            TextView nick_name_tv = holder.getView(R.id.nick_name_tv);
            TextView content_tv = holder.getView(R.id.content_tv);
            TextView time_tv = holder.getView(R.id.time_tv);
            L_.e(room.getName());
            Friend friend=new Friend();
            friend.setUserId(room.getUserId());
            friend.setNickName(room.getNickName());
            AvatarHelper.getInstance().displayAvatar(friend, avatar_img, true);
            nick_name_tv.setText(room.getName());
            time_tv.setText(TimeUtils.getFriendlyTimeDesc(UIUtils.getContext(), (int) room.getCreateTime()));
            content_tv.setText(room.getDesc());
            holder.getView(R.id.id_root_ry).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    interMucChat(room.getJid(),room.getName());
                }
            });
        }
    }

    private void interMucChat(String roomJid, String roomName) {
        Log.d("roamer","加入群聊");
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(Constants.EXTRA_USER_ID, roomJid);
        intent.putExtra(Constants.EXTRA_NICK_NAME, roomName);
        intent.putExtra(Constants.EXTRA_IS_GROUP_CHAT, true);
        startActivity(intent);
    }

}
