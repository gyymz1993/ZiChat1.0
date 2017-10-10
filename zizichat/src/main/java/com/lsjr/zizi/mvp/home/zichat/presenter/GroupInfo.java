package com.lsjr.zizi.mvp.home.zichat.presenter;

import android.widget.ImageView;
import android.widget.TextView;

import com.andview.myrvview.LQRRecyclerView;
import com.lqr.adapter.LQRAdapterForRecyclerView;
import com.lqr.adapter.LQRViewHolderForRecyclerView;
import com.lsjr.bean.ObjectResult;
import com.lsjr.callback.ChatObjectCallBack;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.R;
import com.lsjr.zizi.chat.bean.MucRoom;
import com.lsjr.zizi.chat.bean.MucRoomMember;
import com.lsjr.zizi.chat.bean.ResultCode;
import com.lsjr.zizi.chat.broad.MsgBroadcast;
import com.lsjr.zizi.chat.dao.ChatMessageDao;
import com.lsjr.zizi.chat.dao.FriendDao;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.loader.AvatarHelper;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.mvp.home.zichat.GroupInfoActivity;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;
import com.ymz.baselibrary.utils.UIUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/16 9:22
 */

public interface GroupInfo {

     interface IView{
        LQRRecyclerView getRecycleView();

        void upDataUI( MucRoom mucRoom);

         void getRoom(Friend mRoom);

         void deleteRoom(boolean isDelete);
    }


    class Presenter extends BasePresenter<IView> {
        private GroupInfoActivity activity;
        private String mLoginUserId;
        private Friend mRoom;
        private List<MucRoomMember> mMembers;
        private MucRoom mucRoom;
        private LQRAdapterForRecyclerView<MucRoomMember> userManagerAdapter;

        public Presenter(GroupInfoActivity activity, IView mvpView) {
            super(mvpView);
            this.activity=activity;
            mMembers=new ArrayList<>();
            mLoginUserId = ConfigApplication.instance().getLoginUserId();
            mRoom = FriendDao.getInstance().getFriend(mLoginUserId, activity.getmRoomJid());
        }


        public void init(){
            initAdapter();
            loadMembers();
        }

        private void initAdapter(){
            if (userManagerAdapter == null) {
                userManagerAdapter = new LQRAdapterForRecyclerView<MucRoomMember>(UIUtils.getContext(), mMembers, R.layout.itme_group_manger) {
                    @Override
                    public void convert(LQRViewHolderForRecyclerView helper, MucRoomMember mucRoomMember, int position) {
                        ImageView ivHeader = helper.getView(R.id.ivHeader);
                        AvatarHelper.getInstance().displayAvatar(mucRoomMember.getUserId(), ivHeader, true);
                        TextView tvName = helper.getView(R.id.id_user_name);
                        if (mucRoomMember != null) {
                            tvName.setText(mucRoomMember.getNickName() == null ? "暂无昵称" : mucRoomMember.getNickName());
                        }
                    }
                };
                mvpView.getRecycleView().setAdapter(userManagerAdapter);
            }
        }

        public void loadMembers() {
           // showProgressDialogWithText("获取数据");
            HashMap<String, String> params = new HashMap<>();
            params.put("access_token", ConfigApplication.instance().mAccessToken);
            params.put("roomId", mRoom.getRoomId());
            HttpUtils.getInstance().postServiceData(AppConfig.ROOM_GET, params, new ChatObjectCallBack<MucRoom>(MucRoom.class) {

                @Override
                protected void onXError(String exception) {
                   // dismissProgressDialog();
                    T_.showToastReal(exception);
                }

                @Override
                protected void onSuccess(ObjectResult<MucRoom> result) {
                   // dismissProgressDialog();
                    boolean success = ResultCode.defaultParser(result, true);
                    if (success && result.getData() != null) {
                        mucRoom = result.getData();
                        mMembers.clear();
                        mMembers.addAll(mucRoom.getMembers());
                        userManagerAdapter.setData(mMembers);
                        mvpView.upDataUI(mucRoom);
                        mvpView.getRoom(mRoom);
                    }
                }
            });
        }

        public void deleteRoom(final Friend sortFriend) {
            boolean deleteRoom = false;
            if (mLoginUserId.equals(sortFriend.getRoomCreateUserId())) {
                deleteRoom = true;
            }
            String url = null;
            if (deleteRoom) {
                url = AppConfig.ROOM_DELETE;
            } else {
                url = AppConfig.ROOM_MEMBER_DELETE;
            }
            HashMap<String, String> params = new HashMap<String, String>();
            params.put("access_token", ConfigApplication.instance().mAccessToken);
            L_.e("roomId  "+sortFriend.getRoomId()+"------"+sortFriend.getRemarkName());
            params.put("roomId", sortFriend.getRoomId());
            if (!deleteRoom) {
                params.put("userId", mLoginUserId);
            }

            HttpUtils.getInstance().postServiceData(url, params, new ChatObjectCallBack<Void>(Void.class) {

                @Override
                protected void onXError(String s) {

                }

                @Override
                protected void onSuccess(ObjectResult<Void> objectResult) {
                    boolean success = ResultCode.defaultParser(objectResult, true);
                    if (success) {
                        deleteFriend(sortFriend);
                    }
                }
            });
        }

        private void deleteFriend(final Friend sortFriend) {
            //String firstLetter = sortFriend.getFirstLetter();
           // Friend friend = sortFriend.getBean();
            // 删除这个房间
            FriendDao.getInstance().deleteFriend(mLoginUserId, sortFriend.getUserId());
            // 消息表中删除
            ChatMessageDao.getInstance().deleteMessageTable(mLoginUserId, sortFriend.getUserId());
            // 更新消息界面
            MsgBroadcast.broadcastMsgNumReset(UIUtils.getContext());
            MsgBroadcast.broadcastMsgUiUpdate(UIUtils.getContext());
            mvpView.deleteRoom(true);
           // activity.exitMucChat(friend.getUserId());

        }

    }


}
