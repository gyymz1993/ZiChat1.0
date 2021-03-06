package com.lsjr.zizi.mvp.home.zichat.presenter;

import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.lsjr.bean.ArrayResult;
import com.lsjr.bean.ObjectResult;
import com.lsjr.callback.ChatArrayCallBack;
import com.lsjr.callback.ChatObjectCallBack;
import com.lsjr.utils.HttpUtils;
import com.lsjr.zizi.AppConfig;
import com.lsjr.zizi.base.BaseContract;
import com.lsjr.zizi.chat.bean.MucRoom;
import com.lsjr.zizi.chat.bean.ResultCode;
import com.lsjr.zizi.chat.dao.FriendDao;
import com.lsjr.zizi.chat.db.Friend;
import com.lsjr.zizi.chat.thread.ThreadManager;
import com.lsjr.zizi.loader.AvatarHelper;
import com.lsjr.zizi.mvp.home.ConfigApplication;
import com.lsjr.zizi.mvp.home.session.GroupActivity;
import com.lsjr.zizi.util.TimeUtils;
import com.lsjr.zizi.view.groupview.DingViewGroup;
import com.lsjr.zizi.view.groupview.PerionIconFactory;
import com.ymz.baselibrary.mvp.BasePresenter;
import com.ymz.baselibrary.utils.L_;
import com.ymz.baselibrary.utils.T_;
import com.ymz.baselibrary.utils.UIUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.lsjr.zizi.AppConfig.PAGE_SIZE;

/**
 * 创建人：$ gyymz1993
 * 创建时间：2017/9/26 12:02
 */

public interface GroupList {

    interface IView extends BaseContract.View{

        RecyclerView getRvListView();

        void  interMucChat(String roomId, final String roomName);

        void  loadDataSucceed(List<MucRoom> mMucRooms);
    }


    class GroupListPresenter extends BasePresenter<IView> {
        private GroupActivity activity;

        public GroupListPresenter(IView mvpView) {
            super(mvpView);
        }


        public void joinRoom(final MucRoom room, final String loginUserId) {
            Log.d("roamer", "joinRoom");
            HashMap<String, String> params = new HashMap<>();
            params.put("access_token", ConfigApplication.instance().mAccessToken);
            params.put("roomId", room.getId());
            if (room.getUserId().equals(loginUserId)) {
                params.put("type", "1");
            } else {
                params.put("type", "2");
            }
            // showProgressDialogWithText("进入房间");
            HttpUtils.getInstance().postServiceData(AppConfig.ROOM_JOIN, params, new ChatArrayCallBack<Void>(Void.class) {

                @Override
                protected void onXError(String exception) {
                    //dismissProgressDialog();
                    //T_.showToastReal(exception);
                }

                @Override
                protected void onSuccess(ArrayResult<Void> result) {
                    // dismissProgressDialog();
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
                        FriendDao.getInstance().createOrUpdateFriend(friend);
                        mvpView.interMucChat(room.getJid(), room.getName());
                    }
                }
            });

        }


        private int mPageIndex = 0;

        public void requestData() {
            mvpView.showLoading();
            HashMap<String, String> params = new HashMap<>();
            params.put("userId", ConfigApplication.instance().mLoginUser.getUserId());
            params.put("pageIndex", String.valueOf(mPageIndex));
            params.put("pageSize", String.valueOf(PAGE_SIZE));
            params.put("access_token", ConfigApplication.instance().mAccessToken);
            HttpUtils.getInstance().postServiceData(AppConfig.ROOM_LIST, params, new ChatArrayCallBack<MucRoom>(MucRoom.class) {
                @Override
                protected void onXError(String exception) {
                    //dismissProgressDialog();
                }

                @Override
                protected void onSuccess(ArrayResult result) {
                    //dismissProgressDialog();
                    boolean success = ResultCode.defaultParser(result, true);
                    if (success) {
                        L_.e(result.getData().toString());
                        List<MucRoom> mMucRooms = result.getData();
                        // mPageIndex++;
                        // mAdapter.notifyDataSetChanged(mMucRooms);
                        mvpView.loadDataSucceed(mMucRooms);
                    }
                }

            });
        }

        public void loadMembers(String roomId, DingViewGroup dingViewGroup) {
            // showProgressDialogWithText("获取数据");
            HashMap<String, String> params = new HashMap<>();
            params.put("access_token", ConfigApplication.instance().mAccessToken);
            L_.e(roomId);
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
                        if (mucRoom == null) return;
                        int count;
                        if (mucRoom.getMembers().size() > 4) {
                            count = 4;
                        } else {
                            count = mucRoom.getMembers().size();
                        }
                        for (int i = 0; i < count; i++) {
                            existIds.add(mucRoom.getMembers().get(i).getUserId());
                        }
                        L_.e("获取群数据----" + existIds.get(0));
                        getAvatarList(existIds, dingViewGroup);

                    }
                }
            });
        }


        private void getAvatarList(final List<String> avatarList, DingViewGroup dingViewGroup) {
            ThreadManager.getPool().execute(new Runnable() {
                @Override
                public void run() {
                    final List<Bitmap> bitmapList = new ArrayList<>();
                    L_.e("获取群数据a-----" + avatarList.size());
                    for (String decodePic : avatarList) {
                        L_.e("获取群数据 decodePic----" + decodePic);
                        String url = AvatarHelper.getAvatarUrl(decodePic, true);
                        L_.e("获取群数据- url---" + url);
                        try {
                            Bitmap myBitmap = Glide.with(UIUtils.getContext())
                                    .load(url)
                                    .asBitmap() //必须
                                    .centerCrop()
                                    .into(500, 500)
                                    .get();
                            L_.e("获取群数据 myBitmap==============" + myBitmap);
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


        }


    }
}
