package com.lsjr.zizi.mvp.home;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.lsjr.bean.ObjectResult;
import com.lsjr.callback.ChatObjectCallBack;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.R;
import com.lsjr.zizi.base.MvpFragment;
import com.lsjr.zizi.chat.bean.BaseSortModel;
import com.lsjr.zizi.chat.bean.MucRoom;
import com.lsjr.zizi.chat.bean.ResultCode;
import com.lsjr.zizi.chat.broad.MsgBroadcast;
import com.lsjr.zizi.chat.broad.MucgroupUpdateUtil;
import com.lsjr.zizi.chat.dao.ChatMessageDao;
import com.lsjr.zizi.chat.dao.FriendDao;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.chat.thread.ThreadManager;
import com.lsjr.zizi.chat.utils.HtmlUtils;
import com.lsjr.zizi.chat.utils.StringUtils;
import com.lsjr.zizi.chat.utils.TimeUtils;
import com.lsjr.zizi.chat.xmpp.XmppMessage;
import com.lsjr.zizi.loader.AvatarHelper;
import com.lsjr.zizi.mvp.home.session.ChatActivity;
import com.lsjr.zizi.mvp.home.session.NewFriendActivity;
import com.lsjr.zizi.util.PinyinUtils;
import com.lsjr.zizi.view.ClearEditText;
import com.lsjr.zizi.view.NineGridImageView;
import com.lsjr.zizi.view.groupview.DingViewGroup;
import com.lsjr.zizi.view.groupview.PerionIconFactory;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;
import com.ymz.baselibrary.utils.UIUtils;
import com.ys.uilibrary.base.BaseRecyclerAdapter;
import com.ys.uilibrary.base.BaseRecyclerHolder;
import com.ys.uilibrary.swip.SwipeMenuLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import butterknife.BindView;
import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/8/5 16:14
 */

public class MessageFragment extends MvpFragment {

    ClearEditText mClearEditText;
    @BindView(R.id.id_ms_rv)
    RecyclerView idMsRv;
    private boolean mNeedUpdate = false;
    private List<BaseSortModel<Friend>> mFriendList;// 筛选后的朋友数据
    private List<BaseSortModel<Friend>> mOriginalFriendList;// 原始的朋友数据，也就是从数据库查询出来，没有筛选的
    private Handler mHandler = new Handler();
    private MessageAdapter mAdapter;

    public MessageFragment() {
        mOriginalFriendList = new ArrayList<>();
        mFriendList = new ArrayList<>();
    }

    @Override
    protected void lazyLoad() {
        super.lazyLoad();
    }


    @Override
    protected void initView() {
        mClearEditText = (ClearEditText) mView.findViewById(R.id.search_edit);
        mClearEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String filter = mClearEditText.getText().toString().trim().toUpperCase();
                mFriendList.clear();
                if (mOriginalFriendList != null && mOriginalFriendList.size() > 0) {
                    for (int i = 0; i < mOriginalFriendList.size(); i++) {
                        BaseSortModel<Friend> mode = mOriginalFriendList.get(i);
                        // 获取筛选的数据
                        if (TextUtils.isEmpty(filter) || mode.getSimpleSpell().startsWith(filter) || mode.getWholeSpell().startsWith(filter)
                                || mode.getBean().getShowName().startsWith(filter)) {
                            mFriendList.add(mode);
                        }
                    }
                }
                mAdapter.notifyDataSetChanged();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        mAdapter = new MessageAdapter(getActivity(),mFriendList,R.layout.item_message);
        idMsRv.setLayoutManager(new LinearLayoutManager(getContext()) {
            @Override
            public boolean canScrollVertically() {
                return true;
            }
        });
        mAdapter.setOnItemSelectListener(new OnItemSelectListener<BaseSortModel< Friend >>() {
            @Override
            public void onItemSelect(BaseSortModel < Friend >  friendBaseSortModel) {
                Friend friend=friendBaseSortModel.getBean();
                UIUtils.getHandler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (friend.getUnReadNum() > 0) {
                            MsgBroadcast.broadcastMsgNumUpdate(getActivity(), false, friend.getUnReadNum());
                            friend.setUnReadNum(0);
                            mAdapter.notifyDataSetChanged();
                        }
                    }
                },1000);

                /*将消息设置为0*/
                if (friend.getRoomFlag() == 0) {
                    if (friend.getUserId().equals(Friend.ID_NEW_FRIEND_MESSAGE)) {// 新朋友消息
                        openActivity( NewFriendActivity.class);
                    } else {
                        Intent intent = new Intent(getActivity(), ChatActivity.class);
                        intent.putExtra(ChatActivity.FRIEND,friend);
                        startActivity(intent);
                    }
                } else {
                    Intent intent = new Intent(getActivity(), ChatActivity.class);
                    intent.putExtra(Constants.EXTRA_USER_ID, friend.getUserId());
                    intent.putExtra(Constants.EXTRA_NICK_NAME, friend.getNickName());
                    intent.putExtra(Constants.EXTRA_IS_GROUP_CHAT, true);
                    startActivity(intent);
                }
            }
        });
        idMsRv.setAdapter(mAdapter);

    }



    @Override
    public void onResume() {
        super.onResume();
        if (mNeedUpdate) {
            mNeedUpdate = false;
            loadData();
        }

    }

    @Override
    protected BasePresenter createPresenter() {
        return null;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_message;
    }

    @Override
    protected void afterCreate(Bundle savedInstanceState) {
        loadData();
        getActivity().registerReceiver(mUpdateReceiver, new IntentFilter(MsgBroadcast.ACTION_MSG_UI_UPDATE));
    }

    private BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(MsgBroadcast.ACTION_MSG_UI_UPDATE)) {
                if (isResumed()) {
                    loadData();
                } else {
                    mNeedUpdate = true;
                }
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unregisterReceiver(mUpdateReceiver);
    }

    /**
     * 是下拉刷新，还是上拉加载
     */

    private void loadData() {
        new Thread(() -> {
            String mLoginUserId = ConfigApplication.instance().mLoginUser.getUserId();
            long startTime = System.currentTimeMillis();
            final List<Friend> friends = FriendDao.getInstance().getNearlyFriendMsg(mLoginUserId);
            if (friends != null && friends.size() < 1){
                loadData();
                return;
            }
            assert friends != null;
            if (friends.size()==1){
                // 通知界面更新
                HomeActivity homeActivity= (HomeActivity) getActivity();
                if (homeActivity!=null&&homeActivity.getmXmppService()!=null){
                    MsgBroadcast.broadcastMsgNumReset(homeActivity.getmXmppService());
                }
            }
            long delayTime = 200 - (startTime - System.currentTimeMillis());// 保证至少200ms的刷新过程
            if (delayTime < 0) {
                delayTime = 0;
            }
            mHandler.postDelayed(() -> {
                mOriginalFriendList.clear();
                mFriendList.clear();
                String filter = mClearEditText.getText().toString().trim().toUpperCase();
                if (friends.size() > 0) {
                    for (int i = 0; i < friends.size(); i++) {
                        BaseSortModel<Friend> mode = new BaseSortModel<>();
                        mode.setBean(friends.get(i));
                        setSortCondition(mode);
                        mOriginalFriendList.add(mode);
                        // 获取筛选的数据
                        if (TextUtils.isEmpty(filter) || mode.getSimpleSpell().startsWith(filter) || mode.getWholeSpell().startsWith(filter)
                                || mode.getBean().getShowName().startsWith(filter)) {
                            mFriendList.add(mode);
                        }
                        L_.e("当前消息界面数据---->"+friends.size()+":"+friends.get(i).toString());
                    }
                }
                mAdapter.notifyDataSetChanged();
            }, delayTime);
        }).start();

    }


    private void setSortCondition(BaseSortModel<Friend> mode) {
        Friend friend = mode.getBean();
        if (friend == null) {
            return;
        }
        String name = friend.getShowName();
        String wholeSpell = PinyinUtils.getPingYin(name);
        if (!TextUtils.isEmpty(wholeSpell)) {
            String firstLetter = Character.toString(wholeSpell.charAt(0));
            mode.setWholeSpell(wholeSpell);
            mode.setFirstLetter(firstLetter);
            mode.setSimpleSpell(PinyinUtils.converterToFirstSpell(name));
        } else {// 如果全拼为空，理论上是一种错误情况，因为这代表着昵称为空
            mode.setWholeSpell("#");
            mode.setFirstLetter("#");
            mode.setSimpleSpell("#");
        }
    }



    public void sendBroadcast() {
        Intent mIntent = new Intent(MucgroupUpdateUtil.ACTION_UPDATE);
        getActivity().sendBroadcast(mIntent);
    }

    public class MessageAdapter extends BaseRecyclerAdapter<BaseSortModel<Friend>> {
        public MessageAdapter(Context context, List<BaseSortModel<Friend>> datas, int itemLayoutId) {
            super(context, datas, itemLayoutId);
        }

        @Override
        protected void convert(BaseRecyclerHolder holder, BaseSortModel<Friend> item, int position) {

            SwipeMenuLayout swipeMenuLayout = holder.getView(R.id.id_root_swp);
            RelativeLayout itemView = holder.getView(R.id.id_ry_content);
            itemView.setOnClickListener(v -> {
                if (onItemSelectListener!=null){
                    onItemSelectListener.onItemSelect(item);
                }
            });

            if (item.getBean().getUserId().equals("10000")){
               // swipeMenuLayout.setLeftSwipe(false);
                swipeMenuLayout.setSwipeEnable(false);
            }else {
                swipeMenuLayout.setLeftSwipe(true);
            }
            Button btnDelete = holder.getView(R.id.btnDelete);
            btnDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //(CstSwipeDelMenu holder.itemView).quickClose();
                    swipeMenuLayout.quickClose();
                    String mLoginUserId = ConfigApplication.instance().mLoginUser.getUserId();
                    if (item.getBean().getUnReadNum() > 0) {
                        MsgBroadcast.broadcastMsgNumUpdate(getActivity(), false, item.getBean().getUnReadNum());
                    }
                    BaseSortModel<Friend> mode = mFriendList.get(position);
                    mFriendList.remove(mode);
                    mOriginalFriendList.remove(mode);
                    //mAdapter.notifyDataSetChanged();
                    mAdapter.notifyItemRemoved(position);//推荐用这个
                    // 如果是普通的人，从好友表中删除最后一条消息的记录，这样就不会查出来了
                    FriendDao.getInstance().resetFriendMessage(mLoginUserId, item.getBean().getUserId());
                    // 消息表中删除
                    ChatMessageDao.getInstance().deleteMessageTable(mLoginUserId, item.getBean().getUserId());

                }
            });
            CircleImageView avatar_img = holder.getView(R.id.avatar_img);
            TextView num_tv = holder.getView(R.id.num_tv);
            TextView nick_name_tv = holder.getView(R.id.nick_name_tv);
            TextView content_tv = holder.getView( R.id.content_tv);
            TextView time_tv = holder.getView(R.id.time_tv);
            NineGridImageView ngiv = holder.getView(R.id.ngiv);

            /*群聊头像*/
            DingViewGroup dingViewGroup = holder.getView(R.id.iv_item_avatar);
            final Friend friend = mFriendList.get(position).getBean();
            if (friend.getRoomFlag() == 0) {// 这是单个人
                switch (friend.getUserId()) {
                    case Friend.ID_SYSTEM_MESSAGE: // 系统消息的头像
                       // avatar_img.setImageResource(R.drawable.im_notice);
                        friend.setNickName("孜孜管家服务");
                        break;
                    case Friend.ID_NEW_FRIEND_MESSAGE: // 新朋友的头像
                        friend.setNickName("新的朋友");
                        break;
                    default: // 其他
                        break;
                }
                AvatarHelper.getInstance().displayAvatar(friend, avatar_img, true);
                avatar_img.setVisibility(View.VISIBLE);
                dingViewGroup.setVisibility(View.GONE);
                ngiv.setVisibility(View.GONE);
            }else {
                avatar_img.setVisibility(View.VISIBLE);
                ngiv.setVisibility(View.GONE);
                avatar_img.setImageResource(R.drawable.head_group);


                dingViewGroup.setVisibility(View.VISIBLE);
                avatar_img.setVisibility(View.GONE);
                loadMembers(friend.getRoomId(),dingViewGroup);

            }
            nick_name_tv.setText(friend.getShowName());
            time_tv.setText(TimeUtils.getFriendlyTimeDesc(getActivity(), friend.getTimeSend()));
            CharSequence content ;
            if (friend.getType() == XmppMessage.TYPE_TEXT) {
                String s = StringUtils.replaceSpecialChar(friend.getContent());
                content = HtmlUtils.transform200SpanString(s.replaceAll("\n", "\r\n"), true);
            } else if (friend.getType()==XmppMessage.TYPE_CARD){
                content = "个人名片";
            }else {
                content = friend.getContent();
            }
            if (Friend.ID_SYSTEM_MESSAGE.equals(friend.getUserId())&&content.toString().contains("面试")){
                content_tv.setText("欢迎使用孜孜管家聊天系统");
            }else {
                content_tv.setText(content);
            }
            if (friend.getUnReadNum() > 0) {
                String numStr = friend.getUnReadNum() >= 99 ? "99+" : friend.getUnReadNum() + "";
                num_tv.setText(numStr);
                num_tv.setVisibility(View.VISIBLE);
            } else {
                num_tv.setVisibility(View.GONE);
            }

        }

        OnItemSelectListener onItemSelectListener;
        void setOnItemSelectListener(OnItemSelectListener onItemSelectListener) {
            this.onItemSelectListener = onItemSelectListener;
        }
    }

    public  interface  OnItemSelectListener<T>{
        void onItemSelect(T t);
    }




    private void getAvatarList(final List<String>  avatarList, DingViewGroup dingViewGroup){
        ThreadManager.getPool().execute(new Runnable() {
            @Override
            public void run() {
                final List<Bitmap> bitmapList = new ArrayList<>();
                L_.e("获取群数据a-----"+avatarList.size());
                for (String decodePic : avatarList) {
                    L_.e("获取群数据 decodePic----"+decodePic);
                    String url = AvatarHelper.getAvatarUrl(decodePic, true);
                    L_.e("获取群数据- url---"+url);
                    try {
                        Bitmap myBitmap = Glide.with(UIUtils.getContext())
                                .load(url)
                                .asBitmap() //必须
                                .centerCrop()
                                .into(500, 500)
                                .get();
                        L_.e("获取群数据 myBitmap=============="+myBitmap);
                        bitmapList.add(myBitmap);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                }
                final Bitmap result = PerionIconFactory.getAvatar(bitmapList, 200, 200);
                UIUtils.runInMainThread(new Runnable() {
                    @Override
                    public void run() {
                        dingViewGroup.setImageBitmap(bitmapList.size() == 1 ? bitmapList.get(0) : result);
                    }
                });
            }
        });


       // L_.e("获取群数据=============="+bitmapList.size()+"==="+bitmapList.get(0)+result);


    }



    private void loadMembers(String roomId, DingViewGroup dingViewGroup) {
       // showProgressDialogWithText("获取数据");
        HashMap<String, String> params = new HashMap<>();
        params.put("access_token", ConfigApplication.instance().mAccessToken);
        params.put("roomId", roomId);
        HttpUtils.getInstance().postServiceData(AppConfig.ROOM_GET, params, new ChatObjectCallBack<MucRoom>(MucRoom.class) {

            @Override
            protected void onXError(String exception) {
                T_.showToastReal(exception);
            }

            @Override
            protected void onSuccess(ObjectResult<MucRoom> result) {
                List<String> existIds = new ArrayList<>();
                boolean success = ResultCode.defaultParser(result, true);
                if (success && result.getData() != null) {
                    MucRoom mucRoom = result.getData();
                    //L_.e("获取群数据----"+mucRoom.getMembers().size());
                    if (mucRoom==null)return;
                    int count;
                    if (mucRoom.getMembers().size()>4){
                        count=4;
                    }else {
                        count=mucRoom.getMembers().size();
                    }
                    for (int i = 0; i < count; i++) {
                        existIds.add(mucRoom.getMembers().get(i).getUserId());
                    }
                    L_.e("获取群数据----"+existIds.get(0));
                    getAvatarList(existIds,  dingViewGroup);

                }
            }
        });
    }


}
