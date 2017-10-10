package com.lsjr.zizi.mvp.home.zichat.presenter;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.andview.adapter.ABaseRefreshAdapter;
import com.andview.adapter.BaseRecyclerHolder;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.BaseContract;
import com.lsjr.zizi.chat.dao.FriendDao;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.loader.AvatarHelper;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.UIUtils;
import com.ys.cn.CNPinyin;
import com.ys.cn.CNPinyinFactory;
import com.ys.head.StickyHeaderAdapter;
import com.ys.head.StickyHeaderDecoration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/10/9 10:07
 */

public interface AddressListFrag {

    interface IView extends BaseContract.View{
        RecyclerView getRvView();

        View getHeadView();
    }

    class Presenter extends BasePresenter<AddressListFrag.IView>{
        private String mLoginUserId;
        private List<Friend> friends;
        AddressListAdapter adapter;
        LinearLayoutManager manager;
        private ArrayList<CNPinyin<Friend>> contactList;
        public Presenter(IView mvpView) {
            super(mvpView);
            friends = new ArrayList<>();
            contactList=new ArrayList<>();
            mLoginUserId = ConfigApplication.instance().mLoginUser.getUserId();
        }

        public void loadData(Context context) {
            new Thread(() -> {
                long startTime = System.currentTimeMillis();
                friends = FriendDao.getInstance().getAllFriends(mLoginUserId);
                contactList = CNPinyinFactory.createCNPinyinList(friends);
                Collections.sort(contactList);
                if (contactList==null||contactList.size()==0)return;
                L_.e("朋友条数-------->"+contactList.size());
                long delayTime = 200 - (startTime - System.currentTimeMillis());// 保证至少200ms的刷新过程
                if (delayTime < 0) {
                    delayTime = 0;
                }
                initAdapter(context);
            }).start();
        }


        public  void initAdapter(Context context){
            if (adapter==null){
                manager = new LinearLayoutManager(context);
                mvpView.getRvView().setLayoutManager(manager);
                adapter =new AddressListAdapter(context,contactList, R.layout.item_contacts);
                mvpView.getRvView().setAdapter(adapter);
                mvpView.getRvView().addItemDecoration(new StickyHeaderDecoration(adapter));
                //adapter.setHeaderView(mvpView.getHeadView(),mvpView.getRvView());
            }else {
                adapter.notifyDataSetChanged();
            }

        }

        public ArrayList<CNPinyin<Friend>> getContactList() {
            return contactList;
        }

        public LinearLayoutManager getManager() {
            return manager;
        }
    }


    public class AddressListAdapter extends ABaseRefreshAdapter<CNPinyin<Friend>> implements StickyHeaderAdapter<AddressListAdapter.HeaderHolder> {

        private List<CNPinyin<Friend>> contactList;
        public AddressListAdapter(Context context, List<CNPinyin<Friend>> datas, int itemLayoutId) {
            super(context, datas, itemLayoutId);
            this.contactList=datas;
        }

        @Override
        protected void convert(BaseRecyclerHolder baseRecyclerHolder,CNPinyin<Friend> friendCNPinyin, int i) {
            // 设置头像
            //final Friend friend = friends.get(position);
            RelativeLayout id_root_ry;
            TextView catagoryTitleTv;
            TextView nick_name_tv;
            TextView des_tv;
            CircleImageView avatar_img = baseRecyclerHolder.getView(R.id.avatar_img);
            id_root_ry = baseRecyclerHolder.getView(R.id.id_root_ry);
            catagoryTitleTv =  baseRecyclerHolder.getView(R.id.catagory_title);
            nick_name_tv =  baseRecyclerHolder.getView(R.id.nick_name_tv);
            des_tv = baseRecyclerHolder.getView(R.id.des_tv);
            Friend friend = friendCNPinyin.data;
            if (friend.getRoomFlag() == 0) {// 这是单个人
                if (friend.getUserId().equals(Friend.ID_SYSTEM_MESSAGE)) {// 系统消息的头像
                    //contentViewHolder.avatar_img.setImageResource(R.drawable.im_notice);
                    friend.setNickName("管家服务");

                } else if (friend.getUserId().equals(Friend.ID_NEW_FRIEND_MESSAGE)) {// 新朋友的头像
                    //contentViewHolder.avatar_img.setImageResource(R.drawable.im_new_friends);
                    friend.setNickName("新的朋友");
                } else {// 其他
                    // AvatarHelper.getInstance().displayAvatar(friend, contentViewHolder.avatar_img, true);
                }
                AvatarHelper.getInstance().displayAvatar(friend, avatar_img, true);
            } else {// 这是1个房间
                if (TextUtils.isEmpty(friend.getRoomCreateUserId())) {
                    avatar_img.setImageResource(R.drawable.avatar_normal);
                } else {
                    AvatarHelper.getInstance().displayAvatar(friend, avatar_img, true);// 目前在备注名放房间的创建者Id
                }
            }

            L_.e("好友详情页信息------------>"+friend.toString());
            // 昵称
            String name = friend.getRemarkName();
            if (TextUtils.isEmpty(name)) {
                name = friend.getNickName();

            }
            L_.e("设置好友昵称"+name);
            nick_name_tv.setText(name);

            // 个性签名
            if (!TextUtils.isEmpty(friend.getNickName())){
                des_tv.setVisibility(View.GONE);
                des_tv.setText(friend.getRemarkName());
            }else {
                des_tv.setVisibility(View.GONE);
            }

            id_root_ry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(friend.getUserId().equals(Friend.ID_NEW_FRIEND_MESSAGE)){
                       // openActivity(NewFriendActivity.class);
                    }else {
//                        Intent intent = new Intent(getContext(), BasicInfoActivity.class);
//                        intent.putExtra(AppConfig.EXTRA_USER_ID, friend.getUserId());
//                        startActivity(intent);
                    }

                }
            });
        }

        @Override
        public long getHeaderId(int childAdapterPosition) {
            return contactList.get(childAdapterPosition).getFirstChar();
        }

        @Override
        public HeaderHolder onCreateHeaderViewHolder(ViewGroup parent) {
            return new HeaderHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.indexsticky_item_index, parent, false));
        }

        @Override
        public void onBindHeaderViewHolder(HeaderHolder holder, int childAdapterPosition) {
            holder.tv_header.setText(String.valueOf(contactList.get(childAdapterPosition).getFirstChar()));
        }


        public class HeaderHolder extends RecyclerView.ViewHolder {
            public final TextView tv_header;
            public HeaderHolder(View itemView) {
                super(itemView);
                tv_header = (TextView) itemView.findViewById(R.id.tv_index);
            }
        }


    }
}
