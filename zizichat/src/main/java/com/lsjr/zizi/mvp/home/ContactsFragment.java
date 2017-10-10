package com.lsjr.zizi.mvp.home;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpFragment;
import com.lsjr.zizi.chat.bean.BaseSortModel;
import com.lsjr.zizi.chat.dao.FriendDao;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.loader.AvatarHelper;
import com.lsjr.zizi.mvp.home.session.BasicInfoActivity;
import com.lsjr.zizi.mvp.home.session.GroupActivity;
import com.lsjr.zizi.mvp.home.session.NearbyActivity;
import com.lsjr.zizi.mvp.home.session.NewFriendActivity;
import com.lsjr.zizi.mvp.home.session.PhoneContactActivity;
import com.lsjr.zizi.mvp.home.session.SeachFriendActivity;
import com.lsjr.zizi.view.ClearEditText;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.UIUtils;
import com.ys.cn.CNPinyin;
import com.ys.cn.CNPinyinFactory;
import com.ys.cn.CNPinyinIndex;
import com.ys.cn.CNPinyinIndexFactory;
import com.zhy.autolayout.AutoLinearLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import cn.ittiger.indexlist.IndexStickyView;
import cn.ittiger.indexlist.adapter.IndexHeaderFooterAdapter;
import cn.ittiger.indexlist.adapter.IndexStickyViewAdapter;
import de.hdodenhof.circleimageview.CircleImageView;


/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/7 13:50
 */

public class ContactsFragment extends MvpFragment {

    MyIndexStickyViewAdapter mAdapter;
    private List<Friend> friends;
    private String mLoginUserId;
    @BindView(R.id.id_contacts)
    IndexStickyView idContacts;
    @BindView(R.id.swipeRefreshLayout)
    SwipeRefreshLayout mRefreshLayout;
    @BindView(R.id.search_edit)
    ClearEditText mClearEditText;
    List<CNPinyin<Friend>> contactList;
    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_contacte;
    }

    @Override
    protected void initView() {
        idContacts.setAdapter(mAdapter);
        initRecycleView();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        mLoginUserId = ConfigApplication.instance().mLoginUser.getUserId();
        friends = new ArrayList<>();
        mAdapter = new MyIndexStickyViewAdapter(friends);
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        loadData();
        //loadData();
        mClearEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String filter = mClearEditText.getText().toString().trim().toUpperCase();
                friends.clear();
                if (contactList.size() > 0) {
                    ArrayList<CNPinyinIndex<Friend>> cnPinyinIndices = CNPinyinIndexFactory.indexList(contactList, filter);
                    for (int i=0;i<cnPinyinIndices.size();i++){
                        Friend data = cnPinyinIndices.get(i).cnPinyin.data;
                        friends.add(data);
                        L_.e("f------"+friends.get(i));
                    }
                }
                mAdapter.reset(friends);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length()==0){
                    loadData();
                }
            }
        });


    }

    public void initRecycleView() {
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mRefreshLayout.setRefreshing(true);
                mRefreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadData();
                        mRefreshLayout.setRefreshing(false);
                    }
                }, 1000);
            }
        });
       // idContacts.addItemDecoration(new IndexStickyViewDecoration(getActivity()));
        addHeadView();
       // loadData();

    }


    public void addHeadView() {

        //添加自定义头部菜单项
        IndexHeaderFooterAdapter headerFooterAdapter = new IndexHeaderFooterAdapter<Friend>() {
            @Override
            public HeadViewHolder onCreateViewHolder(ViewGroup parent) {
                View view = LayoutInflater.from(getContext()).inflate(R.layout.header_rv_contacts, parent, false);
                return new HeadViewHolder(view);
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, Friend itemData) {
                HeadViewHolder headViewHolder = (HeadViewHolder) holder;

                headViewHolder.llNewFriend.setOnClickListener(v -> {
                    //openActivity(GroupActivity.class);
                    openActivity(SeachFriendActivity.class);
                });

                headViewHolder.llGroup.setOnClickListener(v -> openActivity(GroupActivity.class));
                headViewHolder.llNearFriend.setOnClickListener(v -> openActivity(NearbyActivity.class));

                headViewHolder.llContact.setOnClickListener(v -> openActivity(PhoneContactActivity.class));
            }
        };

        mAdapter.addIndexHeaderAdapter(headerFooterAdapter);

    }


    class MyIndexStickyViewAdapter extends IndexStickyViewAdapter<Friend> {

        public MyIndexStickyViewAdapter(List<Friend> list) {

            super(list);
        }

        @Override
        public RecyclerView.ViewHolder onCreateIndexViewHolder(ViewGroup parent) {

            View view = LayoutInflater.from(getContext()).inflate(R.layout.indexsticky_item_index, parent, false);
            return new IndexViewHolder(view);
        }

        @Override
        public RecyclerView.ViewHolder onCreateContentViewHolder(ViewGroup parent) {

            View view = LayoutInflater.from(getContext()).inflate(R.layout.item_contacts, parent, false);
            return new ContentViewHolder(view);
        }

        @Override
        public void onBindIndexViewHolder(RecyclerView.ViewHolder holder, int position, String indexName) {

            IndexViewHolder indexViewHolder = (IndexViewHolder) holder;
            indexViewHolder.mTextView.setText(indexName);
            //indexViewHolder.mTextView.setTextColor(UIUtils.getColor(R.color.button_bg));
        }

        @Override
        public void onBindContentViewHolder(RecyclerView.ViewHolder holder, int position, Friend friend) {
            // 设置头像
            //final Friend friend = friends.get(position);
            ContentViewHolder contentViewHolder = (ContentViewHolder) holder;
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
                AvatarHelper.getInstance().displayAvatar(friend, contentViewHolder.avatar_img, true);
            } else {// 这是1个房间
                if (TextUtils.isEmpty(friend.getRoomCreateUserId())) {
                    contentViewHolder.avatar_img.setImageResource(R.drawable.avatar_normal);
                } else {
                    AvatarHelper.getInstance().displayAvatar(friend, contentViewHolder.avatar_img, true);// 目前在备注名放房间的创建者Id
                }
            }

            L_.e("好友详情页信息------------>"+friend.toString());
            // 昵称
            String name = friend.getRemarkName();
            if (TextUtils.isEmpty(name)) {
                name = friend.getNickName();

            }
            L_.e("设置好友昵称"+name);
            contentViewHolder.nick_name_tv.setText(name);

            // 个性签名
            if (!TextUtils.isEmpty(friend.getNickName())){
                contentViewHolder.des_tv.setVisibility(View.GONE);
                contentViewHolder.des_tv.setText(friend.getRemarkName());
            }else {
                contentViewHolder.des_tv.setVisibility(View.GONE);
            }

            contentViewHolder.id_root_ry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(friend.getUserId().equals(Friend.ID_NEW_FRIEND_MESSAGE)){
                        openActivity(NewFriendActivity.class);
                    }else {
                        Intent intent = new Intent(getContext(), BasicInfoActivity.class);
                        intent.putExtra(AppConfig.EXTRA_USER_ID, friend.getUserId());
                        startActivity(intent);
                    }

                }
            });
        }
    }


    class IndexViewHolder extends RecyclerView.ViewHolder {
        TextView mTextView;

        public IndexViewHolder(View itemView) {

            super(itemView);
            mTextView = (TextView) itemView.findViewById(R.id.tv_index);
        }
    }

    class ContentViewHolder extends RecyclerView.ViewHolder {
        RelativeLayout id_root_ry;
        TextView catagoryTitleTv;
        CircleImageView avatar_img;
        TextView nick_name_tv;
        TextView des_tv;

        public ContentViewHolder(View itemView) {
            super(itemView);
            id_root_ry = (RelativeLayout) itemView.findViewById(R.id.id_root_ry);
            catagoryTitleTv = (TextView) itemView.findViewById(R.id.catagory_title);
            avatar_img = (CircleImageView) itemView.findViewById(R.id.avatar_img);
            nick_name_tv = (TextView) itemView.findViewById(R.id.nick_name_tv);
            des_tv = (TextView) itemView.findViewById(R.id.des_tv);
        }
    }



    @Override
    protected void lazyLoad() {
        super.lazyLoad();
        L_.e("开始加载朋友条数");

    }

    public void loadData() {
        new Thread(() -> {
            long startTime = System.currentTimeMillis();
            friends = FriendDao.getInstance().getAllFriends(mLoginUserId);
            contactList = CNPinyinFactory.createCNPinyinList(friends);
            Collections.sort(contactList);
            if (friends==null||friends.size()==0)return;
            L_.e("朋友条数-------->"+friends.size());
            long delayTime = 200 - (startTime - System.currentTimeMillis());// 保证至少200ms的刷新过程
            if (delayTime < 0) {
                delayTime = 0;
            }
            UIUtils.getHandler().postDelayed(() ->
                    mAdapter.reset(friends), delayTime);
        }).start();
    }


    class HeadViewHolder extends RecyclerView.ViewHolder {
        AutoLinearLayout llGroup;
        AutoLinearLayout llNearFriend;
        AutoLinearLayout llNewFriend;
        AutoLinearLayout llContact;

        public HeadViewHolder(View itemView) {
            super(itemView);
            llGroup = (AutoLinearLayout) itemView.findViewById(R.id.llGroup);
            llNearFriend  = (AutoLinearLayout) itemView.findViewById(R.id.llNearFriend);
            llNewFriend = (AutoLinearLayout) itemView.findViewById(R.id.llNewFriend);
            llContact = (AutoLinearLayout) itemView.findViewById(R.id.llContact);
        }
    }


    public View getHeadView() {
        View headView = UIUtils.inflate(R.layout.header_rv_contacts);
        AutoLinearLayout llGroup = (AutoLinearLayout) headView.findViewById(R.id.llGroup);
        AutoLinearLayout llNearFriend = (AutoLinearLayout) headView.findViewById(R.id.llNearFriend);

        llGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openActivity(GroupActivity.class);
            }
        });
        llNearFriend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openActivity(NearbyActivity.class);
            }
        });
        return headView;
    }

}
