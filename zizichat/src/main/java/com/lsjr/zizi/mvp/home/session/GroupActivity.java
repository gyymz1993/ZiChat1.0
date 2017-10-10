package com.lsjr.zizi.mvp.home.session;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.andview.adapter.ABaseRefreshAdapter;
import com.andview.adapter.BaseRecyclerHolder;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpActivity;
import com.lsjr.zizi.chat.bean.MucRoom;
import com.lsjr.zizi.chat.broad.MucgroupUpdateUtil;
import com.lsjr.zizi.chat.dao.FriendDao;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.loader.AvatarHelper;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.mvp.home.Constants;
import com.lsjr.zizi.mvp.home.zichat.CreatNewGroupActivity;
import com.lsjr.zizi.mvp.home.zichat.presenter.GroupList;
import com.lsjr.zizi.util.TimeUtils;
import com.lsjr.zizi.view.CircleImageView;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.UIUtils;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/14 17:04
 */

@SuppressLint("Registered")
public class GroupActivity extends MvpActivity<GroupList.GroupListPresenter> implements GroupList.IView {

    @BindView(R.id.id_contacts)
    RecyclerView idRvGroup;

    private List<MucRoom> mMucRooms;
    private MucRoomAdapter mAdapter;

    @Override
    protected GroupList.GroupListPresenter createPresenter() {
        return new GroupList.GroupListPresenter(this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_nearby;
    }


    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        EventBus.getDefault().register(this);
       registerReceiver(mUpdateReceiver, MucgroupUpdateUtil.getUpdateActionFilter());
       initRvView();
       mvpPresenter.requestData();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void nameUpdate(String update) {
        if (!TextUtils.isEmpty(update)){
            mvpPresenter.requestData();
        }
    }

    @Override
    protected void initTitle() {
        super.initTitle();
        setTitleText("群聊");
        getToolBarView().getRightImageView().setVisibility(View.VISIBLE);
        getToolBarView().getRightImageView().setImageResource(R.drawable.icon_add);
        getToolBarView().getRightImageView().setOnClickListener(v -> {
            openActivity(CreatNewGroupActivity.class);
        });
    }

    private void initRvView() {
        mMucRooms = new ArrayList<>();
        mAdapter = new MucRoomAdapter(this,mMucRooms,R.layout.item_group);
        mAdapter.setOnItemClickListener((baseRecyclerHolder, position, item) -> {
            MucRoom room = mMucRooms.get(position);
            Friend friend = FriendDao.getInstance().getFriend(
                    ConfigApplication.instance().getLoginUserId(), room.getJid());
            if (friend == null) {// friend为null，说明之前没加入过该房间，那么调用接口加入
                // 将房间作为一个好友存到好友表
                mvpPresenter.joinRoom(room, ConfigApplication.instance().getLoginUserId());
            } else {
                interMucChat(room.getJid(), room.getName());
            }
        });

        idRvGroup.setLayoutManager(new LinearLayoutManager(this){
            @Override
            public boolean canScrollVertically() {
                return true;
            }
        });
        idRvGroup.setAdapter(mAdapter);
    }



    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MucgroupUpdateUtil.ACTION_UPDATE)) {
                mvpPresenter.requestData();
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mUpdateReceiver);
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void showError(int str) {
        dismissProgressDialog();
    }

    @Override
    public void showLoading() {
        showProgressDialogWithText("下载数据");
    }

    @Override
    public RecyclerView getRvListView() {
        return null;
    }

    @Override
    public void interMucChat(String roomId, String roomName) {
        Log.d("roamer","加入群聊");
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(Constants.EXTRA_USER_ID, roomId);
        intent.putExtra(Constants.EXTRA_NICK_NAME, roomName);
        intent.putExtra(Constants.EXTRA_IS_GROUP_CHAT, true);
        startActivity(intent);
    }


    @Override
    public void loadDataSucceed(List<MucRoom> mMuc) {
        dismissProgressDialog();
        mMucRooms=mMuc;
        mAdapter.notifyDataSetChanged(mMucRooms);
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
            holder.getView(R.id.id_root_ry).setOnClickListener(v -> {
                //onJoinRoom(room.getJid(),room.getName());
            });
            holder.getView(R.id.id_root_ry).setOnLongClickListener(v -> true);

        }
    }

}
