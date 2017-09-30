package com.lsjr.zizi.mvp.home.session.adapter;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.andview.adapter.ABaseRefreshAdapter;
import com.andview.adapter.BaseRecyclerHolder;
import com.lsjr.zizi.R;
import com.lsjr.zizi.chat.bean.MucRoom;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.loader.AvatarHelper;
import com.lsjr.zizi.mvp.home.zichat.presenter.GroupList;
import com.lsjr.zizi.util.TimeUtils;
import com.lsjr.zizi.view.CircleImageView;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.UIUtils;

import java.util.List;

public class MucRoomAdapter extends ABaseRefreshAdapter<MucRoom> {

    GroupList.GroupListPresenter presenter;
         public MucRoomAdapter(Context context, List<MucRoom> datas, int itemLayoutId) {
            this(context, datas, itemLayoutId,null);
        }

        public MucRoomAdapter(Context context, List<MucRoom> datas, int itemLayoutId, GroupList.GroupListPresenter presenter) {
            super(context, datas, itemLayoutId);
            this.presenter=presenter;
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
                    //onJoinRoom(room.getJid(),room.getName());
                    //presenter.joinRoom();
                }
            });
            holder.getView(R.id.id_root_ry).setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return false;
                }
            });

        }
    }